package com.example.transportapp.doc.engine

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Template acquisition, parsing and validation (Implementation.md §9.2). A template that
 * fails validation is refused — the caller keeps the previously active version and records
 * the failure. A shop that cannot bill because someone published a malformed template is a
 * far worse outcome than a shop running last week's template.
 */
object TemplateParser {

    /** Expressions the whitelisted calculator understands (§9.6). Expressions are data. */
    val EXPRESSION_WHITELIST = setOf(
        "sum(items.freight)",
        "sum(items.amount)",
        "sum(items.taxable)",
        "sum(items.tax)",
        "count(items)",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    sealed interface ParseResult {
        data class Ok(val template: TemplateModel) : ParseResult
        data class Refused(val reason: String) : ParseResult
    }

    fun parse(contentJson: String): ParseResult = try {
        val template = json.decodeFromString<TemplateModel>(contentJson)
        validate(template)
    } catch (e: SerializationException) {
        ParseResult.Refused("malformed template JSON: ${e.message}")
    } catch (e: IllegalArgumentException) {
        ParseResult.Refused("malformed template JSON: ${e.message}")
    }

    private fun validate(template: TemplateModel): ParseResult {
        if (template.schemaVersion > ENGINE_SCHEMA_VERSION) {
            return ParseResult.Refused("template schemaVersion ${template.schemaVersion} exceeds the engine's $ENGINE_SCHEMA_VERSION — please update the app")
        }
        if (template.business.shopName.isBlank()) {
            return ParseResult.Refused("the business block needs a shop name")
        }
        template.sections.forEach { section ->
            if (section.type !in KNOWN_SECTION_TYPES) {
                return ParseResult.Refused("unknown section type '${section.type}'")
            }
            section.fields.forEach { field ->
                val expression = field.expression ?: return@forEach
                if (expression !in EXPRESSION_WHITELIST) {
                    return ParseResult.Refused("field '${field.key}' carries non-whitelisted expression '$expression'")
                }
            }
        }
        // Every field key must be unique document-wide (§9.15).
        val keys = template.sections.flatMap { it.fields }.map { it.key } +
            template.sections.filter { it.type == "items" }.flatMap { it.columns }.map { it.key }
        val duplicate = keys.groupBy { it }.filter { it.value.size > 1 }.keys.firstOrNull()
        if (duplicate != null) {
            return ParseResult.Refused("field key '$duplicate' is defined more than once")
        }
        return ParseResult.Ok(template)
    }
}
