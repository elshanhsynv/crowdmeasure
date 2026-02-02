package com.example.crowdmeasure.presentation.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.ui.components.PrimaryButton
import com.example.crowdmeasure.presentation.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    val settings = vm.settings.collectAsState().value
    val consentAccepted = settings?.consentAccepted ?: false
    val collectionEnabled = settings?.collectionEnabled ?: false

    val requestCoarseLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional, one-shot; ok if denied */ }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Welcome") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Text("What we collect (opt-in only)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "We collect ONLY:\n" +
                            "• Network context (Wi-Fi/cell, metered, VPN, captive portal)\n" +
                            "• Optional coarse location (approximate, one-shot)\n" +
                            "• Cell/Wi-Fi radio stats (signal, band/channel if available)\n" +
                            "• Performance timings to your chosen endpoint (DNS/TCP/TLS/TTFB + RTT/jitter/loss)\n" +
                            "No contacts, SMS, call logs, installed apps, MAC/IMEI, ad IDs, or precise GPS."
                )
            }

            SectionCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("I accept and opt in", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = consentAccepted, onCheckedChange = { vm.setConsentAccepted(it) })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable data collection", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = collectionEnabled,
                        onCheckedChange = { vm.setCollectionEnabled(it) },
                        enabled = consentAccepted
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Optional: grant coarse location to attach approximate coordinates to a measurement.")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { requestCoarseLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    enabled = consentAccepted
                ) { Text("Grant coarse location") }
            }

            PrimaryButton(
                text = "Continue",
                onClick = onDone,
                enabled = consentAccepted
            )
        }
    }
}