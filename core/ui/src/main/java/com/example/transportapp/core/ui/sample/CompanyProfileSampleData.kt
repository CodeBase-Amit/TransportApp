package com.example.transportapp.core.ui.sample

/**
 * T25 Company profile demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object CompanyProfileSampleData {

    const val TITLE = "Company profile"

    const val PREVIEW_HEADING = "HOW IT WILL PRINT"
    const val IDENTITY_HEADING = "Identity"
    const val ADDRESS_HEADING = "Address"
    const val TAX_HEADING = "Tax and Registration"
    const val CONTACT_HEADING = "Contact as printed"
    const val LOGO_HEADING = "Logo"
    const val FOOTER_HEADING = "What prints at the bottom"

    const val LEGAL_NAME = "Shivshakti Roadlines"
    const val TRADE_NAME = ""
    const val CONSTITUTION = "Proprietorship"
    const val ADDRESS = "14, Transport Nagar, AB Road, Indore"
    const val CITY = "Indore"
    const val PINCODE = "452003"
    const val STATE = "Madhya Pradesh"
    const val GSTIN = "23AABCS4521M1Z9"
    const val PAN = "AABCS4521M"
    const val TRANSPORTER_ID = ""
    const val PHONE = "+91 731 2589 041"
    const val ALT_PHONE = "+91 94250 33712"
    const val EMAIL = "office@shivshaktiroadlines.in"
    const val WEBSITE = ""
    const val FOOTER_CLAUSE = ""

    val CONSTITUTIONS = listOf("Proprietorship", "Partnership", "Private Limited")

    const val SAVE = "Save"
    const val SAVE_AND_UPDATE = "Save and update all templates"
    const val TEMPLATE_NOTE = "5 installed templates will use the new details on the next print."

    const val DELETE_TITLE = "Delete this record"
    const val DELETE_BODY = "4,188 bilties use this company, so it can't be deleted."

    const val LOGO_NOTE = "PNG or JPG, at least 600x600. It prints in black and white too, so avoid thin light lines."
}
