package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.LightbulbCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.crowdmeasure.presentation.ui.components.PermissionRow
import com.example.crowdmeasure.presentation.ui.components.SettingsSectionCard
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled

@Composable
internal fun PrivacySettingsTab() {
    val context = LocalContext.current

    var coarseGranted by remember { mutableStateOf(AppPermissions.hasCoarseLocation(context)) }
    var fineGranted by remember { mutableStateOf(hasFineLocation(context)) }
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(context)) }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(context)) }
    var notificationsGranted by remember {
        mutableStateOf(AppPermissions.hasPostNotifications(context))
    }

    fun refresh() {
        coarseGranted = AppPermissions.hasCoarseLocation(context)
        fineGranted = hasFineLocation(context)
        phoneGranted = AppPermissions.hasPhoneState(context)
        locationServicesOn = isLocationServicesEnabled(context)
        notificationsGranted = AppPermissions.hasPostNotifications(context)
    }

    val requestCoarse =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val requestFine =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val requestPhone =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        SettingsSectionCard(
            title = "Permissions",
            description = "Improve measurement quality",
            icon = Icons.Outlined.Lock
        ) {
            if (!locationServicesOn) {
                LocationServicesWarningBanner(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )
            }

            PermissionRow(
                title = "Coarse Location",
                subtitle = "Adds approximate coordinates to measurements",
                granted = coarseGranted,
                enabled = true,
                onRequest = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                icon = Icons.Outlined.MyLocation
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PermissionRow(
                title = "Fine Location",
                subtitle = "Required for detailed cell signal on some devices (e.g., Samsung)",
                granted = fineGranted,
                enabled = true,
                onRequest = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                icon = Icons.Outlined.PinDrop
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PermissionRow(
                title = "Phone State",
                subtitle = "Enables additional cell metrics on some devices",
                granted = phoneGranted,
                enabled = true,
                onRequest = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) },
                icon = Icons.Outlined.PhoneAndroid
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PermissionRow(
                title = "Notifications",
                subtitle = "Enables post notifications",
                granted = notificationsGranted,
                enabled = true,
                onRequest = { requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                icon = Icons.Outlined.Notifications
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { refresh() }) {
                    Text("Refresh Status")
                }
            }
        }

        PermissionUsageSection()
        TipsSection()
        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun LocationServicesWarningBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Location services are OFF - cell metrics may be empty",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = "Open location settings",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PermissionUsageSection() {
    SettingsSectionCard(
        title = "What the permissions are used for",
        description = "We use these permissions to improve measurement accuracy",
        icon = Icons.Outlined.Shield
    ) {
        val items = listOf(
            Icons.Outlined.CellTower to "Cell signal\naccuracy",
            Icons.Outlined.Map to "Location\ncontext",
            Icons.Outlined.BarChart to "Network\nanalytics",
            Icons.Outlined.Security to "Data\nquality"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(2).forEach { (icon, label) ->
                UsageFeatureChip(icon = icon, label = label, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.drop(2).forEach { (icon, label) ->
                UsageFeatureChip(icon = icon, label = label, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UsageFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TipsSection() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LightbulbCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "To get the best results, grant all permissions and keep Location Services ON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

private fun hasFineLocation(context: Context): Boolean = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED
