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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.NestedCard
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.DeliveryType
import com.example.transportapp.core.ui.sample.Party
import com.example.transportapp.core.ui.sample.Risk
import com.example.transportapp.domain.transport.PaymentMode

@Composable
fun BookingFormScreen(
    onClose: () -> Unit,
    onBooked: (String) -> Unit,
    onSetRate: (() -> Unit)? = null,
    viewModel: BookingFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val bookedBiltyNo by viewModel.bookedBiltyNo.collectAsState()
    LaunchedEffect(bookedBiltyNo) {
        bookedBiltyNo?.let { no ->
            onBooked(no)
            viewModel.consumeBookedBiltyNo()
        }
    }
    BookingFormContent(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onBookAndPrint = { viewModel.onEvent(BookingFormEvent.Submit) },
        onSetRate = onSetRate ?: {},
    )
    // S21: the Add-charge dialog — label + rupee amount, taxed as its own line.
    if (state.showAddCharge) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onEvent(BookingFormEvent.DismissAddCharge) },
            title = { Text("Add a charge", style = com.example.transportapp.core.designsystem.theme.TransportTypeScale.titleMedium) },
            text = {
                Column {
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = state.chargeLabel,
                        onValueChange = { viewModel.onEvent(BookingFormEvent.ChangeChargeLabel(it)) },
                        label = "Charge name (e.g. Loading, Waybill)"
                    )
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = state.chargeAmount,
                        onValueChange = { viewModel.onEvent(BookingFormEvent.ChangeChargeAmount(it)) },
                        label = "Amount (₹)",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Added before GST on this booking.",
                        style = com.example.transportapp.core.designsystem.theme.TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = state.chargeLabel.isNotBlank() && (state.chargeAmount.toLongOrNull() ?: 0L) > 0,
                    onClick = { viewModel.onEvent(BookingFormEvent.SaveManualCharge) }
                ) { Text("Add", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onEvent(BookingFormEvent.DismissAddCharge) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BookingFormContent(
    state: BookingFormUiState,
    onEvent: (BookingFormEvent) -> Unit,
    onClose: () -> Unit,
    onBookAndPrint: () -> Unit,
    onSetRate: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Column {
                if (state.provisionalWarning != null) {
                    ProvisionalBanner(state.provisionalWarning)
                }
                if (state.rateCardWarning != null) {
                    RateCardBanner(state.rateCardWarning, onSetRate)
                }
                BookingStickyBar(state, onBookAndPrint)
            }
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
            item { RouteSection(state, onEvent) }
            item { GoodsWeightSection(state, onEvent) }
            item { ArticlesSection(state, onEvent) }
            item { TermsSection(state, onEvent) }
            item { ChargesSection(state, onEvent) }
            item { BookingFooter(state, onEvent) }
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
        GroupHeading("PARTIES", modifier = Modifier.padding(bottom = 12.dp))
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
                    onClear = { onEvent(BookingFormEvent.ClearConsignor) },
                    onAdd = { onEvent(BookingFormEvent.StartConsignorSearch) },
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
                    onClear = { onEvent(BookingFormEvent.ClearConsignee) },
                    onAdd = { onEvent(BookingFormEvent.StartConsigneeSearch) },
                )
            }
        }
    }
}

