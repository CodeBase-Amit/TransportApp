package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ErrorBanner
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T1 — Sign in. The only screen a signed-out user sees after the carousel.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    errorMessage: String? = null,
    loading: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("SR", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(24.dp))
        Text("Book a bilty in under a minute", style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Four printed copies, a live register, and every challan and freight bill built from the same form.",
            style = TransportTypeScale.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        // Three reassurances
        ReassuranceRow(Icons.Rounded.CloudOff, "Works offline", "Bilties save on the phone and sync when there's signal")
        Spacer(Modifier.height(16.dp))
        ReassuranceRow(Icons.Rounded.Lock, "Only your staff see your data", "Each company's register is separate and private")
        Spacer(Modifier.height(16.dp))
        ReassuranceRow(Icons.Rounded.Print, "Prints on your own letterhead", "Real A4 output, sharp on paper and as PDF")

        Spacer(Modifier.weight(1f))

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage, modifier = Modifier.padding(bottom = 16.dp))
        }

        // Google sign-in button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(percent = 100))
                .then(
                    if (loading) Modifier else Modifier
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Google G mark placeholder
                Box(
                    modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (loading) "Signing in…" else "Continue with Google",
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("By continuing you agree to our ", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppTextButton("Terms", onClick = onTerms)
            Text(" and ", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppTextButton("Privacy Policy", onClick = onPrivacy)
        }
    }
}

@Composable
private fun ReassuranceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}