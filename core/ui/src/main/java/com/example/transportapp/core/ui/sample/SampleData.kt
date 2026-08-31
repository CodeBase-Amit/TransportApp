package com.example.transportapp.core.ui.sample

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.Weight
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import com.example.transportapp.domain.transport.Role

/**
 * The canonical demo dataset (Design.md §B6). Every screen reuses these exact
 * names and numbers so the product reads as one.
 */
object SampleData {

    const val COMPANY = "Shivshakti Roadlines"
    const val COMPANY_ADDRESS = "Transport Nagar, Indore 452003"
    const val GSTIN = "23AABCS4521M1Z9"
    const val PAN = "AABCS4521M"

    const val BRANCH_INDORE = "Indore"
    const val BRANCH_NAGPUR = "Nagpur"
    const val BRANCH_BHIWANDI = "Bhiwandi"

    const val USER_NAME = "Mahesh Patidar"
    const val USER_EMAIL = "mahesh.patidar@gmail.com"
    val USER_ROLE = Role.OWNER
    const val USER_INITIALS = "MP"

    const val CONSIGNOR = "Deepak Steel Traders"
    const val CONSIGNOR_PHONE = "+91 94250 61183"
    const val CONSIGNOR_GSTIN = "23AACDS8812K1Z4"
    const val CONSIGNOR_STATION = "Indore"

    const val CONSIGNEE = "Nashik Hardware Mart"
    const val CONSIGNEE_PHONE = "+91 98600 27419"
    const val CONSIGNEE_GSTIN = "27AAFCN3390L1Z8"
    const val CONSIGNEE_STATION = "Nashik"

    const val BILTY_NO = "IND/2627/04188"
    const val RESERVED_BILTY_NO = "IND/2627/04189"
    const val CHALLAN_NO = "CHL/IND/2627/00742"
    const val FREIGHT_BILL_NO = "FB/IND/2627/00311"
    const val RECEIPT_NO = "RCPT/IND/2627/00518"

    const val ROUTE_LINE = "Indore → Nashik"
    const val ROUTE_DISTANCE = "585 km"
    const val ROUTE_TRANSIT_DAYS = "usually 2 days"
    const val EXPECTED_ARRIVAL = "27 Aug"

    const val GOODS = "MS pipes"
    const val PACKAGES = 12
    const val ACTUAL_WEIGHT_KG = 780
    val ACTUAL_WEIGHT = Weight.fromKg(780)
    const val RATE_PER_KG = 4.50
    const val MINIMUM_FREIGHT = 500

    const val TERMS_PAYMENT = "TO PAY"
    val PAYMENT_MODE = PaymentMode.TOPAY
    val STATUS = ConsignmentStatus.IN_TRANSIT

    val FREIGHT = Money.fromRupees(3510)
    val HAMALI = Money.fromRupees(96)
    val DOOR_DELIVERY = Money.fromRupees(150)
    val TAXABLE = Money.fromRupees(3756)
    val GST = Money.fromRupees(187, 80)
    val ROUNDING = Money.fromRupees(0, 20)
    val GRAND_TOTAL = Money.fromRupees(3944)
    const val AMOUNT_IN_WORDS = "Three thousand nine hundred forty four rupees only"

    const val VEHICLE = "MH 15 BK 4412"
    const val VEHICLE_CAPACITY_KG = 9000
    const val DRIVER = "Gurmeet Singh"
    const val DRIVER_LICENCE = "MH1520190004512"
    const val DRIVER_PHONE = "+91 90280 41176"

    const val LORRY_HIRE = 18500.0
    const val ADVANCE = 12000.0
    const val BALANCE = 6500.0