@Composable
private fun SelectedPartyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    party: Party?,
    onClear: () -> Unit,
    onAdd: () -> Unit = {},
) {
    NestedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (party == null) onAdd else null
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
private fun RouteSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("ROUTE", modifier = Modifier.padding(bottom = 12.dp))
        NestedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onEvent(BookingFormEvent.ToggleRoutePicker) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parts = state.routeLabel.split(" · ")
                Text(parts.getOrElse(0) { state.routeLabel }, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (parts.getOrElse(0) { "" }.contains("→")) {
                    Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp).size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (parts.size > 1) {
                        parts.drop(1).forEach { segment ->
                            Text(segment, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Tap to pick a route", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Pick route", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            // Inline route list (S14): the §B6 picker is a visible choice list, not a popup —
            // every route row is one tap away, no second window.
            if (state.showRoutePicker) {
                Column {
                    state.routeOptions.forEach { (id, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEvent(BookingFormEvent.SelectRoute(id))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoodsWeightSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    var goodsMenu by remember { mutableStateOf(false) }
    Column {
        GroupHeading("GOODS", modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(percent = 100))
                    .clickable { goodsMenu = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.goods, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Pick goods", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = goodsMenu, onDismissRequest = { goodsMenu = false }) {
                state.goodsOptions.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name, style = TransportTypeScale.bodyMedium) },
                        onClick = {
                            goodsMenu = false
                            onEvent(BookingFormEvent.SelectGoods(id))
                        },
                    )
                }
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
            state.chargeableCaption,
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
            // §10.1 volumetric: L×B×H in cm — the dated setting's divisor (6000) governs.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                TransportTextField(
                    value = state.lengthCm,
                    onValueChange = { onEvent(BookingFormEvent.ChangeLengthCm(it)) },
                    label = "Length (cm)",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    monospace = true
                )
                TransportTextField(
                    value = state.breadthCm,
                    onValueChange = { onEvent(BookingFormEvent.ChangeBreadthCm(it)) },
                    label = "Breadth (cm)",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    monospace = true
                )
                TransportTextField(
                    value = state.heightCm,
                    onValueChange = { onEvent(BookingFormEvent.ChangeHeightCm(it)) },
                    label = "Height (cm)",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    monospace = true
                )
            }
        }
    }
}

@Composable
private fun ArticlesSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            GroupHeading("MORE ARTICLES · ${state.extraItems.size}", modifier = Modifier.weight(1f))
            AppTextButton("Add article", onClick = { onEvent(BookingFormEvent.AddArticle) })
        }
        state.extraItems.forEachIndexed { index, row ->
            ContentCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                TransportTextField(
                    value = row.description,
                    onValueChange = { onEvent(BookingFormEvent.ChangeArticleDescription(index, it)) },
                    label = "Article ${index + 2} description",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TransportTextField(
                        value = row.packages,
                        onValueChange = { onEvent(BookingFormEvent.ChangeArticlePackages(index, it)) },
                        label = "Packages",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        monospace = true
                    )
                    TransportTextField(
                        value = row.weightKg,
                        onValueChange = { onEvent(BookingFormEvent.ChangeArticleWeight(index, it)) },
                        label = "Actual weight",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        monospace = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AppTextButton("Remove", onClick = { onEvent(BookingFormEvent.RemoveArticle(index)) }, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TermsSection(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        GroupHeading("TERMS", modifier = Modifier.padding(bottom = 12.dp))
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
        GroupHeading("CHARGES", trailing = {
            AppTextButton("Add charge", onClick = { onEvent(BookingFormEvent.ToggleAddCharge) })
        }, modifier = Modifier.padding(bottom = 12.dp))
        NestedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                state.charges.forEach { charge ->
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
                            IconButton(onClick = { onEvent(BookingFormEvent.RemoveCharge(charge.headCode)) }) {
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
                if (state.showRounding) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rounding", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(state.roundingLabel, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingFooter(state: BookingFormUiState, onEvent: (BookingFormEvent) -> Unit) {
    Column {
        if (state.amending != null) {
            TransportTextField(
                value = state.amendReason,
                onValueChange = { onEvent(BookingFormEvent.ChangeAmendReason(it)) },
                label = "Amendment reason · required, at least 10 characters",
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
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

/** §9's mandated provisional-numbering banner — silent renumbering is the auditor's nightmare. */
@Composable
private fun ProvisionalBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(transportColors().haulAmberContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            style = TransportTypeScale.bodySmall,
            color = transportColors().onHaulAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Design T5's fallback banner: the company default stepped in for a missing party rate. */
@Composable
private fun RateCardBanner(message: String, onSetRate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(transportColors().haulAmberContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            style = TransportTypeScale.bodySmall,
            color = transportColors().onHaulAmber,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.TextButton(onClick = onSetRate) {
            Text("Set a rate", style = TransportTypeScale.labelLarge, color = transportColors().onHaulAmber)
        }
    }
}

@Composable
private fun BookingStickyBar(state: BookingFormUiState, onBookAndPrint: () -> Unit) {
    // §7.1 amendment mode (S15): the reason is required before the successor is booked.
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(modifier = Modifier.weight(1f)) {
            if (state.amending != null) {
                Text(
                    "AMENDING ${state.amending}",
                    style = TransportTypeScale.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text("GRAND TOTAL", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "₹${state.grandTotal.formatted()}",
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
            leadingIcon = Icons.Rounded.Print,
            celebrate = true, // S20: booking is the app's peak moment — sunrise accent.
        )
        }
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