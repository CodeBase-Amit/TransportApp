package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.SetupWizardSampleData

data class SetupWizardUiState(
    val step: Int = 0,
    val stepLabels: List<String> = SetupWizardSampleData.STEP_LABELS,
    val title: String = SetupWizardSampleData.TITLE,
    val skipLabel: String = SetupWizardSampleData.SKIP,
    val nextLabel: String = SetupWizardSampleData.NEXT,
    val finishLabel: String = SetupWizardSampleData.FINISH,
    val addVehiclesLaterLabel: String = SetupWizardSampleData.ADD_VEHICLES_LATER,
    // Step 1 — Company
    val companyName: String = SetupWizardSampleData.COMPANY_NAME,
    val headOffice: String = SetupWizardSampleData.HEAD_OFFICE,
    val phone: String = SetupWizardSampleData.PHONE,
    val email: String = SetupWizardSampleData.EMAIL,
    val printHeading: String = SetupWizardSampleData.PRINT_HEADING,
    val printName: String = SetupWizardSampleData.PRINT_NAME,
    val printPhoneLabel: String = SetupWizardSampleData.PRINT_PHONE_LABEL,
    val printDoc: String = SetupWizardSampleData.PRINT_DOC,
    // Step 2 — Tax
    val gstin: String = SetupWizardSampleData.GSTIN,
    val pan: String = SetupWizardSampleData.PAN,
    val gstHeading: String = SetupWizardSampleData.GST_HEADING,
    val gstForwardLabel: String = SetupWizardSampleData.GST_FORWARD,
    val gstReverseLabel: String = SetupWizardSampleData.GST_REVERSE,
    val gstOption: Int = 0,
    val gstNote: String = SetupWizardSampleData.GST_NOTE,
    val gstThirdParty: String = SetupWizardSampleData.GST_THIRD_PARTY,
    // Step 3 — Branch
    val branchName: String = SetupWizardSampleData.BRANCH_NAME,
    val branchAddress: String = SetupWizardSampleData.BRANCH_ADDRESS,
    val branchCode: String = SetupWizardSampleData.BRANCH_CODE,
    val branchHeading: String = SetupWizardSampleData.BRANCH_HEADING,
    val branchPrefix: String = SetupWizardSampleData.BRANCH_CODE,
    val branchFy: String = SetupWizardSampleData.BRANCH_FY_PART,
    val branchDigits: String = SetupWizardSampleData.BRANCH_DIGITS,
    val nextBiltyLabel: String = SetupWizardSampleData.NEXT_BILTY_LABEL,
    val nextBilty: String = SetupWizardSampleData.NEXT_BILTY,
    // Step 4 — Vehicle
    val vehicleNumber: String = SetupWizardSampleData.VEHICLE_NUMBER,
    val ownershipLabel: String = SetupWizardSampleData.OWNERSHIP_LABEL,
    val ownershipOptions: List<String> = SetupWizardSampleData.OWNERSHIP_OPTIONS,
    val ownership: String = SetupWizardSampleData.OWNERSHIP_DEFAULT,
    val capacity: String = SetupWizardSampleData.CAPACITY,
    val capacityUnit: String = SetupWizardSampleData.CAPACITY_UNIT,
    val driverName: String = SetupWizardSampleData.DRIVER_NAME,
    val driverPhone: String = SetupWizardSampleData.DRIVER_PHONE
)

sealed interface SetupWizardEvent {
    data object Next : SetupWizardEvent
    data class SelectOwnership(val value: String) : SetupWizardEvent
    data class SelectGstOption(val option: Int) : SetupWizardEvent
    data object Finish : SetupWizardEvent
}
