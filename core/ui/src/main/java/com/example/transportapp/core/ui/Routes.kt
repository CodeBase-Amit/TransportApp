package com.example.transportapp.core.ui

import android.net.Uri

// Navigation route constants matching TransportApp.md §6 + Design.md §C1.
//
// NOTE: document numbers like "IND/2627/04185" contain "/" which is a path separator.
// Every helper that builds a route with an argument MUST Uri.encode the value so the
// route stays a single path segment; navigation decodes it back on read.

object Routes {
    // Auth & onboarding
    const val SPLASH = "splash"                         // T0
    const val CAROUSEL = "carousel"                     // T32
    const val SIGN_IN = "sign_in"                       // T1
    const val COMPANY_PICKER = "company_picker"         // T2
    const val SETUP_WIZARD = "setup_wizard"             // T3
    const val PROFILE = "profile"                       // T33

    // Main
    const val DASHBOARD = "dashboard"                   // T4
    const val BOOKING_FORM = "booking_form"             // T5
    const val BILTY_PREVIEW = "bilty_preview/{biltyNo}" // T6
    const val REGISTER = "register"                     // T7
    const val CASE_FILE = "case_file/{biltyNo}"         // T8
    const val STATUS_SHEET = "status_sheet/{biltyNo}"   // T9
    const val CHALLAN_BUILDER = "challan_builder"       // T10
    const val CHALLAN_DETAIL = "challan_detail/{challanNo}" // T11
    const val VEHICLE_BOARD = "vehicle_board"           // T12

    // Money
    const val UNBILLED_POOL = "unbilled_pool"           // T13
    const val FREIGHT_BILL = "freight_bill/{billId}"    // T14 — a draft or an issued record
    const val PAYMENTS = "payments"                     // T15
    const val STATEMENT = "statement/{partyId}"         // T16

    // Masters
    const val MASTERS_HUB = "masters_hub"               // T17
    const val MASTER_LIST = "master_list/{type}"        // T18
    const val MASTER_EDITOR = "master_editor/{type}/{id}" // T19
    const val RATE_CARD_EDITOR = "rate_card_editor/{partyId}" // T20

    // Reports
    const val REPORTS_HUB = "reports_hub"               // T21
    const val REPORT_VIEWER = "report_viewer/{reportId}" // T22
    const val EXPORT_CENTRE = "export_centre"           // T23

    // Settings
    const val SETTINGS_HUB = "settings_hub"             // T24
    const val COMPANY_PROFILE = "company_profile"       // T25
    const val BRANCHES = "branches"                     // T26
    const val MEMBERS = "members"                       // T27
    const val NUMBERING = "numbering"                   // T28
    const val TEMPLATES = "templates"                   // T29
    const val TEMPLATE_REQUESTS = "template_requests"   // T30
    const val ACCOUNT_DATA = "account_data"             // T31

    // Dev / verification — every screen, one click away
    const val SCREEN_INDEX = "screen_index"

    fun biltyPreview(biltyNo: String) = "bilty_preview/${Uri.encode(biltyNo)}"
    fun caseFile(biltyNo: String) = "case_file/${Uri.encode(biltyNo)}"
    fun statusSheet(biltyNo: String) = "status_sheet/${Uri.encode(biltyNo)}"
    fun challanDetail(challanNo: String) = "challan_detail/${Uri.encode(challanNo)}"
    fun freightBill(billId: String) = "freight_bill/${Uri.encode(billId)}"
    fun statement(partyId: String) = "statement/${Uri.encode(partyId)}"
    fun masterList(type: String) = "master_list/${Uri.encode(type)}"
    fun masterEditor(type: String, id: String) = "master_editor/${Uri.encode(type)}/${Uri.encode(id)}"
    fun rateCardEditor(partyId: String) = "rate_card_editor/${Uri.encode(partyId)}"
    fun reportViewer(reportId: String) = "report_viewer/${Uri.encode(reportId)}"
}