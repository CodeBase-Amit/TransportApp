package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
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
    onReviewDuplicates: () -> Unit = {},
    viewModel: MastersHubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    MastersHubContent(
        state = state,
        onBack = onBack,
        onMasterClick = onMasterClick,
        onReviewDuplicates = onReviewDuplicates,
    )
}

@Composable
fun MastersHubContent(
    state: MastersHubUiState,
    onBack: () -> Unit,
    onMasterClick: (String) -> Unit,
    onReviewDuplicates: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.sectionSpacing)
    ) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)
        Text(
            state.subtitle,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = Dimens.screenPadding,
                vertical = Dimens.chipGap
            )
        )

        state.groups.forEach { group ->
            Spacer(Modifier.height(Dimens.chipGap))
            GroupHeading(
                group.heading,
                modifier = Modifier.padding(
                    horizontal = Dimens.screenPadding,
                    vertical = Dimens.chipGap
                )
            )
            ContentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding),
                contentPadding = 0.dp
            ) {
                group.rows.forEachIndexed { index, (label, count) ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = Dimens.cardPaddingStandard)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onMasterClick(label) }
                            .padding(
                                horizontal = Dimens.cardPaddingStandard,
                                vertical = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = TransportTypeScale.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            count,
                            style = TransportTypeScale.dataMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.screenPadding))
        ContentCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding),
            fill = transportColors().haulAmberContainer,
            border = null,
            contentPadding = Dimens.cardPaddingStandard
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.Merge,
                    contentDescription = null,
                    tint = transportColors().onHaulAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    state.duplicateBanner,
                    style = TransportTypeScale.bodyMedium,
                    color = transportColors().onHaulAmber,
                    modifier = Modifier.weight(1f)
                )
                AppTextButton(
                    state.duplicateAction,
                    // S27: routes to the party list prefiltered on duplicates — was a VM no-op.
                    onClick = onReviewDuplicates,
                    color = transportColors().onHaulAmber
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MastersHubPreview() {
    TransportAppTheme {
        MastersHubContent(
            state = MastersHubUiState(),
            onBack = {},
            onMasterClick = {}
        )
    }
}
