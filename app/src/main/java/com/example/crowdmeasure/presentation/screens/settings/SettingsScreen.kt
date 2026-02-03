package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.ui.components.SectionCard
import com.example.crowdmeasure.presentation.util.UiState
import com.example.crowdmeasure.presentation.util.AppPermissions

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val settings = vm.settings.collectAsState().value

    var endpoint by remember(settings?.endpointUrl) { mutableStateOf(settings?.endpointUrl ?: "") }
    var autoHoursText by remember(settings?.autoRunIntervalHours) {
        mutableStateOf((settings?.autoRunIntervalHours ?: 6).toString())
    }
    var exportNText by remember { mutableStateOf("50") }

    // Permission states (refreshable)
    var coarseGranted by remember { mutableStateOf(AppPermissions.hasCoarseLocation(ctx)) }
    var fineGranted by remember { mutableStateOf(hasFineLocation(ctx)) }
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(ctx)) }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(ctx)) }

    fun refreshPermissionStates() {
        coarseGranted = AppPermissions.hasCoarseLocation(ctx)
        fineGranted = hasFineLocation(ctx)
        phoneGranted = AppPermissions.hasPhoneState(ctx)
        locationServicesOn = isLocationServicesEnabled(ctx)
    }

    val requestCoarse = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissionStates() }

    val requestFine = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissionStates() }

    val requestPhone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissionStates() }

    LaunchedEffect(Unit) {
        vm.ensureMaintenanceScheduled()
        refreshPermissionStates()
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrivacyAndPermissionsCard(
            consentAccepted = settings?.consentAccepted ?: false,
            collectionEnabled = settings?.collectionEnabled ?: false,
            onConsentChange = vm::setConsent,
            onCollectionChange = vm::setCollection,
            coarseGranted = coarseGranted,
            fineGranted = fineGranted,
            phoneGranted = phoneGranted,
            locationServicesOn = locationServicesOn,
            onGrantCoarse = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            onGrantFine = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onGrantPhone = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) },
            onRefresh = ::refreshPermissionStates
        )

        SectionCard {
            Text("Test endpoint", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS URL") },
                singleLine = true,
                supportingText = { Text("Must be HTTPS.") }
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.saveEndpoint(endpoint) }) { Text("Save") }
        }

        SectionCard {
            Text("Collection rules", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val wifiOnly = settings?.collectOnlyWifi ?: false
            SettingSwitchRow(
                title = "Collect only on Wi-Fi",
                subtitle = "Avoid mobile data usage",
                checked = wifiOnly,
                onCheckedChange = vm::setCollectOnlyWifi
            )
        }

        SectionCard {
            Text("Auto-run", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val enabled = settings?.autoRunEnabled ?: false
            val consentAccepted = settings?.consentAccepted == true
            val collectionEnabled = settings?.collectionEnabled == true
            val autoRunAllowed = consentAccepted && collectionEnabled

            if (!autoRunAllowed) {
                Text("Enable consent and data collection to allow auto-run.")
                Spacer(Modifier.height(8.dp))
            }

            SettingSwitchRow(
                title = "Enable auto-run",
                subtitle = "Runs collection periodically in the background",
                checked = enabled,
                enabled = autoRunAllowed,
                onCheckedChange = {
                    val hours = autoHoursText.toIntOrNull()?.coerceIn(1, 999) ?: 6
                    vm.setAutoRunEnabled(it, hours)
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = autoHoursText,
                onValueChange = { autoHoursText = it.filter(Char::isDigit).take(3) },
                label = { Text("Every X hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("Common values: 6, 12, 24") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val hours = autoHoursText.toIntOrNull()?.coerceIn(1, 999) ?: 6
                    vm.setAutoRunEnabled(enabled, hours)
                },
                enabled = autoRunAllowed
            ) { Text("Apply interval") }
        }

        SectionCard {
            Text("Export", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = exportNText,
                onValueChange = { exportNText = it.filter(Char::isDigit).take(5) },
                label = { Text("Export last N records") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                is UiState.Success -> Text("Share sheet opened.")
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

@Composable
private fun PrivacyAndPermissionsCard(
    consentAccepted: Boolean,
    collectionEnabled: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onCollectionChange: (Boolean) -> Unit,
    coarseGranted: Boolean,
    fineGranted: Boolean,
    phoneGranted: Boolean,
    locationServicesOn: Boolean,
    onGrantCoarse: () -> Unit,
    onGrantFine: () -> Unit,
    onGrantPhone: () -> Unit,
    onRefresh: () -> Unit
) {
    SectionCard {
        Text("Privacy & permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Text(
            "CrowdMeasure collects network/performance measurements only when you opt in. " +
                    "Optional permissions improve measurement quality.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        SettingSwitchRow(
            title = "I agree and opt in",
            subtitle = "Required before enabling collection",
            checked = consentAccepted,
            onCheckedChange = onConsentChange
        )

        Divider()

        SettingSwitchRow(
            title = "Enable data collection",
            subtitle = "Allows background measurement collection",
            checked = collectionEnabled,
            enabled = consentAccepted,
            onCheckedChange = onCollectionChange
        )

        Spacer(Modifier.height(10.dp))

        // Helpful status hint for Samsung-style gating
        if (!locationServicesOn) {
            AssistChip(
                onClick = onRefresh,
                label = { Text("Location services are OFF — cell metrics may be empty") }
            )
            Spacer(Modifier.height(8.dp))
        }

        PermissionRow(
            title = "Location (approximate)",
            subtitle = "Adds coarse coordinates to a measurement",
            granted = coarseGranted,
            enabled = consentAccepted,
            onRequest = onGrantCoarse
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            title = "Location (precise)",
            subtitle = "Often required to read detailed serving cell / signal (Samsung)",
            granted = fineGranted,
            enabled = consentAccepted,
            onRequest = onGrantFine
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            title = "Phone state",
            subtitle = "Enables additional cell metrics on some devices",
            granted = phoneGranted,
            enabled = consentAccepted,
            onRequest = onGrantPhone,
            buttonText = "Grant"
        )

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    enabled: Boolean,
    onRequest: () -> Unit,
    buttonText: String = "Grant"
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (granted) "Status: Granted" else "Status: Not granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            OutlinedButton(
                onClick = onRequest,
                enabled = enabled && !granted
            ) {
                Text(if (granted) "Granted" else buttonText)
            }
        }
    )
}

private fun hasFineLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

private fun isLocationServicesEnabled(context: Context): Boolean {
    val lm = context.getSystemService<LocationManager>() ?: return false
    return try {
        lm.isLocationEnabled
    } catch (_: Throwable) {
        val gps = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val net = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        gps || net
    }
}