    val registerRows = listOf(
        RegisterRow("IND/2627/04188", "Nashik Hardware Mart", "3,944.00", "Indore", "Nashik", ConsignmentStatus.IN_TRANSIT, PaymentMode.TOPAY, "12 pkg · 780 kg"),
        RegisterRow("IND/2627/04187", "Bhusawal Cement Agency", "12,180.00", "Indore", "Bhusawal", ConsignmentStatus.AT_HUB, PaymentMode.TBB, "40 pkg · 4,200 kg"),
        RegisterRow("IND/2627/04186", "Sai Electricals", "6,750.00", "Indore", "Dhule", ConsignmentStatus.DELIVERED, PaymentMode.PAID, "8 pkg · 310 kg"),
        RegisterRow("IND/2627/04185", "Nashik Hardware Mart", "2,410.00", "Indore", "Dhule", ConsignmentStatus.HELD, PaymentMode.TOPAY, "6 pkg · 180 kg", exception = "Shortage — 1 bundle short"),
        RegisterRow("IND/2627/04184", "Vidarbha Traders", "18,920.00", "Nagpur", "Nashik", ConsignmentStatus.OUT_FOR_DELIVERY, PaymentMode.TBB, "55 pkg · 6,100 kg"),
        RegisterRow("IND/2627/04183", "Kalyan Steel Mart", "4,060.00", "Indore", "Kalyan", ConsignmentStatus.RETURNED, PaymentMode.TOPAY, "9 pkg · 420 kg")
    )

    val caseFileEvents = listOf(
        CaseEvent("Booked", "Indore", "25 Aug, 11:42 AM", "Mahesh Patidar", StepState.DONE),
        CaseEvent("Loaded", "Indore", "25 Aug, 2:10 PM", "on CHL/IND/2627/00742", StepState.DONE),
        CaseEvent("In transit", "departed Indore", "25 Aug, 3:05 PM", "Ramesh Yadav", StepState.DONE),
        CaseEvent("At hub", "Dhule", "26 Aug, 8:40 AM", "Iqbal Shaikh", StepState.DONE),
        CaseEvent("In transit", "departed Dhule", "26 Aug, 10:15 AM", "Iqbal Shaikh", StepState.DONE),
        CaseEvent("Arrived", "Nashik", "expected 27 Aug", "—", StepState.UPCOMING)
    )

    val journeySteps = listOf(
        RouteLineStep("Booked", StepState.DONE),
        RouteLineStep("Loaded", StepState.DONE),
        RouteLineStep("In transit", StepState.CURRENT),
        RouteLineStep("At hub", StepState.UPCOMING),
        RouteLineStep("Arrived", StepState.UPCOMING),
        RouteLineStep("Delivered", StepState.UPCOMING)
    )

    val mastersCounts = listOf(
        "Parties" to "1,284", "Stations" to "96", "Routes" to "141", "Branches" to "3",
        "Goods types" to "38", "Charge heads" to "9", "Rate cards" to "64",
        "Vehicles" to "22", "Drivers" to "17"
    )

    // ── T3 Setup wizard ────────────────────────────────────────────────
    const val COMPANY_NAME = "Shivshakti Roadlines"
    const val HEAD_OFFICE = "Plot 14, Transport Nagar, Indore, Madhya Pradesh 452003"
    const val COMPANY_PHONE = "+91 94250 61183"
    const val COMPANY_EMAIL = "office@shivshaktiroadlines.in"
    const val BRANCH_CODE = "IND"
    const val FY_PART = "2627"
    const val VEHICLE_NUMBER = "MH 15 BK 4412"

    // ── T12 Vehicle board ──────────────────────────────────────────────
    data class VehicleRow(
        val number: String,
        val ownership: String,
        val isLate: Boolean,
        val lateLine: String? = null,
        val stops: List<String> = emptyList(),
        val currentStop: Int = 0,
        val driver: String,
        val load: String,
        val challan: String? = null,
        val idleDays: Int? = null,
        val lastTrip: String? = null
    )

    val vehicles = listOf(
        VehicleRow("MH 15 BK 4412", "Own", true, "1 day late · expected 24 Aug, 6:00 PM", listOf("Indore", "Dhule", "Bhiwandi"), 1, "Gurmeet Singh", "8,120 kg", "CHL/IND/2627/00742"),
        VehicleRow("RJ 14 PA 3345", "Own", false, null, listOf("Nagpur", "Jaipur"), 0, "Balwinder Singh", "6,400 kg", "CHL/NAG/2627/00219"),
        VehicleRow("GJ 05 KT 8891", "Attached", false, null, listOf("Indore", "Ahmedabad"), 2, "Rafiq Mansuri", "4,900 kg", "CHL/IND/2627/00741"),
        VehicleRow("MP 09 GH 2207", "Own", false, null, emptyList(), 0, "—", "—", null, idleDays = 6, lastTrip = "19 Aug")
    )

