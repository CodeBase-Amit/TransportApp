package com.example.transportapp.feature.booking.screen

import com.example.transportapp.core.ui.sample.BiltyCopyConfig
import com.example.transportapp.core.ui.sample.BiltyPaperData
import com.example.transportapp.core.ui.sample.BiltySampleData

data class BiltyPreviewUiState(
    val biltyNo: String = BiltySampleData.BILTY_NO,
    val copyCount: Int = BiltySampleData.copyConfigs.size,
    val copyConfigs: List<BiltyCopyConfig> = BiltySampleData.copyConfigs,
    val grandTotalFormatted: String = BiltySampleData.paper.grandTotal,
    val paper: BiltyPaperData = BiltySampleData.paper
)
