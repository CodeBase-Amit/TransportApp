package com.example.transportapp.core.ui.sample

data class BiltyCopyConfig(
    val label: String,
    val caption: String
)

data class BiltyPaperData(
    val companyName: String,
    val addressLine: String,
    val contactLine: String,
    val consignorName: String,
    val consignorContact: String,
    val consignorGstin: String,
    val consignorAddress: String,
    val consigneeName: String,
    val consigneeContact: String,
    val consigneeGstin: String,
    val consigneeAddress: String,
    val docNo: String,
    val date: String,
    val fromStation: String,
    val toStation: String,
    val goodsHeaders: List<String>,
    val goodsValues: List<String>,
    val freight: String,
    val hamali: String,
    val doorDelivery: String,
    val taxable: String,
    val gst: String,
    val rounding: String,
    val totalLabel: String,
    val grandTotal: String,
    val amountInWords: String,
    val stamp: String,
    val footer: String
)

/**
 * T6 Bilty preview demo data — the printed lettersheet and the four copy
 * labels/captions. Reuses shared SampleData values where they already match.
 */
object BiltySampleData {

    const val BILTY_NO = SampleData.BILTY_NO

    val copyConfigs = listOf(
        BiltyCopyConfig("CONSIGNOR", "Consignor copy"),
        BiltyCopyConfig("CONSIGNEE", "Consignee copy"),
        BiltyCopyConfig("DRIVER", "Driver copy"),
        BiltyCopyConfig("OFFICE", "Office copy")
    )

    val paper = BiltyPaperData(
        companyName = "SHIVSHAKTI ROADLINES",
        addressLine = "123 Transport Nagar, A.B. Road, Indore (M.P.) - 452010",
        contactLine = "Ph: 0731-2555555 | GSTIN: 23AAAAA1234A1Z5",
        consignorName = SampleData.CONSIGNOR,
        consignorContact = "Indore · ${SampleData.CONSIGNOR_PHONE}",
        consignorGstin = "GSTIN ${SampleData.CONSIGNOR_GSTIN}",
        consignorAddress = "Loha Bhavan, Indore",
        consigneeName = SampleData.CONSIGNEE,
        consigneeContact = "Nashik · ${SampleData.CONSIGNEE_PHONE}",
        consigneeGstin = "GSTIN ${SampleData.CONSIGNEE_GSTIN}",
        consigneeAddress = "MIDC Ambad, Nashik",
        docNo = SampleData.BILTY_NO,
        date = "25.08.2026",
        fromStation = "INDORE",
        toStation = "NASHIK",
        goodsHeaders = listOf("Pkg", "Description", "Actual Wt.", "Charged Wt.", "Rate", "Amount"),
        goodsValues = listOf("12", "MS PIPES", "780 kg", "780 kg", "4.50", "3,510.00"),
        freight = "3,510.00",
        hamali = "96.00",
        doorDelivery = "150.00",
        taxable = "3,756.00",
        gst = "187.80",
        rounding = "0.20",
        totalLabel = "Grand Total",
        grandTotal = "3,944.00",
        amountInWords = "Rupees three thousand nine hundred forty four only",
        stamp = "TO PAY",
        footer = "At owner's risk · Door delivery · Private mark DST-114 · E-way bill 281047556392"
    )
}