    // ── T10 Challan builder ────────────────────────────────────────────
    data class LoadableConsignment(
        val docNumber: String,
        val consignee: String,
        val packages: String,
        val weight: String,
        val amount: String,
        val paymentMode: PaymentMode,
        val status: ConsignmentStatus,
        val isOnwardLeg: Boolean = false,
        val onwardNote: String? = null
    )

    val loadable = listOf(
        LoadableConsignment("IND/2627/04188", "Nashik Hardware Mart", "12 pkg", "780 kg", "3,944.00", PaymentMode.TOPAY, ConsignmentStatus.BOOKED),
        LoadableConsignment("IND/2627/04191", "Sai Electricals, Dhule", "8 pkg", "310 kg", "1,180.00", PaymentMode.PAID, ConsignmentStatus.BOOKED),
        LoadableConsignment("IND/2627/04192", "Vidarbha Traders, Nashik", "55 pkg", "6,100 kg", "18,920.00", PaymentMode.TBB, ConsignmentStatus.BOOKED),
        LoadableConsignment("IND/2627/04193", "Kalyan Steel Mart", "9 pkg", "930 kg", "4,060.00", PaymentMode.TOPAY, ConsignmentStatus.BOOKED),
        LoadableConsignment("IND/2627/04187", "Bhusawal Cement Agency", "40 pkg", "4,200 kg", "12,180.00", PaymentMode.TBB, ConsignmentStatus.AT_HUB, isOnwardLeg = true, onwardNote = "already on CHL/IND/2627/00738 to Dhule")
    )

    // ── T11 Challan detail ─────────────────────────────────────────────
    const val CHALLAN_ROUTE = "Indore → Bhiwandi via Dhule · 588 km"
    const val CHALLAN_CREATED = "created 25 Aug, 1:50 PM by Mahesh Patidar"
    const val CHALLAN_DISPATCHED = "dispatched 25 Aug, 3:05 PM · expected Bhiwandi 27 Aug, 6:00 PM"
    const val CHALLAN_CONSIGNMENTS = 14
    const val CHALLAN_LOAD_KG = "8,120 kg"
    const val CHALLAN_HIRE = "18,500.00"
    const val CHALLAN_BALANCE = "6,500.00"

    data class ChallanStationGroup(val station: String, val count: Int, val rows: List<ChallanRow>)
    data class ChallanRow(val bilty: String, val consignee: String, val weight: String, val mode: PaymentMode)

    val challanGroups = listOf(
        ChallanStationGroup("DHULE", 4, listOf(
            ChallanRow("IND/2627/04191", "Sai Electricals", "310 kg", PaymentMode.PAID),
            ChallanRow("IND/2627/04185", "Nashik Hardware Mart", "180 kg", PaymentMode.TOPAY),
            ChallanRow("IND/2627/04189", "Dhule Agro", "1,120 kg", PaymentMode.PAID)
        )),
        ChallanStationGroup("NASHIK", 7, listOf(
            ChallanRow("IND/2627/04188", "Nashik Hardware Mart", "780 kg", PaymentMode.TOPAY),
            ChallanRow("IND/2627/04192", "Vidarbha Traders", "6,100 kg", PaymentMode.TBB)
        )),
        ChallanStationGroup("BHIWANDI", 3, emptyList())
    )

    // ── T13 Unbilled pool ──────────────────────────────────────────────
    data class UnbilledParty(
        val name: String,
        val total: String,
        val consignments: Int,
        val period: String,
        val branches: String,
        val ageBuckets: Triple<Int, Int, Int>, // 0-30, 31-60, 60+
        val oldestDays: Int,
        val selected: Boolean,
        val expanded: Boolean = false,
        val allOver60: Boolean = false,
        val rows: List<UnbilledRow> = emptyList()
    )

    data class UnbilledRow(val bilty: String, val route: String, val date: String, val amount: String)

