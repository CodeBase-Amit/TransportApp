package com.example.transportapp.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.NestedCard
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.domain.transport.PaymentMode

@Composable
fun BookingFormScreen(
    onClose: () -> Unit,
    onBookAndPrint: () -> Unit,
    viewModel: BookingFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    BookingFormContent(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onBookAndPrint = onBookAndPrint
    )
}

@Composable
fun BookingFormContent(
    state: BookingFormUiState,
    onEvent: (BookingFormEvent) -> Unit,
    onClose: () -> Unit,
    onBookAndPrint: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            BookingStickyBar(state, onBookAndPrint)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.screenPadding,
                end = Dimens.screenPadding,
                bottom = Dimens.stickyBarMulti + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { BookingTopBar(state, onClose) }
            item { PartiesSection(state, onEvent) }
            item { RouteSection(state) }
            item { GoodsWeightSection(state, onEvent) }
            item { TermsSection(state, onEvent) }
            item { ChargesSection(state, onEvent) }
            item { BookingFooter(state) }
        }
    }
}

@Composable
private fun BookingTopBar(state: BookingFormUiState, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            "New bilty",
            style = TransportTypeScale.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            state.reservedNumber,
            style = TransportTypeScale.dataSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
private fun PartiesSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("Parties", modifier = Modifier.padding(bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Consignor
            if (state.isSearchingConsignor && state.consignor == null) {
                PartySearchField(
                    query = state.searchQuery,
                    onQuery = { onEvent(BookingFormEvent.SearchConsignor(it)) },
                    onSelect = { onEvent(BookingFormEvent.SelectConsignor(it)) },
                    results = state.searchResults
                )
            } else {
                SelectedPartyCard(
                    icon = Icons.Rounded.Upload,
                    party = state.consignor,
                    onClear = { onEvent(BookingFormEvent.ClearConsignor) }
                )
            }
            // Consignee
            if (state.isSearchingConsignee && state.consignee == null) {
                PartySearchField(
                    query = state.searchQuery,
                    onQuery = { onEvent(BookingFormEvent.SearchConsignee(it)) },
                    onSelect = { onEvent(BookingFormEvent.SelectConsignee(it)) },
                    results = state.searchResults
                )
            } else {
                SelectedPartyCard(
                    icon = Icons.Rounded.Download,
                    party = state.consignee,
                    onClear = { onEvent(BookingFormEvent.ClearConsignee) }
                )
            }
        }
    }
}

@Composable
private fun SelectedPartyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    party: Party?,
    onClear: () -> Unit
) {
    NestedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (party != null) {
                    Text(party.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${party.station} · ${party.phone} · GSTIN ${party.gstin}",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Tap to add", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PartySearchField(
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (Party) -> Unit,
    results: List<Party>
) {
    Column {
        TransportTextField(
            value = query,
            onValueChange = onQuery,
            label = "Search party",
            leadingIcon = Icons.Rounded.Search,
            placeholder = "Type 3+ characters"
        )
        if (results.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            ) {
                results.forEach { party ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(party) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(party.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${party.station} · ${party.biltyCount} bilties${party.usualRoute?.let { " · usually to ${it.split("→").last().trim()}" } ?: ""}",
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteSection(state: BookingFormUiState) {
    Column {
        GroupHeading("Route", modifier = Modifier.padding(bottom = 12.dp))
        NestedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Indore", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp).size(20.dp))
                Text("Nashik", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(
                    "585 km · usually 2 days · arrives 27 Aug",
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Also used:", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("Indore → Bhopal", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text("Indore → Pune", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GoodsWeightSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("Goods", modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(percent = 100))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("MS pipes", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "rate ${state.rate} · ${state.rateNote}",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TransportTextField(
                value = state.packages,
                onValueChange = { onEvent(BookingFormEvent.ChangePackages(it)) },
                label = "Packages",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                monospace = true
            )
            TransportTextField(
                value = state.actualWeightKg,
                onValueChange = { onEvent(BookingFormEvent.ChangeWeight(it)) },
                label = "Actual weight",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = null,
                helperText = null,
                monospace = true
            )
        }
        Text(
            "Chargeable 780 kg · minimum 500 kg on this route",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (state.weightError != null) {
            Text(
                state.weightError,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(BookingFormEvent.ToggleMoreDetails) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("More details", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "Goods value, e-way bill, private mark, dimensions",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.showMoreDetails) {
            TransportTextField(value = "", onValueChange = {}, label = "Goods value", modifier = Modifier.padding(bottom = 12.dp))
            TransportTextField(value = "", onValueChange = {}, label = "E-way bill number", modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun TermsSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("Terms", modifier = Modifier.padding(bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Payment", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    SegmentedControl(
                        options = listOf(
                            PaymentMode.PAID to "Paid",
                            PaymentMode.TOPAY to "To Pay",
                            PaymentMode.TBB to "TBB"
                        ),
                        selected = state.paymentMode,
                        onSelect = { onEvent(BookingFormEvent.ChangePaymentMode(it)) }
                    )
                }
                Spacer(Modifier.width(12.dp))
                PaymentStamp(mode = state.paymentMode)
            }
            Column {
                Text("Risk", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                SegmentedControl(
                    options = listOf(Risk.OWNER to "Owner's", Risk.CARRIER to "Carrier's"),
                    selected = state.risk,
                    onSelect = { onEvent(BookingFormEvent.ChangeRisk(it)) }
                )
            }
            Column {
                Text("Delivery", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                SegmentedControl(
                    options = listOf(DeliveryType.GODOWN to "Godown", DeliveryType.DOOR to "Door"),
                    selected = state.delivery,
                    onSelect = { onEvent(BookingFormEvent.ChangeDelivery(it)) }
                )
            }
        }
    }
}

@Composable
private fun ChargesSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("Charges", trailing = {
            AppTextButton("Add charge", onClick = {})
        }, modifier = Modifier.padding(bottom = 12.dp))
        NestedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                state.charges.forEachIndexed { index, charge ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(charge.label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            if (charge.detail.isNotEmpty()) {
                                Text(charge.detail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            charge.amount.formatted(),
                            style = TransportTypeScale.dataMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (charge.isRemovable) {
                            IconButton(onClick = { onEvent(BookingFormEvent.RemoveCharge(index)) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Taxable", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(state.taxable.formatted(), style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.gstLabel, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(state.gst.formatted(), style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rounding", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("+${state.rounding.formatted()}", style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BookingFooter(state: BookingFormUiState) {
    Column {
        Text(
            "Booked by ${state.bookedBy}",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Reserved number ${state.reservedNumber} · this number is used even if you're offline",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun BookingStickyBar(state: BookingFormUiState, onBookAndPrint: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("GRAND TOTAL", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                state.grandTotal.formatted(),
                style = TransportTypeScale.dataLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                state.amountInWords,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AppPrimaryButton(
            text = "Book and print",
            onClick = onBookAndPrint,
            leadingIcon = Icons.Rounded.Print
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingFormPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        BookingFormContent(
            state = BookingFormUiState(),
            onEvent = {},
            onClose = {},
            onBookAndPrint = {}
        )
    }
}