package com.example.crowdmeasure.presentation.screens.consent

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.AppPermissions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.repo.AppSettings


/**
 * Full-screen consent gate that appears on first launch or when consent is needed.
 *
 * Design:
 * - Professional, enterprise-grade UI
 * - Clear hierarchy (what, why, how)
 * - Privacy-first messaging
 * - Progressive disclosure (optional permissions separate)
 * - Non-blocking (user can skip if they want)
 *
 * UX Flow:
 * 1. Show privacy promise and value proposition
 * 2. User accepts consent
 * 3. User enables collection (separate step for clarity)
 * 4. Optional: grant permissions for better data
 * 5. Done button enabled when consent + collection are enabled
 *
 * Performance:
 * - Minimal recomposition (stable state, derived values)
 * - Permission state refreshed on screen visibility
 * - Smooth animations for state changes
 *
 * @param visible Whether the screen should be shown
 * @param onComplete Callback when user completes setup (consent + collection enabled)
 * @param onDismiss Callback when user dismisses without completing
 * @param viewModel ViewModel for consent state and actions
 */
@Composable
fun ConsentGateScreen(
    visible: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ConsentGateViewModel = hiltViewModel<ConsentGateViewModel>(),
) {

    val context = LocalContext.current
    val settings = viewModel.settings.collectAsStateWithLifecycle().value

    val consentAccepted = settings?.consentAccepted ?: false
    val collectionEnabled = settings?.collectionEnabled ?: false

    // Permission state (refreshed when visible changes)
    var fineLocationGranted by remember { mutableStateOf(false) }
    var phoneStateGranted by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            fineLocationGranted = AppPermissions.hasFineLocation(context)
            phoneStateGranted = AppPermissions.hasPhoneState(context)
        }
    }

    val requestFineLocation = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> fineLocationGranted = granted }
    )

    val requestPhoneState = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> phoneStateGranted = granted }
    )

    ConsentGateContent(
        visible = visible,
        onComplete = onComplete,
        onDismiss = onDismiss,
        consentAccepted = consentAccepted,
        collectionEnabled = collectionEnabled,
        onSetConsent = viewModel::setConsent,
        onSetCollection = viewModel::setCollection,
        fineLocationGranted = fineLocationGranted,
        phoneStateGranted = phoneStateGranted,
        onRequestFineLocation = {
            requestFineLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onRequestPhoneState = {
            requestPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
        }
    )
}

@Composable
private fun ConsentGateContent(
    visible: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    consentAccepted: Boolean,
    collectionEnabled: Boolean,
    onSetConsent: (Boolean) -> Unit,
    onSetCollection: (Boolean) -> Unit,
    fineLocationGranted: Boolean,
    phoneStateGranted: Boolean,
    onRequestFineLocation: () -> Unit,
    onRequestPhoneState: () -> Unit
) {
    val spacing = LocalSpacing.current
    val canComplete = consentAccepted && collectionEnabled

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screenPadding)
                        .padding(top = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg)
                ) {
                    ConsentHeader()

                    Spacer(Modifier.height(spacing.xs))

                    PrivacyPromiseCard()

                    ConsentSwitchItem(
                        title = "I understand and agree",
                        subtitle = "Required to participate in crowdsourced measurements",
                        checked = consentAccepted,
                        onCheckedChange = onSetConsent,
                        enabled = true
                    )

                    OptionalPermissionsCard(
                        fineLocationGranted = fineLocationGranted,
                        phoneStateGranted = phoneStateGranted,
                        enabled = consentAccepted,
                        onRequestFineLocation = onRequestFineLocation,
                        onRequestPhoneState = onRequestPhoneState
                    )

                    ConsentSwitchItem(
                        title = "Enable data collection",
                        subtitle = "Start collecting network measurements in the background",
                        checked = collectionEnabled,
                        onCheckedChange = onSetCollection,
                        enabled = consentAccepted
                    )

                    ConsentHints(
                        consentAccepted = consentAccepted,
                        collectionEnabled = collectionEnabled,
                        onAcceptConsent = { onSetConsent(true) },
                        onEnableCollection = { onSetCollection(true) }
                    )

                    Spacer(Modifier.height(spacing.xl))
                }

                ConsentActions(
                    canComplete = canComplete,
                    onComplete = onComplete,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}


@Composable
private fun ConsentHeader() {
    val spacing = LocalSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(spacing.md))

        Text(
            text = "Privacy & Consent",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(spacing.sm))

        Text(
            text = "Help improve telecom quality by contributing anonymous network measurements",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = spacing.lg)
        )
    }
}