    val unbilledParties = listOf(
        UnbilledParty(
            "Deepak Steel Traders", "86,420.00", 23, "1 Jul to 31 Jul", "Indore and Nagpur",
            Triple(15, 5, 3), 41, selected = true, expanded = true,
            rows = listOf(
                UnbilledRow("IND/2627/04180", "Indore → Nashik", "25 Aug", "3,944.00"),
                UnbilledRow("IND/2627/04179", "Indore → Nashik", "24 Aug", "4,120.00"),
                UnbilledRow("IND/2627/04178", "Indore → Bhiwandi", "23 Aug", "6,750.00")
            )
        ),
        UnbilledParty(
            "Vidarbha Traders", "1,24,180.00", 34, "1 Jul to 31 Jul", "Nagpur",
            Triple(10, 12, 12), 38, selected = false,
            rows = emptyList()
        ),
        UnbilledParty(
            "Bhusawal Cement Agency", "41,900.00", 18, "1 Jun to 31 Jul", "Indore",
            Triple(0, 0, 18), 72, selected = false, allOver60 = true,
            rows = emptyList()
        )
    )

    // ── T14 Freight bill ───────────────────────────────────────────────
    const val BILL_PARTY = "Deepak Steel Traders"
    const val BILL_GSTIN = "23AACDS8812K1Z4"
    const val BILL_PERIOD = "1–31 Jul"
    const val BILL_CONSIGNMENTS = 23
    const val BILL_FREIGHT = "86,420.00"
    const val BILL_GST = "4,321.00"
    const val BILL_TOTAL = "90,741.00"
    const val BILL_DUE = "30 days · 30 Sep 2026"

    // ── T15 Payments ───────────────────────────────────────────────────
    data class ToPayRow(
        val bilty: String,
        val consignee: String,
        val amount: String,
        val mode: PaymentMode,
        val status: ConsignmentStatus,
        val collectable: Boolean = true,
        val caption: String? = null
    )

    val toPayRows = listOf(
        ToPayRow("IND/2627/04188", "Nashik Hardware Mart", "3,944.00", PaymentMode.TOPAY, ConsignmentStatus.ARRIVED),
        ToPayRow("IND/2627/04185", "Nashik Hardware Mart", "2,410.00", PaymentMode.TOPAY, ConsignmentStatus.HELD, collectable = false, caption = "Held — collect only after the shortage is settled"),
        ToPayRow("IND/2627/04177", "Pune Auto Parts", "6,120.00", PaymentMode.TOPAY, ConsignmentStatus.ARRIVED),
        ToPayRow("IND/2627/04176", "Jalgaon Traders", "1,860.00", PaymentMode.TOPAY, ConsignmentStatus.OUT_FOR_DELIVERY)
    )

    val receiptRows = listOf(
        Triple("RCPT/IND/2627/00518", "Deepak Steel Traders", "50,000.00"),
        Triple("RCPT/IND/2627/00517", "Vidarbha Traders", "32,400.00"),
        Triple("RCPT/IND/2627/00516", "Sai Electricals", "6,750.00")
    )

    // ── T16 Statement ──────────────────────────────────────────────────
    const val OPENING_BALANCE = "3,42,100.00 Dr"
    const val CLOSING_BALANCE = "3,26,441.00 Dr"
    const val OVER90_AGEING = "1,92,300.00 is over 90 days old"

    data class LedgerRow(val date: String, val docNo: String, val desc: String, val debit: String, val credit: String, val balance: String)
    val ledgerRows = listOf(
        LedgerRow("12 Jul", "FB/IND/2627/00298", "freight bill", "22,400.00", "—", "3,64,500.00"),
        LedgerRow("18 Jul", "RCPT/IND/2627/00412", "NEFT", "—", "18,000.00", "3,46,500.00"),
        LedgerRow("25 Jul", "FB/IND/2627/00301", "freight bill", "15,800.00", "—", "3,62,300.00"),
        LedgerRow("02 Aug", "RCPT/IND/2627/00490", "cheque", "—", "25,000.00", "3,37,300.00"),
        LedgerRow("09 Aug", "CN/IND/2627/00007", "credit note", "—", "3,200.00", "3,34,100.00"),
        LedgerRow("12 Aug", "FB/IND/2627/00308", "freight bill", "42,341.00", "—", "3,76,441.00"),
        LedgerRow("20 Aug", "RCPT/IND/2627/00518", "NEFT", "—", "50,000.00", "3,26,441.00")
    )

