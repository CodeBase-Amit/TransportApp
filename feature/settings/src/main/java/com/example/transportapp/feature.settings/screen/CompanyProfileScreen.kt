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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T25 — Company profile. Live letterhead preview in paper colours.
 */
@Composable
fun CompanyProfileScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Text("Company profile", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton("Save", onClick = {})
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            Text("HOW IT WILL PRINT", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(16.dp)
            ) {
                Column {
                    Box(modifier = Modifier.size(64.dp).background(PaperColors.paperRule.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text("SR", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Shivshakti Roadlines", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
                    Text("Plot 41, Transport Nagar, Indore 452003 · GSTIN 23AABCS4521M1Z9 · PAN AABCS4521M", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
                    Text("+91 731 2589 041 · office@shivshaktiroadlines.in", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
                }
            }

            GroupHeading("Identity")
            TransportTextField("Shivshakti Roadlines", {}, "Legal name")
            TransportTextField("", {}, "Trade name")
            TransportTextField("Proprietorship", {}, "Constitution")

            GroupHeading("Address")
            TransportTextField("Plot 41, Transport Nagar", {}, "Address", singleLine = false, maxLines = 3)
            TransportTextField("Indore", {}, "City")
            TransportTextField("Madhya Pradesh", {}, "State")
            TransportTextField("452003", {}, "Pincode", monospace = true)

            GroupHeading("Tax and registration")
            TransportTextField("23AABCS4521M1Z9", {}, "GSTIN", monospace = true, trailingIcon = Icons.Rounded.CheckCircle)
            TransportTextField("AABCS4521M", {}, "PAN", monospace = true)
            TransportTextField("", {}, "Transporter ID")

            GroupHeading("Contact as printed")
            TransportTextField("+91 731 2589 041", {}, "Phone")
            TransportTextField("+91 94250 33712", {}, "Alternate phone")
            TransportTextField("office@shivshaktiroadlines.in", {}, "Email")
            TransportTextField("", {}, "Website")

            GroupHeading("Logo")
            Box(
                modifier = Modifier.size(96.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Text("PNG or JPG, at least 600x600. It prints in black and white too, so avoid thin light lines.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            GroupHeading("What prints at the bottom")
            TransportTextField("", {}, "Footer clause", singleLine = false, maxLines = 4)
        }

        AppPrimaryButton("Save and update all templates", onClick = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}