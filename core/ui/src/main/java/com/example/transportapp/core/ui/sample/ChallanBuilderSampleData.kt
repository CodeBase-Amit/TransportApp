package com.example.transportapp.core.ui.sample

import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

object ChallanBuilderSampleData {

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

    val filterChips = listOf("Booked here", "Arrived from elsewhere", "By station")

    val reservedNumber = "CHL/IND/2627/00742"
    val readyToLoad = "Ready to load · 23 at Indore"

    val vehicleNumber = SampleData.VEHICLE
    val vehicleOwnership = "Own · 9,000 kg"
    val driverName = SampleData.DRIVER
    val driverLicenceLine = "Licence ${SampleData.DRIVER_LICENCE} · ${SampleData.DRIVER_PHONE}"

    val routeTo = "Bhiwandi"
    val routeVia = "Dhule"
    val lorryHire = "18,500"
    val advancePaid = "12,000"
    val balance = "6,500.00"

    val capacityKg = SampleData.VEHICLE_CAPACITY_KG
    val maxStations = 3
    val freightTotal = "41,880.00"
    const val selectAll = "Select all"
    const val createChallan = "Create challan"
    const val consignmentsSuffix = "consignments"
}