    // ── T4 Dashboard ───────────────────────────────────────────────────
    data class DashTile(val label: String, val value: String, val qualifier: String, val money: Boolean = false, val amberBar: Boolean = false)
    val dashboardTiles = listOf(
        DashTile("Running services", "4", "next arrival 26 Aug, 9:00 AM"),
        DashTile("In transit", "61", "812 packages"),
        DashTile("Booked today", "23", "18,340 kg · 74,210.00"),
        DashTile("To Pay to collect", "41,760.00", "9 consignments at Indore", money = true, amberBar = true),
        DashTile("Unbilled freight", "3,86,540.00", "oldest 41 days", money = true, amberBar = true),
        DashTile("Receivable", "12,84,900.00", "1,92,300.00 over 90 days", money = true, amberBar = true),
        DashTile("Exceptions", "3", "last 30 days"),
        DashTile("Overdue arrivals", "7", "past expected date"),
        DashTile("Vehicles idle", "2", "more than 5 days"),
        DashTile("This month", "24,18,600.00", "vs 17,02,400.00 hire", money = true)
    )

    data class DashException(val title: String, val body: String, val isLate: Boolean = false)
    val dashboardExceptions = listOf(
        DashException("IND/2627/04185 held at Dhule", "Shortage — 1 bundle short · 24 Aug, 3:20 PM"),
        DashException("MH 15 BK 4412 is 1 day late", "Indore → Bhiwandi · expected 24 Aug, 6:00 PM", isLate = true)
    )

    // ── T21 Reports ────────────────────────────────────────────────────
    data class ReportGroup(val heading: String, val reports: List<Triple<String, String, String?>>)
    val reportGroups = listOf(
        ReportGroup("HOW MUCH DID WE EARN", listOf(
            Triple("Freight register", "every bilty with its charges", "4,18,72,140.00"),
            Triple("Revenue by route", "which lanes actually pay", null),
            Triple("Revenue by party", "your top twenty customers", null),
            Triple("Revenue by branch", "Indore, Nagpur, Bhiwandi side by side", null)
        )),
        ReportGroup("WHAT DO PEOPLE OWE US", listOf(
            Triple("Outstanding by party", "with ageing buckets", "62,14,900.00"),
            Triple("To Pay pending", "goods delivered, money not collected", "41,760.00"),
            Triple("Ageing summary", "0-30, 31-60, 61-90, over 90", null)
        )),
        ReportGroup("WHAT DID IT COST US", listOf(
            Triple("Lorry hire register", "every challan with hire and balance", "18,42,000.00"),
            Triple("Vehicle-wise cost", "own vehicles against attached", null),
            Triple("Driver advances outstanding", "who is holding company money", null)
        )),
        ReportGroup("WHAT THE DEPARTMENT WILL ASK", listOf(
            Triple("GST outward summary", "taxable value and tax by rate", null),
            Triple("GSTR-1 style B2B listing", "invoice-wise with GSTINs", null),
            Triple("Income and expense summary", "for the return", null),
            Triple("Document number continuity", "gaps and cancellations", null)
        ))
    )

    // ── T22 Report viewer ──────────────────────────────────────────────
    data class FreightRow(val bilty: String, val date: String, val consignor: String, val consignee: String, val route: String, val pkg: String, val weight: String, val freight: String, val gst: String, val total: String, val cancelled: Boolean = false)
    val freightRows = listOf(
        FreightRow("IND/2627/04188", "25 Aug", "Deepak Steel Traders", "Nashik Hardware Mart", "Indore → Nashik", "12", "780 kg", "3,510.00", "187.80", "3,944.00"),
        FreightRow("IND/2627/04187", "25 Aug", "Bhusawal Cement", "Bhusawal Cement Agency", "Indore → Bhusawal", "40", "4,200 kg", "10,000.00", "500.00", "10,500.00"),
        FreightRow("IND/2627/04186", "24 Aug", "Sai Electricals", "Sai Electricals, Dhule", "Indore → Dhule", "8", "310 kg", "5,500.00", "275.00", "5,775.00"),
        FreightRow("IND/2627/04185", "24 Aug", "Nashik Hardware", "Nashik Hardware Mart", "Indore → Dhule", "6", "180 kg", "2,100.00", "105.00", "2,205.00", cancelled = true)
    )

