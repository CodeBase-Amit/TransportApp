package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun MastersHubScreen(
    onBack: () -> Unit,
    onMasterClick: (String) -> Unit,
    viewModel: MastersHubViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    MastersHubContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onMasterClick = onMasterClick
    )
}

@Composable
fun MastersHubContent(
    state: MastersHubUiState,
    onEvent: (MastersHubEvent) -> Unit,
    onBack: () -> Unit,
    onMasterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)
        Text(
            state.subtitle,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
        )

        state.groups.forEach { group ->
            Spacer(Modifier.height(8.dp))
            GroupHeading(group.heading, modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp)
            ) {
                group.rows.forEach { (label, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMasterClick(label) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(count, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding)
                .background(transportColors().haulAmberContainer, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Merge, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(state.duplicateBanner, style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
            AppTextButton(state.duplicateAction, onClick = { onEvent(MastersHubEvent.ReviewDuplicates) }, color = transportColors().onHaulAmber)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MastersHubPreview() {
    TransportAppTheme {
        MastersHubContent(
            state = MastersHubUiState(),
            onEvent = {},
            onBack = {},
            onMasterClick = {}
        )
    }
}
