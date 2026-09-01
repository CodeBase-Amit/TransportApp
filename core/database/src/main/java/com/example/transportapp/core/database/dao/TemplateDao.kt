package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Template access (Phase 3 S11). Versions are rows; the "exactly one active per
 * (company, template_key)" invariant is a repository transaction that flips the old row
 * and inserts the new one together.
 */
@Dao
interface TemplateDao {

    @Upsert
    suspend fun upsertTemplate(entity: TemplateEntity)

    @Query("SELECT * FROM TEMPLATE_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY template_key, version DESC")
    fun observeTemplates(companyId: String): Flow<List<TemplateEntity>>

    @Query(
        """
        SELECT * FROM TEMPLATE_E
        WHERE company_id = :companyId AND template_key = :templateKey AND is_active = 1 AND deleted_at IS NULL
        ORDER BY version DESC LIMIT 1
        """,
    )
    suspend fun getActiveTemplate(companyId: String, templateKey: String): TemplateEntity?

    /** The pinned-version lookup a reprint needs (§9.12). */
    @Query(
        """
        SELECT * FROM TEMPLATE_E
        WHERE company_id = :companyId AND template_key = :templateKey AND version = :version AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getTemplateVersion(companyId: String, templateKey: String, version: Int): TemplateEntity?

    @Query("SELECT COUNT(*) FROM TEMPLATE_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countTemplates(companyId: String): Int
}
