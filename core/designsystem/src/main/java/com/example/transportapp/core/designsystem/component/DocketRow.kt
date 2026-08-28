package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

/**
 * The 88dp docket row (Design.md §B3) — the most important row in the app.
 * Three lines: document number + amount, route + consignee, chips + packages.
 *
 * @param docNumber Bilty/challan/bill number in Plex Mono
 * @param amount Money figure right-aligned, in Plex Mono
 * @param fromStation Origin station
 * @param toStation Destination station
 * @param consignee Name of the consignee or party
 * @param status Journey status (shown as a chip)
 * @param paymentMode Payment mode (shown as a stamp)
 * @param packagesCaption e.g. "12 pkg · 780 kg"
 * @param syncPending Whether to show the Pending sync chip
 * @param exceptionCaption Optional exception text in error color
 * @param selected Whether the row is selected (multi-select mode)
 * @param onClick Click handler
 */
@Composable
fun DocketRow(
    docNumber: String,
    amount: String,
    fromStation: String,
    toStation: String,
    consignee: String,
    status: ConsignmentStatus,
    paymentMode: PaymentMode?,
    packagesCaption: String,
    modifier: Modifier = Modifier,
    syncPending: Boolean = false,
    exceptionCaption: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.rowDocket)
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Line 1: doc number + amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = docNumber,
                style = TransportTypeScale.dataMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = amount,
                style = TransportTypeScale.dataMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Line 2: route + consignee
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fromStation, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                Icons.Rounded.ArrowRightAlt,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(toStation, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "· $consignee",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Line 3: chips + caption
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JourneyChip(status = status)
            if (paymentMode != null) {
                PaymentStamp(mode = paymentMode)
            }
            Text(
                text = packagesCaption,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (syncPending) {
                SyncChip(state = SyncState.PENDING)
            }
        }
        // Exception line (hidden most of the time)
        if (exceptionCaption != null) {
            Text(
                text = exceptionCaption,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1
            )
        }
    }
}