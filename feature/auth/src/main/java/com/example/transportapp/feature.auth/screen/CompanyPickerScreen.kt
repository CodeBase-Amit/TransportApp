package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T2 — Company and branch picker. Branch is chosen inside the company card.
 */
@Composable
fun CompanyPickerScreen(
    onCompanySelected: (String) -> Unit,
    onRegisterCompany: () -> Unit,
    onSignOut: () -> Unit
) {
    val companies = SampleData.companies
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedBranch by remember { mutableStateOf("Indore") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your companies", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = onSignOut) {
                Icon(Icons.Rounded.Logout, contentDescription = "Sign out", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        GroupHeading("Companies you work at", modifier = Modifier.padding(bottom = 12.dp))

        companies.forEachIndexed { index, company ->
            CompanyCard(
                company = company,
                isSelected = index == selectedIndex,
                selectedBranch = if (index == selectedIndex) selectedBranch else null,
                onSelect = { selectedIndex = index },
                onSelectBranch = { selectedBranch = it },
                onOpen = { onCompanySelected(company.name) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading("Invitations", modifier = Modifier.padding(bottom = 12.dp))
        SampleData.invitations.forEach { invitation ->
            InvitationCard(invitation)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
        AppOutlinedButton(
            "Register a new company",
            onClick = onRegisterCompany,
            leadingIcon = Icons.Rounded.AddBusiness,
            modifier = Modifier.fillMaxWidth(),
            labelColor = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompanyCard(
    company: SampleData.CompanyRow,
    isSelected: Boolean,
    selectedBranch: String?,
    onSelect: () -> Unit,
    onSelectBranch: (String) -> Unit,
    onOpen: () -> Unit
) {
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val fill = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(fill, RoundedCornerShape(20.dp))
            .border(border, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
            .padding(Dimens.cardPaddingStandard)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(company.initials, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(company.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(company.roleLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isSelected) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isSelected && company.branches.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(Modifier.height(12.dp))
            Text("WORKING AT", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                company.branches.forEach { branch ->
                    FilterChip(
                        label = branch,
                        selected = branch == selectedBranch,
                        onClick = { onSelectBranch(branch) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Bilty series ", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(company.series, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            AppPrimaryButton("Open ${company.name}", onClick = onOpen, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InvitationCard(invitation: SampleData.Invitation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(Dimens.cardPaddingStandard)
    ) {
        Text(invitation.companyName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Invited by ${invitation.invitedBy} as ${invitation.role} · expires in ${invitation.expiresIn}",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppPrimaryButton("Accept", onClick = {}, modifier = Modifier.weight(1f), height = 48.dp)
            AppOutlinedButton("Decline", onClick = {}, modifier = Modifier.weight(1f), height = 48.dp)
        }
    }
}