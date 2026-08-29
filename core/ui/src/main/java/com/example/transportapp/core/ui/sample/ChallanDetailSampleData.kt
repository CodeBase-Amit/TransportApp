package com.example.transportapp.core.ui.sample

import com.example.transportapp.domain.transport.PaymentMode

object ChallanDetailSampleData {

    data class ChallanRow(val bilty: String, val consignee: String, val weight: String, val mode: PaymentMode)
    data class ChallanStationGroup(val station: String, val count: Int, val rows: List<ChallanRow>)
    data class BiltyLine(val bilty: String, val dest: String, val weight: String)

    val challanNo = SampleData.CHALLAN_NO

    val challanRouteFrom = "Indore"
    val challanRouteTo = "Bhiwandi"
    val challanRouteVia = "via Dhule · 588 km"

    val createdLine = SampleData.CHALLAN_CREATED
    val dispatchedLine = SampleData.CHALLAN_DISPATCHED

    val consignments = SampleData.CHALLAN_CONSIGNMENTS
    val loadKg = SampleData.CHALLAN_LOAD_KG
    val hire = SampleData.CHALLAN_HIRE
    val balance = SampleData.CHALLAN_BALANCE

    val whatsLoadedTitle = "What's loaded · 14"
    val whatsLoadedAction = "Group by station"
    val showAll = "Show all 14"
    val editLoad = "Edit load"

    val vehicleAndDriverHeading = "Vehicle and Driver"

    val vehicleNumber = SampleData.VEHICLE
    val vehicleOwnership = "Own · 9,000 kg"
    val driverInitials = "GS"
    val driverLine = "${SampleData.DRIVER} · ${SampleData.DRIVER_PHONE}"

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

    val paperCompany = "TRANSPORT CORPORATION"
    val paperDocType = "LOADING CHALLAN"
    val paperChallanNo = "Challan No: ${SampleData.CHALLAN_NO}"
    val paperVehicle = "Vehicle ${SampleData.VEHICLE}"

    val paperBiltyLines = listOf(
        BiltyLine("BL/4092", "Dhule", "1,200 kg"),
        BiltyLine("BL/4105", "Nashik", "850 kg")
    )
    val paperSeeFull = "See full challan"

    val dispatchedNotice = "Balance 6,500.00 payable to the driver when the trip closes."

    val statusOpen = "Open"
}
