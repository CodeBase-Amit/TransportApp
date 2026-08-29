package com.example.transportapp.core.ui.sample

import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

object PaymentsSampleData {

    data class ToPayRow(
        val bilty: String,
        val consignee: String,
        val amount: String,
        val mode: PaymentMode,
        val status: ConsignmentStatus,
        val collectable: Boolean = true,
        val caption: String? = null
    )

    val title = "Payments"
    val subtitle = "Today · 25 Aug"

    val toPayTab = "To Pay"
    val billReceiptsTab = "Bill receipts"

    val toCollect = "41,760.00"
    val atIndore = "9"

    val toPayRows = listOf(
        ToPayRow("IND/2627/04188", "Nashik Hardware Mart", "3,944.00", PaymentMode.TOPAY, ConsignmentStatus.IN_TRANSIT),
        ToPayRow("IND/2627/04187", "Bhusawal Cement Agency", "12,180.00", PaymentMode.TBB, ConsignmentStatus.AT_HUB),
        ToPayRow("IND/2627/04185", "Nashik Hardware Mart", "2,410.00", PaymentMode.TOPAY, ConsignmentStatus.HELD, collectable = false, caption = "Held – collect only after the shortage is settled"),
        ToPayRow("IND/2627/04177", "Pune Auto Parts", "6,120.00", PaymentMode.TOPAY, ConsignmentStatus.ARRIVED),
        ToPayRow("IND/2627/04176", "Jalgaon Traders", "1,860.00", PaymentMode.TOPAY, ConsignmentStatus.OUT_FOR_DELIVERY)
    )

    val receivedThisMonth = "4,18,200.00"
    val receipts = "27"
    val receiptModeLine = "NEFT · 20 Aug"

    val receiptRows = listOf(
        Triple("RCPT/IND/2627/00518", "Deepak Steel Traders", "50,000.00"),
        Triple("RCPT/IND/2627/00517", "Vidarbha Traders", "32,400.00"),
        Triple("RCPT/IND/2627/00516", "Sai Electricals", "6,750.00")
    )
}
