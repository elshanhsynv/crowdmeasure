package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.ui.components.SectionCard
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    vm: SettingsViewModel = hiltViewModel()
) {
    val settings = vm.settings.collectAsState().value
    val ctx = LocalContext.current

    var endpoint by remember(settings?.endpointUrl) { mutableStateOf(settings?.endpointUrl ?: "") }
    var autoHoursText by remember(settings?.autoRunIntervalHours) { mutableStateOf((settings?.autoRunIntervalHours ?: 6).toString()) }
    var exportNText by remember { mutableStateOf("50") }

    val coarseGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val requestCoarseLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        coarseGranted.value = granted
    }

    LaunchedEffect(Unit) { vm.ensureMaintenanceScheduled() }

    Column(
        Modifier.padding(contentPadding).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        // ✅ NEW: Opt-in & permissions in Settings
        SectionCard {
            Text("Privacy & consent", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val consentAccepted = settings?.consentAccepted ?: false
            val collectionEnabled = settings?.collectionEnabled ?: false

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("I accept and opt in")
                Switch(
                    checked = consentAccepted,
                    onCheckedChange = { vm.setConsent(it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable data collection")
                Switch(
                    checked = collectionEnabled,
                    onCheckedChange = { vm.setCollection(it) },
                    enabled = consentAccepted
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Optional: allow coarse location to attach approximate coordinates to a measurement.")
            Spacer(Modifier.height(8.dp))

            Text("Coarse location permission: ${if (coarseGranted.value) "Granted" else "Not granted"}")

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { requestCoarseLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                enabled = consentAccepted
            ) { Text("Grant coarse location") }
        }

        SectionCard {
            Text("Test Endpoint URL", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.saveEndpoint(endpoint) }) { Text("Save") }
            Spacer(Modifier.height(6.dp))
            Text("Note: must be HTTPS.")
        }

        SectionCard {
            Text("Collection rules", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val wifiOnly = settings?.collectOnlyWifi ?: false
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Collect only on Wi-Fi")
                Switch(checked = wifiOnly, onCheckedChange = { vm.setCollectOnlyWifi(it) })
            }
        }

        SectionCard {
            Text("Auto-run", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val enabled = settings?.autoRunEnabled ?: false

            val consentAccepted = settings?.consentAccepted == true
            val collectionEnabled = settings?.collectionEnabled == true
            val autoRunAllowed = consentAccepted && collectionEnabled

            if (!autoRunAllowed) {
                Text("Enable consent + collection to allow auto-run.")
                Spacer(Modifier.height(8.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable auto-run")
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        val hours = autoHoursText.toIntOrNull() ?: 6
                        vm.setAutoRunEnabled(it, hours)
                    },
                    enabled = autoRunAllowed
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = autoHoursText,
                onValueChange = { autoHoursText = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text("Every X hours") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val hours = autoHoursText.toIntOrNull() ?: 6
                    vm.setAutoRunEnabled(enabled, hours)
                },
                enabled = autoRunAllowed
            ) { Text("Apply interval") }

            Spacer(Modifier.height(6.dp))
            Text("Privacy: no background collection unless auto-run is enabled.")
        }

        SectionCard {
            Text("Export", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exportNText,
                onValueChange = { exportNText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Export last N records") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val n = exportNText.toIntOrNull()?.coerceIn(1, 10_000) ?: 50
                vm.exportLastN(ctx, n)
            }) { Text("Export to JSON & share") }

            Spacer(Modifier.height(8.dp))
            when (val st = vm.exportState.collectAsState().value) {
                UiState.Idle -> {}
                UiState.Loading -> Text("Exporting…")
                is UiState.Success -> Text("Sharesheet opened.")
                is UiState.Error -> Text("Export error: ${st.message}")
            }
        }

        SectionCard {
            Text("Delete my data", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::deleteMyData) { Text("Delete local DB data") }
        }
    }
}