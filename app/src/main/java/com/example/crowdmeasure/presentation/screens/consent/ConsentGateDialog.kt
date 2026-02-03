package com.example.crowdmeasure.presentation.screens.consent

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.util.AppPermissions

@Composable
fun ConsentGateDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    vm: ConsentGateViewModel
) {
    if (!visible) return

    val ctx = LocalContext.current
    val settings = vm.settings.collectAsState().value

    val consentAccepted = settings?.consentAccepted ?: false
    val collectionEnabled = settings?.collectionEnabled ?: false

    var coarseGranted by remember { mutableStateOf(AppPermissions.hasCoarseLocation(ctx)) }
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(ctx)) }

    // Refresh permission status whenever the dialog becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            coarseGranted = AppPermissions.hasCoarseLocation(ctx)
            phoneGranted = AppPermissions.hasPhoneState(ctx)
        }
    }

    val requestCoarse = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> coarseGranted = granted }

    val requestPhone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> phoneGranted = granted }

    val canFinish = consentAccepted && collectionEnabled

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
        title = { Text("Privacy & opt-in") },
        text = {
            ConsentGateContent(
                consentAccepted = consentAccepted,
                collectionEnabled = collectionEnabled,
                onConsent = vm::setConsent,
                onCollection = vm::setCollection,
                coarseGranted = coarseGranted,
                phoneGranted = phoneGranted,
                requestCoarse = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                requestPhone = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) }
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = canFinish
            ) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

@Composable
private fun ConsentGateContent(
    consentAccepted: Boolean,
    collectionEnabled: Boolean,
    onConsent: (Boolean) -> Unit,
    onCollection: (Boolean) -> Unit,
    coarseGranted: Boolean,
    phoneGranted: Boolean,
    requestCoarse: () -> Unit,
    requestPhone: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Intro / promise
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CrowdMeasure is opt-in only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "We collect network/performance measurements to improve quality insights.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bullet("No personal identifiers collected")
                    Bullet("You can pause collection anytime")
                    Bullet("Optional permissions improve measurement quality")
                }
            }
        }

        // Main controls
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                SettingSwitchRow(
                    title = "I agree and opt in",
                    subtitle = "Required to enable measurement collection",
                    checked = consentAccepted,
                    onCheckedChange = onConsent
                )
                Divider()
                SettingSwitchRow(
                    title = "Enable data collection",
                    subtitle = "Starts collecting measurements in the background",
                    checked = collectionEnabled,
                    enabled = consentAccepted,
                    onCheckedChange = onCollection
                )
            }
        }

        // Optional permissions
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Optional permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "You can grant these now or later in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                PermissionRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Coarse location",
                    subtitle = "Improves regional accuracy (not precise GPS)",
                    granted = coarseGranted,
                    enabled = consentAccepted,
                    onRequest = requestCoarse,
                    buttonText = "Grant"
                )

                Spacer(Modifier.height(10.dp))

                PermissionRow(
                    icon = Icons.Filled.PhoneAndroid,
                    title = "Phone state",
                    subtitle = "Enables cell metrics for better signal insights",
                    granted = phoneGranted,
                    enabled = consentAccepted,
                    onRequest = requestPhone,
                    buttonText = "Grant"
                )
            }
        }

        // Hint if user is stuck
        if (!consentAccepted) {
            AssistChip(
                onClick = { onConsent(true) },
                label = { Text("Tap “I agree” to continue") }
            )
        } else if (!collectionEnabled) {
            AssistChip(
                onClick = { onCollection(true) },
                label = { Text("Enable collection to finish") }
            )
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
    // Entire row is clickable (better UX than tiny switch target)
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    enabled: Boolean,
    onRequest: () -> Unit,
    buttonText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                if (granted) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        OutlinedButton(
            onClick = onRequest,
            enabled = enabled && !granted
        ) {
            Text(if (granted) "Granted" else buttonText)
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}