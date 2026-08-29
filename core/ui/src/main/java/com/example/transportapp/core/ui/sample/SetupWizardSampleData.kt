package com.example.transportapp.core.ui.sample

/**
 * T3 Company setup wizard demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object SetupWizardSampleData {

    const val TITLE = "Set up your company"
    const val SKIP = "Skip for now"
    const val NEXT = "Next"
    const val FINISH = "Finish setup"
    const val ADD_VEHICLES_LATER = "Add vehicles later"

    val STEP_LABELS = listOf("Company", "Tax", "Branch", "Vehicle")

    // Step 1 — Company
    const val COMPANY_NAME = "Shivshakti Roadlines"
    const val HEAD_OFFICE = "Plot 14, Transport Nagar, Indore, Madhya Pradesh 452003"
    const val PHONE = "+91 94250 61183"
    const val EMAIL = "office@shivshaktiroadlines.in"
    const val PRINT_HEADING = "How it will print"
    const val PRINT_NAME = "SHIVSHAKTI ROADLINES"
    const val PRINT_PHONE_LABEL = "Ph: +91 94250 61183"
    const val PRINT_DOC = "CONSIGNMENT NOTE"

    // Step 2 — Tax
    const val GSTIN = "23AABCS4521M1Z9"
    const val PAN = "AABCS4521M"
    const val GST_HEADING = "GST on freight"
    const val GST_FORWARD = "We pay GST – 5% forward charge, no input credit"
    const val GST_REVERSE = "The consignee pays under reverse charge"
    const val GST_NOTE = "This decides how every freight bill is calculated. You can change it later in Company profile, and bilties already issued keep the treatment they were printed with."
    const val GST_THIRD_PARTY = "Confirm the current GTA rates with your CA before your first bill."

    // Step 3 — Branch
    const val BRANCH_NAME = "Indore"
    const val BRANCH_ADDRESS = "Same as head office"
    const val BRANCH_CODE = "IND"
    const val BRANCH_HEADING = "Bilty numbers from this branch"
    const val BRANCH_FY_PART = "2627"
    const val BRANCH_DIGITS = "5"
    const val NEXT_BILTY_LABEL = "Next bilty will print as"
    const val NEXT_BILTY = "IND/2627/00001"

    // Step 4 — Vehicle
    const val VEHICLE_NUMBER = "MH 15 BK 4412"
    const val OWNERSHIP_LABEL = "Ownership"
    val OWNERSHIP_OPTIONS = listOf("Own", "Attached")
    const val OWNERSHIP_DEFAULT = "Own"
    const val CAPACITY = "9000"
    const val CAPACITY_UNIT = "kg"
    const val DRIVER_NAME = "Gurmeet Singh"
    const val DRIVER_PHONE = "+91 90280 41176"

    // Done frame
    const val DONE_TITLE = "Shivshakti Roadlines is ready"
    const val DONE_BODY = "You're the owner. Book your first bilty and all four copies print together."
    const val DONE_PRIMARY = "Book the first bilty"
    const val DONE_SECONDARY = "Go to dashboard"
}