    // ── T23 Exports ────────────────────────────────────────────────────
    val exportSheets = listOf("Freight register", "Bilty-wise charge detail", "Freight bills issued", "Bill line items", "Money receipts", "Receipt allocations", "To Pay collections", "Lorry hire register", "Driver advances", "Party master with GSTINs", "GST outward summary", "Document number continuity")

    // ── T24 Settings ───────────────────────────────────────────────────
    data class SettingsGroup(val heading: String, val rows: List<SettingsRow>)
    data class SettingsRow(val icon: String, val label: String, val value: String? = null, val locked: Boolean = false, val gate: String? = null, val syncIcon: Boolean = false)

    val settingsGroups = listOf(
        SettingsGroup("THE COMPANY", listOf(
            SettingsRow("business", "Company profile", "Shivshakti Roadlines"),
            SettingsRow("account_balance", "Branches", "3"),
            SettingsRow("group", "Members and roles", "4 active, 1 invited"),
            SettingsRow("numbers", "Numbering series", "6")
        )),
        SettingsGroup("DOCUMENTS", listOf(
            SettingsRow("description", "Templates", "5 installed"),
            SettingsRow("photo_camera", "Template requests", "1 quoted"),
            SettingsRow("print", "Print settings", "A4, 4 copies"),
            SettingsRow("article", "Terms and conditions", null)
        )),
        SettingsGroup("THIS PHONE", listOf(
            SettingsRow("language", "Language", "English"),
            SettingsRow("text_fields", "Text size", "System"),
            SettingsRow("dark_mode", "Theme", "Follow system"),
            SettingsRow("print", "Printer", "Not set up"),
            SettingsRow("sync", "Sync", "3 changes waiting", syncIcon = true)
        )),
        SettingsGroup("ACCOUNT", listOf(
            SettingsRow("folder", "Account and data", null),
            SettingsRow("help", "Help and how-to", null),
            SettingsRow("info", "About", "v1.0.4 (118)")
        ))
    )

    // ── T26 Branches ───────────────────────────────────────────────────
    data class BranchRow(val name: String, val isHeadOffice: Boolean, val address: String, val members: Int, val openBiltes: Int, val toPay: String, val series: List<String>, val hasNoMembers: Boolean = false)
    val branches = listOf(
        BranchRow("Indore", true, "Plot 41, Transport Nagar, Indore 452003", 3, 41, "41,760.00", listOf("IND/2627/*", "CHL/IND/2627/*", "FB/IND/2627/*", "RCPT/IND/2627/*")),
        BranchRow("Nagpur", false, "Wadi Naka, Nagpur 440023", 1, 18, "96,200.00", listOf("NAG/2627/*", "CHL/NAG/2627/*", "FB/NAG/2627/*", "RCPT/NAG/2627/*")),
        BranchRow("Bhiwandi", false, "Kalyan Road, Bhiwandi 421302", 0, 0, "0.00", listOf("BHW/2627/*", "CHL/BHW/2627/*", "FB/BHW/2627/*", "RCPT/BHW/2627/*"), hasNoMembers = true)
    )

    // ── T27 Members ────────────────────────────────────────────────────
    data class MemberRow(val name: String, val email: String, val scope: String, val role: String, val isSelf: Boolean = false, val invited: Boolean = false, val invitedBy: String? = null, val invitedRole: String? = null, val invitedDate: String? = null)
    val members = listOf(
        MemberRow("Mahesh Patidar", "mahesh@shivshaktiroadlines.in", "All branches", "Owner", isSelf = true),
        MemberRow("Sunita Jain", "sunita@shivshaktiroadlines.in", "All branches", "Manager"),
        MemberRow("Ramesh Yadav", "ramesh.y@shivshaktiroadlines.in", "Indore only", "Booking Clerk"),
        MemberRow("Iqbal Shaikh", "iqbal.s@shivshaktiroadlines.in", "Nagpur only", "Delivery Clerk"),
        MemberRow("accounts.bhiwandi@gmail.com", "accounts.bhiwandi@gmail.com", "Bhiwandi only", "Accountant", invited = true, invitedBy = "Mahesh Patidar", invitedDate = "22 Aug")
    )

