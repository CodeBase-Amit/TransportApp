package com.example.transportapp.feature.auth.screen

/**
 * T3 — Company setup wizard (S18). Input fields default EMPTY — the user types their own
 * company; the sample values were demo furniture and no longer seed the registration.
 */
data class SetupWizardUiState(
    val step: Int = 0,
    val stepLabels: List<String> = listOf("Company", "Tax", "Branch", "Vehicle"),
    val title: String = "Set up your company",
    val skipLabel: String = "Skip for now",
    val nextLabel: String = "Next",
    val finishLabel: String = "Finish setup",
    val addVehiclesLaterLabel: String = "Add vehicles later",
    // Step 1 — Company
    val companyName: String = "",
    val headOffice: String = "",
    val phone: String = "",
    val email: String = "",
    val printHeading: String = "How it will print",
    val printDoc: String = "CONSIGNMENT NOTE",
    // Step 2 — Tax
    val gstin: String = "",
    val pan: String = "",
    val gstHeading: String = "GST on freight",
    val gstForwardLabel: String = "We pay GST – 5% forward charge, no input credit",
    val gstReverseLabel: String = "The consignee pays under reverse charge",
    val gstOption: Int = 0,
    val gstNote: String = "This decides how every freight bill is calculated. You can change it later in Company profile, and bilties already issued keep the treatment they were printed with.",
    val gstThirdParty: String = "Confirm the current GTA rates with your CA before your first bill.",
    // Step 3 — Branch
    val branchName: String = "",
    val branchAddress: String = "",
    val branchCode: String = "",
    val branchHeading: String = "Bilty numbers from this branch",
    val branchFyPart: String = "2627",
    val branchDigits: String = "5",
    val nextBiltyLabel: String = "Next bilty will print as",
    // Step 4 — Vehicle
    val vehicleNumber: String = "",
    val ownershipLabel: String = "Ownership",
    val ownershipOptions: List<String> = listOf("Own", "Attached"),
    val ownership: String = "Own",
    val capacity: String = "",
    val capacityUnit: String = "kg",
    val driverName: String = "",
    val driverPhone: String = "",
    // S18: Finish persists the typed company; error surfaces typed failures (Spec.md §9).
    val justFinished: Boolean = false,
    val error: String? = null
) {
    /** §9 preview: the first number the new branch's bilty series will print. */
    val nextBilty: String
        get() = branchCode.uppercase().takeIf { it.isNotBlank() }?.let { "$it/$branchFyPart/00001" } ?: "—"

    val doneTitle: String
        get() = if (companyName.isBlank()) "Your company is ready" else "$companyName is ready"
}

sealed interface SetupWizardEvent {
    data object Next : SetupWizardEvent
    data class EditField(val field: SetupField, val value: String) : SetupWizardEvent
    data class SelectOwnership(val value: String) : SetupWizardEvent
    data class SelectGstOption(val option: Int) : SetupWizardEvent
    data object Finish : SetupWizardEvent
}

/** Every text field the wizard captures. S18 — input was previously never wired (§5 fix). */
enum class SetupField {
    COMPANY_NAME, HEAD_OFFICE, PHONE, EMAIL,
    GSTIN, PAN,
    BRANCH_NAME, BRANCH_ADDRESS, BRANCH_CODE,
    VEHICLE_NUMBER, CAPACITY, DRIVER_NAME, DRIVER_PHONE,
}
