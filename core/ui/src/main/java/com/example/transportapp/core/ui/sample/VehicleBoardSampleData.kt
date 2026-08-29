package com.example.transportapp.core.ui.sample

object VehicleBoardSampleData {

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

    val filterChips = listOf("Running", "Idle", "Own", "Attached", "Late")

    val summaryRunning = "4"
    val summaryIdle = "2"
    val summaryLate = "1"

    val title = "Vehicles"
    val newChallan = "New challan"
    val loadIt = "Load it"
}
