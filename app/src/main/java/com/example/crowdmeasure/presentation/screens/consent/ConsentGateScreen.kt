package com.example.crowdmeasure.presentation.screens.consent

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.theme.ExtendedColors
import com.example.crowdmeasure.presentation.util.AppPermissions


@Composable
fun ConsentGateScreen(
    visible: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ConsentGateViewModel = hiltViewModel<ConsentGateViewModel>()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var fineLocationGranted by remember { mutableStateOf(false) }
    var phoneStateGranted by remember { mutableStateOf(false) }
    var backgroundLocationGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        fineLocationGranted = AppPermissions.hasFineLocation(context)
        phoneStateGranted = AppPermissions.hasPhoneState(context)
        backgroundLocationGranted = AppPermissions.hasBackgroundLocation(context)
        notificationsGranted = AppPermissions.hasPostNotifications(context)
    }

    LaunchedEffect(visible) { if (visible) refreshPermissions() }

    val requestFine =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }
    val requestBackground =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }
    val requestPhone =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }
    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

    val canCollect = settings != null &&
            settings?.consentGateDismissed == false &&
            fineLocationGranted &&
            phoneStateGranted &&
            backgroundLocationGranted

    ConsentGateContent(
        visible = visible,
        onComplete = onComplete,
        fineLocationGranted = fineLocationGranted,
        backgroundLocationGranted = backgroundLocationGranted,
        phoneStateGranted = phoneStateGranted,
        notificationsGranted = notificationsGranted,
        canComplete = canCollect,
        onRequestFineLocation = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
        onRequestBackgroundLocation = { requestBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
        onRequestPhoneState = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) },
        onRequestNotifications = { requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
    )
}


@Composable
private fun ConsentGateContent(
    visible: Boolean,
    onComplete: () -> Unit,
    fineLocationGranted: Boolean,
    backgroundLocationGranted: Boolean,
    phoneStateGranted: Boolean,
    notificationsGranted: Boolean,
    canComplete: Boolean,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Scrollable body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 40.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ConsentHero()

                    Spacer(Modifier.height(8.dp))

                    PrivacyPromiseCard()

                    PermissionsCard(
                        fineLocationGranted = fineLocationGranted,
                        backgroundLocationGranted = backgroundLocationGranted,
                        phoneStateGranted = phoneStateGranted,
                        onRequestFineLocation = onRequestFineLocation,
                        onRequestBackgroundLocation = onRequestBackgroundLocation,
                        onRequestPhoneState = onRequestPhoneState,
                        notificationsGranted = notificationsGranted,
                        onRequestNotifications = onRequestNotifications,
                    )
                }

                // Sticky bottom CTA
                GetStartedBar(canComplete = canComplete, onComplete = onComplete)
            }
        }
    }
}


@Composable
private fun ConsentHero() {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                listOf(48.dp, 56.dp, 64.dp).forEach { r ->
                    drawCircle(
                        color = primary.copy(alpha = 0.12f),
                        radius = r.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Privacy & Consent",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Help improve telecom quality by contributing anonymous network measurements",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


private val privacyBullets = listOf(
    "No personal identifiers (name, email, phone number) are collected",
    "Network measurements are anonymized before upload",
    "You can pause or stop collection anytime",
    "Data is used only for improving network quality insights"
)

@Composable
private fun PrivacyPromiseCard() {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = onContainer.copy(alpha = 0.10f),
                    modifier = Modifier.size(72.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = onContainer.copy(alpha = 0.10f),
                    modifier = Modifier.size(56.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = onContainer.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = onContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Our Privacy Promise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainer
                    )
                }

                privacyBullets.forEachIndexed { index, bullet ->
                    PrivacyBullet(text = bullet)
                    if (index < privacyBullets.lastIndex) {
                        HorizontalDivider(
                            color = onContainer.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 1.dp),
            tint = onContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = onContainer,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun PermissionsCard(
    fineLocationGranted: Boolean,
    backgroundLocationGranted: Boolean,
    phoneStateGranted: Boolean,
    notificationsGranted: Boolean,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "These permissions improve measurement quality.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PermissionItem(
                icon = Icons.Outlined.PinDrop,
                title = "Fine Location",
                subtitle = "Improves accuracy using precise location (GPS)",
                granted = fineLocationGranted,
                onRequest = onRequestFineLocation
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PermissionItem(
                icon = Icons.Outlined.Wifi,
                title = "Background Location",
                subtitle = "Improves accuracy using approximate location",
                granted = backgroundLocationGranted,
                onRequest = onRequestBackgroundLocation
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PermissionItem(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Phone State",
                subtitle = "Enables cell network metrics for better signal insights",
                granted = phoneStateGranted,
                onRequest = onRequestPhoneState
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PermissionItem(
                icon = Icons.Outlined.Notifications,
                title = "Notification",
                subtitle = "Required for the foreground service notification",
                granted = notificationsGranted,
                onRequest = onRequestNotifications
            )
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (granted) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = ExtendedColors.successDark
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onRequest,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Grant")
            }
        }
    }
}


@Composable
private fun GetStartedBar(canComplete: Boolean, onComplete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onComplete,
            enabled = canComplete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ConsentGatePreview() {
    ConsentGateContent(
        visible = true,
        onComplete = {},
        fineLocationGranted = false,
        backgroundLocationGranted = false,
        phoneStateGranted = true,
        notificationsGranted = true,
        canComplete = false,
        onRequestFineLocation = {},
        onRequestBackgroundLocation = {},
        onRequestPhoneState = {},
        onRequestNotifications = {}
    )
}