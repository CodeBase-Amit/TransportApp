package com.example.transportapp.doc.engine

/**
 * A document template as data (Implementation.md §9.15). Everything optional has a default
 * so a minimal template is short. `schemaVersion` is the engine compatibility gate: the
 * engine refuses a template whose schema version exceeds [ENGINE_SCHEMA_VERSION].
 */
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val ENGINE_SCHEMA_VERSION = 1

@Serializable
data class TemplateModel(
    val schemaVersion: Int,
    val id: String,
    val name: String = "",
    val version: Int = 1,
    val paper: Paper = Paper(),
    val theme: Theme = Theme(),
    val business: BusinessBlock,
    val sections: List<Section> = emptyList(),
    val locale: LocaleHints = LocaleHints(),
)

@Serializable
data class Paper(
    val size: String = "A4",
    val marginMm: Int = 10,
    val orientation: String = "portrait",
)

@Serializable
data class Theme(
    val primaryColor: String = "#0E4D38",
    val textOnPrimary: String = "#FFFFFF",
    val fontFamily: String = "sans",
    val logoAssetRef: String? = null,
)

@Serializable
data class BusinessBlock(
    val shopName: String,
    val tagline: String? = null,
    val tel: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val address: String? = null,
    val taxId: String? = null,
    val extraLines: List<String> = emptyList(),
)

@Serializable
data class LocaleHints(
    val currencySymbol: String = "₹",
    val dateFormat: String = "dd.MM.yyyy",
    val grouping: String = "indian",
)

/**
 * The eight known section types (§9.15). Unknown types fail validation — the compiler of
 * the renderer enumerates every branch, so a new type is a code change by design.
 */
@Serializable
data class Section(
    val type: String,
    val visibleWhen: VisibleWhen? = null,
    val title: String? = null,
    val fields: List<Field> = emptyList(),
    val minRows: Int = 0,
    val columns: List<ItemColumn> = emptyList(),
    val text: String? = null,
)

@Serializable
data class VisibleWhen(val field: String, val equals: String)

/**
 * A field is a *key*, never a business concept: the renderer reads values strictly by key
 * from the snapshot's value map, and a value's meaning lives in the data, not the code.
 */
@Serializable
data class Field(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val kind: String = "text",
    val expression: String? = null,
)

@Serializable
data class ItemColumn(val key: String, val label: String, val widthMm: Int = 20)

/** The section types the engine understands (§9.15). */
val KNOWN_SECTION_TYPES = setOf("header", "title", "customer", "meta", "items", "totals", "footer", "notes")