@Composable
private fun PrivacyPromiseCard() {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.iconTextGap)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Our Privacy Promise",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            PrivacyBullet("No personal identifiers (name, email, phone number) are collected")
            PrivacyBullet("Network measurements are anonymized before upload")
            PrivacyBullet("You can pause or stop collection anytime")
            PrivacyBullet("Data is used only for improving network quality insights")
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConsentControlsCard(
    consentAccepted: Boolean,
    collectionEnabled: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onCollectionChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            ConsentSwitchItem(
                title = "I understand and agree",
                subtitle = "Required to participate in crowdsourced measurements",
                checked = consentAccepted,
                onCheckedChange = onConsentChange,
                enabled = true
            )

            Divider()

            ConsentSwitchItem(
                title = "Enable data collection",
                subtitle = "Start collecting network measurements in the background",
                checked = collectionEnabled,
                onCheckedChange = onCollectionChange,
                enabled = consentAccepted
            )
        }
    }
}

@Composable
private fun ConsentSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun OptionalPermissionsCard(
//    coarseLocationGranted: Boolean,
    fineLocationGranted: Boolean,
    phoneStateGranted: Boolean,
    enabled: Boolean,
    onRequestFineLocation: () -> Unit,
//    onRequestCoarseLocation: () -> Unit,
    onRequestPhoneState: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "These permissions improve measurement quality. You can grant them now or later in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(spacing.xs))

            PermissionItem(
                icon = Icons.Filled.LocationOn,
                title = "Fine Location",
                subtitle = "Improves accuracy using precise location (GPS)",
                granted = fineLocationGranted,
                enabled = enabled,
                onRequest = onRequestFineLocation
            )

//            PermissionItem(
//                icon = Icons.Filled.LocationOn,
//                title = "Coarse Location",
//                subtitle = "Improves regional accuracy (approximate location, not precise GPS)",
//                granted = coarseLocationGranted,
//                enabled = enabled,
//                onRequest = onRequestCoarseLocation
//            )

            PermissionItem(
                icon = Icons.Filled.PhoneAndroid,
                title = "Phone State",
                subtitle = "Enables cell network metrics for better signal insights",
                granted = phoneStateGranted,
                enabled = enabled,
                onRequest = onRequestPhoneState
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
    enabled: Boolean,
    onRequest: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (granted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!granted) {
            val infinite = rememberInfiniteTransition(label = "grant_attention")

            val pulse by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            val borderWidth by animateDpAsState(
                targetValue = if (enabled) (1.dp + (pulse * 2.dp)) else 1.dp,
                label = "borderWidth"
            )

            val scale by animateFloatAsState(
                targetValue = if (enabled) 1.03f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "scale"
            )

            val borderColor = if (enabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f + 0.35f * pulse)
            } else {
                MaterialTheme.colorScheme.outline
            }

            OutlinedButton(
                onClick = onRequest,
                enabled = enabled,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                border = BorderStroke(borderWidth, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (enabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else
                        Color.Transparent
                ),
            ) {
                Text("Grant")
            }
        }
    }
}


@Composable
private fun ConsentHints(
    consentAccepted: Boolean,
    collectionEnabled: Boolean,
    onAcceptConsent: () -> Unit,
    onEnableCollection: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        when {
            !consentAccepted -> {
                AssistChip(
                    onClick = onAcceptConsent,
                    label = { Text("Tap 'I understand and agree' to continue") }
                )
            }

            !collectionEnabled -> {
                AssistChip(
                    onClick = onEnableCollection,
                    label = { Text("Enable collection to finish setup") }
                )
            }
        }
    }
}

@Composable
private fun ConsentActions(
    canComplete: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip for now")
            }

            Button(
                onClick = onComplete,
                enabled = canComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Started")
            }
        }
    }
}


@Preview
@Composable
private fun ConsentGateScreenPreview() {
    ConsentGateContent(
        visible = true,
        onComplete = {},
        onDismiss = {},
        consentAccepted = false,
        collectionEnabled = false,
        onSetConsent = {},
        onSetCollection = {},
        fineLocationGranted = false,
        phoneStateGranted = false,
        onRequestFineLocation = {},
        onRequestPhoneState = {}
    )
}


//@Preview
//@Composable
//private fun OptionalPermissionsCardPreview() {
//    OptionalPermissionsCard(
//        fineLocationGranted = false,
//        phoneStateGranted = false,
//        enabled = true,
//        onRequestFineLocation = {},
//        onRequestPhoneState = {}
//    )
//}