    // ── T28 Numbering ──────────────────────────────────────────────────
    data class SeriesRow(val label: String, val nextNumber: String, val prefix: String, val fy: String, val lastUsed: String, val issued: Int, val remaining: Int, val neverUsed: Boolean = false)
    val seriesList = listOf(
        SeriesRow("Bilty · Indore", "IND/2627/04189", "IND", "2627", "04188", 4188, 812),
        SeriesRow("Bilty · Nagpur", "NAG/2627/01942", "NAG", "2627", "01941", 1941, 59),
        SeriesRow("Challan · Indore", "CHL/IND/2627/00743", "CHL/IND", "2627", "00742", 742, 258),
        SeriesRow("Freight bill · Indore", "FB/IND/2627/00312", "FB/IND", "2627", "00311", 311, 689),
        SeriesRow("Money receipt · Indore", "RCPT/IND/2627/00519", "RCPT/IND", "2627", "00518", 518, 482),
        SeriesRow("Bilty · Bhiwandi", "BHW/2627/00001", "BHW", "2627", "—", 0, 50, neverUsed = true)
    )

    // ── T29 Templates ──────────────────────────────────────────────────
    data class TemplateRow(val name: String, val type: String, val copies: String, val paper: String, val version: String, val inUse: String, val isDefault: Boolean = false, val neverPrinted: Boolean = false, val tags: List<String>)
    val templates = listOf(
        TemplateRow("Shivshakti Bilty 4-copy", "Bilty", "4 copies", "A4 portrait", "v3", "4,188 documents", isDefault = true, tags = listOf("GST")),
        TemplateRow("Shivshakti Bilty · without GST", "Bilty", "4 copies", "A4 portrait", "v1", "0 documents", neverPrinted = true, tags = listOf()),
        TemplateRow("Loading challan A4", "Challan", "2 copies", "A4 portrait", "v1", "—", tags = listOf()),
        TemplateRow("Freight bill with annexure", "Bill", "2 copies", "A4 portrait", "v1", "—", tags = listOf()),
        TemplateRow("Money receipt half-page", "Receipt", "2 copies", "A5 landscape", "v1", "—", tags = listOf())
    )
    data class VersionHistory(val version: String, val date: String, val author: String, val change: String)
    val templateVersions = listOf(
        VersionHistory("v3", "02 Aug 2026", "Shivshakti support", "GSTIN moved above the address so it prints inside the window of your envelopes. Consignee block widened by 12mm."),
        VersionHistory("v2", "19 May 2026", "Shivshakti support", "Added the private mark column. Freight table rows reduced from 10 to 8 to fit."),
        VersionHistory("v1", "14 Apr 2026", "Shivshakti support", "First version.")
    )

    // ── T30 Template requests ──────────────────────────────────────────
    data class TemplateRequest(val id: String, val description: String, val status: String, val step: Int, val sentDate: String, val quotedDate: String? = null, val quotedAmount: String? = null)
    val templateRequests = listOf(
        TemplateRequest("TR-2026-0037", "Bilty · 4 copies · from your own book", "QUOTED", 2, "20 Aug", "22 Aug", "2,500.00")
    )
    data class PastRequest(val id: String, val description: String, val date: String, val status: String)
    val pastRequests = listOf(
        PastRequest("TR-2026-0025", "Loading challan A4", "15 Jul 2026", "INSTALLED"),
        PastRequest("TR-2026-0012", "Bilty with GST", "12 Jun 2026", "CANCELLED")
    )

    // ── T31 Account ────────────────────────────────────────────────────
    data class SyncQueueItem(val description: String, val time: String, val state: String)
    val syncQueue = listOf(
        SyncQueueItem("Bilty IND/2627/04188 booked", "11:38 AM", "Pending sync"),
        SyncQueueItem("Status updated to Loaded", "11:40 AM", "Syncing"),
        SyncQueueItem("Party Nashik Hardware Mart edited", "11:41 AM", "Pending sync")
    )
}

data class RegisterRow(
    val docNumber: String,
    val consignee: String,
    val amount: String,
    val from: String,
    val to: String,
    val status: ConsignmentStatus,
    val paymentMode: PaymentMode?,
    val caption: String,
    val exception: String? = null,
    val syncPending: Boolean = false
)

data class CaseEvent(
    val name: String,
    val station: String,
    val time: String,
    val actor: String,
    val state: StepState
)
