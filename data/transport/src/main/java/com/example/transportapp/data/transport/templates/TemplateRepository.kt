package com.example.transportapp.data.transport.templates

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.TemplateEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.doc.engine.TemplateModel
import com.example.transportapp.doc.engine.TemplateParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One T29 list row. */
data class TemplateSummary(
    val localId: String,
    val templateKey: String,
    val name: String,
    val version: Int,
    val isActive: Boolean,
    val schemaVersion: Int,
    /** BUILT-IN / COMPANY. */
    val visibility: String,
    val sectionCount: Int,
)

/** An active template resolved for rendering: parsed model plus its pinned identity. */
data class ParsedTemplate(
    val localId: String,
    val templateKey: String,
    val version: Int,
    val schemaVersion: Int,
    val contentJson: String,
    val contentHash: String,
    val model: TemplateModel,
)

/**
 * Templates as data (Phase 3 S11, §16.2). Versions are rows; the exactly-one-active
 * invariant is this repository's transaction. Parsing and validation live in the pure
 * `:doc-engine` — a refused template never reaches the database.
 */
interface TemplateRepository {

    fun observeTemplates(): Flow<List<TemplateSummary>>

    suspend fun getActiveTemplate(templateKey: String): ParsedTemplate?

    /** The pinned-version lookup a reprint resolves against (§9.12). */
    suspend fun getTemplateVersion(templateKey: String, version: Int): ParsedTemplate?

    /**
     * Install a template version as data: parse+validate first (a refused template answers
     * a typed error and writes nothing), then flip active within one transaction.
     */
    suspend fun installTemplate(templateKey: String, contentJson: String, now: Long): Result<TemplateSummary>
}

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
    private val outboxWriter: OutboxWriter,
) : TemplateRepository {

    override fun observeTemplates(): Flow<List<TemplateSummary>> = flow {
        val s = sessionRepository.session.first()
        emitAll(
            database.templateDao().observeTemplates(s.companyId)
                .map { rows -> rows.map { it.toSummary() } },
        )
    }

    override suspend fun getActiveTemplate(templateKey: String): ParsedTemplate? {
        val s = sessionRepository.session.first()
        return database.templateDao().getActiveTemplate(s.companyId, templateKey)?.toParsed()
    }

    override suspend fun getTemplateVersion(templateKey: String, version: Int): ParsedTemplate? {
        val s = sessionRepository.session.first()
        return database.templateDao().getTemplateVersion(s.companyId, templateKey, version)?.toParsed()
    }

    override suspend fun installTemplate(templateKey: String, contentJson: String, now: Long): Result<TemplateSummary> {
        val s = sessionRepository.session.first()
        // Parse and validate BEFORE any write: a malformed template is refused with the
        // typed §18.3 code that matches the reason (§9.2's keep-the-old-version rule).
        val previous = database.templateDao().getActiveTemplate(s.companyId, templateKey)
        val nextVersion = (previous?.version ?: 0) + 1
        val parsed = when (val result = TemplateParser.parse(contentJson)) {
            is TemplateParser.ParseResult.Ok -> result.template
            is TemplateParser.ParseResult.Refused -> return Result.failure(
                if (result.reason.contains("schemaVersion")) ErrorCode.TEMPLATE_VERSION_MISSING else ErrorCode.TEMPLATE_FIELD_UNKNOWN,
                result.reason,
            )
        }

        return database.withTransaction {
            // Flip the old active row and insert the new version together (§6.6's atomic
            // replacement: no instant without an active template).
            previous?.let {
                database.templateDao().upsertTemplate(it.copy(is_active = false, updated_at_local = now))
            }
            val entity = TemplateEntity(
                local_id = "tpl-" + UUID.randomUUID().toString(),
                server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null, company_id = s.companyId,
                template_key = templateKey, version = nextVersion, is_active = true,
                schema_version = parsed.schemaVersion, content_json = contentJson,
                content_hash = contentJson.hashCode().toString(16),
                visibility = "COMPANY", created_by_name = s.name,
            )
            database.templateDao().upsertTemplate(entity)
            outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.TEMPLATE,
                entityLocalId = entity.local_id,
                payloadJson = JSONObject()
                    .put("template_key", templateKey)
                    .put("version", nextVersion)
                    .put("schema_version", parsed.schemaVersion)
                    .put("content_hash", entity.content_hash)
                    .toString(),
                now = now,
            )
            Result.success(entity.toSummary())
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun TemplateEntity.toSummary(): TemplateSummary {
        val parsed = TemplateParser.parse(content_json)
        return TemplateSummary(
            localId = local_id, templateKey = template_key,
            name = (parsed as? TemplateParser.ParseResult.Ok)?.template?.name?.ifBlank { null } ?: template_key,
            version = version, isActive = is_active, schemaVersion = schema_version,
            visibility = visibility,
            sectionCount = (parsed as? TemplateParser.ParseResult.Ok)?.template?.sections?.size ?: 0,
        )
    }

    private fun TemplateEntity.toParsed(): ParsedTemplate? = when (val parsed = TemplateParser.parse(content_json)) {
        is TemplateParser.ParseResult.Ok -> ParsedTemplate(
            localId = local_id, templateKey = template_key, version = version,
            schemaVersion = schema_version, contentJson = content_json,
            contentHash = content_hash, model = parsed.template,
        )
        // A stored template that no longer parses is a corrupted row: surface nothing
        // rather than render garbage (the audit trail keeps the refusal).
        is TemplateParser.ParseResult.Refused -> null
    }
}
