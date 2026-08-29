package com.example.transportapp.core.ui.sample

/**
 * T19 Generic master editor demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object MasterEditorSampleData {

    const val TITLE = "Edit party"
    const val LOADING_TITLE = "Edit master"

    const val IDENTITY_HEADING = "Identity"
    const val ADDRESS_HEADING = "Address"
    const val TAX_HEADING = "Tax Information"
    const val DEFAULTS_HEADING = "Defaults for this party"

    const val NAME = "Deepak Steel Traders"
    const val EMAIL = "contact@deepaksteeltraders.in"
    const val PHONE = "+91 94250 61183"
    const val STREET_ADDRESS = "Plot 14, Transport Nagar, Indore"
    const val STATION = "Indore"
    const val PINCODE = "452003"
    const val GSTIN = "23AACDS8812K1Z4"
    const val TAX_STATUS = "Verified active taxpayer"
    const val USUAL_ROUTE = "Indore → Nashik"
    const val RATE_CARD = "Deepak Steel Traders 2026-27"

    const val TYPE_LABEL = "Type"
    val typeOptions = listOf("Consignor", "Consignee", "Both")
    const val DEFAULT_TYPE = "Both"

    const val PAYMENT_LABEL = "Usual payment mode"
    val paymentOptions = listOf("Paid", "To Pay", "TBB")
    const val DEFAULT_PAYMENT = "TBB"

    const val DELETE_LABEL = "Delete this record"
    const val DELETE_MESSAGE = "41 bilties use this party, so it can't be deleted. You can mark it inactive instead."
    const val SAVE_LABEL = "Save party"
    const val SAVE_TOP_LABEL = "Save"
}
