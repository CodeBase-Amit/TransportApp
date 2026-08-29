package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T25 — Company profile. Live letterhead preview in paper colours.
 */
@Composable
fun CompanyProfileScreen(
    onBack: () -> Unit,
    viewModel: CompanyProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    CompanyProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun CompanyProfileContent(
    state: CompanyProfileUiState,
    onEvent: (CompanyProfileEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Text(state.title, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton(state.saveLabel, onClick = { onEvent(CompanyProfileEvent.Save) })
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            Text(state.previewHeading, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(16.dp)
            ) {
                Column {
                    Box(modifier = Modifier.size(64.dp).background(PaperColors.paperRule.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text("SR", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(state.legalName, color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
                    Text("${state.address} · GSTIN: ${state.gstin} | PAN: ${state.pan}", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
                    Text("${state.phone} · ${state.email}", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
                }
            }

            GroupHeading(state.identityHeading)
            TransportTextField(state.legalName, { onEvent(CompanyProfileEvent.ChangeLegalName(it)) }, "Legal name")
            TransportTextField(state.tradeName, { onEvent(CompanyProfileEvent.ChangeTradeName(it)) }, "Trade name")
            Text("Constitution", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(state.constitutionOptions.map { it to it }, state.constitution, { onEvent(CompanyProfileEvent.ChangeConstitution(it)) })

            GroupHeading(state.addressHeading)
            TransportTextField(state.address, { onEvent(CompanyProfileEvent.ChangeAddress(it)) }, "Address", singleLine = false, maxLines = 3)
            TransportTextField(state.city, { onEvent(CompanyProfileEvent.ChangeCity(it)) }, "City")
            TransportTextField(state.pincode, { onEvent(CompanyProfileEvent.ChangePincode(it)) }, "Pincode", monospace = true)
            TransportTextField(state.state, { onEvent(CompanyProfileEvent.ChangeState(it)) }, "State")

            GroupHeading(state.taxHeading)
            TransportTextField(state.gstin, { onEvent(CompanyProfileEvent.ChangeGstin(it)) }, "GSTIN", monospace = true, trailingIcon = Icons.Rounded.CheckCircle)
            TransportTextField(state.pan, { onEvent(CompanyProfileEvent.ChangePan(it)) }, "PAN", monospace = true)
            TransportTextField(state.transporterId, { onEvent(CompanyProfileEvent.ChangeTransporterId(it)) }, "Transporter ID")

            GroupHeading(state.contactHeading)
            TransportTextField(state.phone, { onEvent(CompanyProfileEvent.ChangePhone(it)) }, "Phone")
            TransportTextField(state.altPhone, { onEvent(CompanyProfileEvent.ChangeAltPhone(it)) }, "Alternate phone")
            TransportTextField(state.email, { onEvent(CompanyProfileEvent.ChangeEmail(it)) }, "Email")
            TransportTextField(state.website, { onEvent(CompanyProfileEvent.ChangeWebsite(it)) }, "Website")

            GroupHeading(state.logoHeading)
            Box(
                modifier = Modifier.size(96.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Text(state.logoNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            GroupHeading(state.footerHeading)
            TransportTextField(state.footerClause, { onEvent(CompanyProfileEvent.ChangeFooter(it)) }, "Footer clause", singleLine = false, maxLines = 4)

            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp)).padding(20.dp)
            ) {
                Text(state.deleteTitle, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.deleteBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                com.example.transportapp.core.designsystem.component.AppOutlinedButton(
                    state.deleteTitle,
                    onClick = { onEvent(CompanyProfileEvent.RequestDelete(state.legalName)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    borderColor = MaterialTheme.colorScheme.error,
                    labelColor = MaterialTheme.colorScheme.error
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
        ) {
            AppPrimaryButton(state.saveAndUpdate, onClick = { onEvent(CompanyProfileEvent.Save) }, modifier = Modifier.fillMaxWidth())
            Text(state.templateNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompanyProfilePreview() {
    TransportAppTheme {
        CompanyProfileContent(state = CompanyProfileUiState(), onEvent = {}, onBack = {})
    }
}
