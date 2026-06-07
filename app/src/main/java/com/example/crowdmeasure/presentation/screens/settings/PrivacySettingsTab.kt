package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard
import com.example.crowdmeasure.presentation.ui.components.feedback.LocationServicesBanner
import com.example.crowdmeasure.presentation.ui.components.settings.PermissionRow
import com.example.crowdmeasure.presentation.ui.components.settings.PermissionStatus
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.hasFineLocation
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled

@Composable
internal fun PrivacySettingsTab() {
    val context = LocalContext.current

    var permissionSnapshot by remember {
        mutableStateOf(
            value = PermissionSnapshot.from(context),
            policy = neverEqualPolicy()
        )
    }

    fun refreshPermissions() {
        permissionSnapshot = PermissionSnapshot.from(context)
    }

    val requestCoarse = rememberPermissionLauncher(
        onResult = { refreshPermissions() }
    )

    val requestFine = rememberPermissionLauncher(
        onResult = { refreshPermissions() }
    )

    val requestPhoneState = rememberPermissionLauncher(
        onResult = { refreshPermissions() }
    )

    val requestNotifications = rememberPermissionLauncher(
        onResult = { refreshPermissions() }
    )

    PrivacySettingsContent(
        snapshot = permissionSnapshot,
        onRefresh = ::refreshPermissions,
        onOpenLocationSettings = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        onRequestCoarseLocation = {
            requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onRequestFineLocation = {
            requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onRequestPhoneState = {
            requestPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
        },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )
}

@Composable
private fun rememberPermissionLauncher(
    onResult: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { onResult() }
)

@Composable
private fun PrivacySettingsContent(
    snapshot: PermissionSnapshot,
    onRefresh: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRequestCoarseLocation: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.size(4.dp))

        PermissionsSection(
            snapshot = snapshot,
            onRefresh = onRefresh,
            onOpenLocationSettings = onOpenLocationSettings,
            onRequestCoarseLocation = onRequestCoarseLocation,
            onRequestFineLocation = onRequestFineLocation,
            onRequestPhoneState = onRequestPhoneState,
            onRequestNotifications = onRequestNotifications
        )

        PermissionUsageSection()

        TipsSection()

        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun PermissionsSection(
    snapshot: PermissionSnapshot,
    onRefresh: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRequestCoarseLocation: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    SettingsSectionCard(
        title = "Permissions",
        description = "Improve measurement quality",
        icon = Icons.Outlined.Lock
    ) {
        LocationServicesBanner(
            modifier = Modifier.fillMaxWidth(),
            locationServicesOn = snapshot.locationServicesOn,
            onClick = onOpenLocationSettings
        )

        val permissionItems = remember(snapshot) {
            buildPermissionItems(
                snapshot = snapshot,
                onRequestCoarseLocation = onRequestCoarseLocation,
                onRequestFineLocation = onRequestFineLocation,
                onRequestPhoneState = onRequestPhoneState,
                onRequestNotifications = onRequestNotifications
            )
        }

        PermissionList(
            items = permissionItems
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onRefresh
            ) {
                Text("Refresh Status")
            }
        }
    }
}

@Composable
private fun PermissionList(
    items: List<PermissionItem>
) {
    Column {
        items.forEachIndexed { index, item ->
            PermissionRow(
                title = item.title,
                subtitle = item.subtitle,
                status = item.status,
                onRequest = item.onRequest,
                icon = item.icon,
                buttonText = item.buttonText
            )

            if (index != items.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun buildPermissionItems(
    snapshot: PermissionSnapshot,
    onRequestCoarseLocation: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestNotifications: () -> Unit
): List<PermissionItem> {
    return buildList {
        add(
            PermissionItem(
                title = "Coarse Location",
                subtitle = "Adds approximate coordinates to measurements",
                status = if (snapshot.coarseLocationGranted) {
                    PermissionStatus.Granted
                } else {
                    PermissionStatus.NotGranted
                },
                icon = Icons.Outlined.MyLocation,
                onRequest = onRequestCoarseLocation
            )
        )

        add(
            PermissionItem(
                title = "Fine Location",
                subtitle = "Required for detailed cell signal",
                status = if (snapshot.fineLocationGranted) {
                    PermissionStatus.Granted
                } else {
                    PermissionStatus.NotGranted
                },
                icon = Icons.Outlined.PinDrop,
                onRequest = onRequestFineLocation
            )
        )

        add(
            PermissionItem(
                title = "Phone State",
                subtitle = "Enables additional cell metrics",
                status = if (snapshot.phoneStateGranted) {
                    PermissionStatus.Granted
                } else {
                    PermissionStatus.NotGranted
                },
                icon = Icons.Outlined.PhoneAndroid,
                onRequest = onRequestPhoneState
            )
        )

        add(
            PermissionItem(
                title = "Notifications",
                subtitle = if (snapshot.notificationsRuntimePermissionRequired) {
                    "Allows measurement and status notifications"
                } else {
                    "Enabled automatically on this Android version"
                },
                status = when {
                    snapshot.notificationsGranted -> PermissionStatus.Granted
                    snapshot.notificationsRuntimePermissionRequired -> PermissionStatus.NotGranted
                    else -> PermissionStatus.Disabled
                },
                icon = Icons.Outlined.Notifications,
                onRequest = onRequestNotifications,
                buttonText = "Allow"
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionUsageSection() {
    SettingsSectionCard(
        title = "What the permissions are used for",
        description = "We use these permissions to improve measurement accuracy",
        icon = Icons.Outlined.Shield
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            PermissionUsageItems.forEach { item ->
                UsageFeatureChip(
                    icon = item.icon,
                    label = item.label,
                    modifier = Modifier.weight(1f)
                )
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
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniIconBox(icon = icon)

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TipsSection() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniIconBox(
                icon = Icons.Outlined.LightbulbCircle,
                size = 44.dp,
                iconSize = 22.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Grant all permissions and keep Location Services on for the most accurate measurements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun MiniIconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 30.dp,
    iconSize: androidx.compose.ui.unit.Dp = 15.dp
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.size(size)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Immutable
private data class PermissionSnapshot(
    val coarseLocationGranted: Boolean,
    val fineLocationGranted: Boolean,
    val phoneStateGranted: Boolean,
    val notificationsGranted: Boolean,
    val notificationsRuntimePermissionRequired: Boolean,
    val locationServicesOn: Boolean
) {
    companion object {
        fun from(context: Context): PermissionSnapshot {
            val notificationsRuntimePermissionRequired =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            return PermissionSnapshot(
                coarseLocationGranted = AppPermissions.hasCoarseLocation(context),
                fineLocationGranted = hasFineLocation(context),
                phoneStateGranted = AppPermissions.hasPhoneState(context),
                notificationsGranted = !notificationsRuntimePermissionRequired ||
                        AppPermissions.hasPostNotifications(context),
                notificationsRuntimePermissionRequired = notificationsRuntimePermissionRequired,
                locationServicesOn = isLocationServicesEnabled(context)
            )
        }
    }
}

@Immutable
private data class PermissionItem(
    val title: String,
    val subtitle: String,
    val status: PermissionStatus,
    val icon: ImageVector,
    val onRequest: () -> Unit,
    val buttonText: String = "Grant"
)

@Immutable
private data class UsageItem(
    val icon: ImageVector,
    val label: String
)

@Stable
private val PermissionUsageItems = listOf(
    UsageItem(
        icon = Icons.Outlined.CellTower,
        label = "Cell signal\naccuracy"
    ),
    UsageItem(
        icon = Icons.Outlined.Map,
        label = "Location\ncontext"
    ),
    UsageItem(
        icon = Icons.Outlined.BarChart,
        label = "Network\nanalytics"
    ),
    UsageItem(
        icon = Icons.Outlined.Security,
        label = "Data\nquality"
    )
)

@Preview(showBackground = true)
@Composable
private fun PrivacySettingsContentPreview() {
    MaterialTheme {
        PrivacySettingsContent(
            snapshot = PermissionSnapshot(
                coarseLocationGranted = true,
                fineLocationGranted = false,
                phoneStateGranted = true,
                notificationsGranted = false,
                notificationsRuntimePermissionRequired = true,
                locationServicesOn = false
            ),
            onRefresh = {},
            onOpenLocationSettings = {},
            onRequestCoarseLocation = {},
            onRequestFineLocation = {},
            onRequestPhoneState = {},
            onRequestNotifications = {}
        )
    }
}