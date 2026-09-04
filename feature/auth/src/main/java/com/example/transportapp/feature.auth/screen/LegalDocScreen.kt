package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * S21 — Terms of Service and Privacy Policy as static offline pages. The sign-in
 * screen's links were dead; these are the honest offline-first answer (the hosted
 * versions replace the content when the online tier lands).
 */
@Composable
fun LegalDocScreen(
    title: String,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = title, onNavigationClick = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (title.equals("Terms", ignoreCase = true)) {
                LegalBody(
                    sections = listOf(
                        "1. Your data stays yours" to
                            "Everything you book, bill, and collect is stored on this device first. We sync it to your company's own workspace only — never to anyone else's.",
                        "2. Fair use" to
                            "The app is licensed to your company for its own transport business. Reselling or repackaging the app is not permitted.",
                        "3. Numbers and money" to
                            "Document numbers are issued per branch and are legally sequential. Once a bilty is issued, its content is locked; corrections are amendments that print their own supplement.",
                        "4. Liability" to
                            "The app records what you enter. Disputes about goods, freight, or delivery are between you and your customers under your consignment note's own terms.",
                        "5. Changes" to
                            "We'll tell you in the app before any change that affects your data or numbering. Your local data is never deleted by an update.",
                    )
                )
            } else {
                LegalBody(
                    sections = listOf(
                        "What we store" to
                            "Company identity, parties, consignments, bills, receipts, and the documents you print. Stored on this device and synced to your company's workspace when you have signal.",
                        "What we never store" to
                            "Aadhaar or PAN numbers. Location is recorded only to the nearest town when you attach it to a status update.",
                        "Who sees your data" to
                            "Only members of your company, scoped by their role and branch. Each company's register is separate and private.",
                        "Deleting your data" to
                            "A non-Owner can leave the company; the device copy is removed on sign-out. An Owner can delete the company, which follows the statutory retention rule rather than erasing issued financial documents.",
                        "Contact" to
                            "Questions about your data: your app administrator first, or the support address on the company profile screen.",
                    )
                )
            }
        }
    }
}

@Composable
private fun LegalBody(sections: List<Pair<String, String>>) {
    sections.forEach { (heading, body) ->
        Text(heading, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

