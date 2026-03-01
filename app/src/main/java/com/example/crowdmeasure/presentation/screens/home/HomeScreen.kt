package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.AppCard
import com.example.crowdmeasure.presentation.ui.components.BannerTone
import com.example.crowdmeasure.presentation.ui.components.InfoBanner
import com.example.crowdmeasure.presentation.ui.components.MetricRow
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import com.example.crowdmeasure.presentation.ui.components.states.InlineStatusText
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.LocationServicesBanner
import com.example.crowdmeasure.presentation.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen - main dashboard for the app.
 *
 * Features:
 * - Run single measurement on demand
 * - View last measurement preview
 * - Upload queue status and manual trigger
 * - Permission/settings warnings
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        contentPadding = contentPadding,
        state = uiState,
        onStartMeasurement = viewModel::startMeasurement,
        onStopMeasurement = viewModel::stopMeasurement,
        onUploadNow = viewModel::uploadNow,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
private fun HomeContent(
    contentPadding: PaddingValues,
    state: HomeUiState,
    onStartMeasurement: () -> Unit,
    onStopMeasurement: () -> Unit,
    onUploadNow: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val spacing = LocalSpacing.current

    // Scrollable column with proper padding
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)  // Scaffold padding (top bar, bottom bar)
            .windowInsetsPadding(WindowInsets.navigationBars)  // Extra safety for nav bar
            .padding(horizontal = spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        // Top spacing
        Spacer(Modifier.height(spacing.xs))

        // Permission warnings (if any)
        PermissionWarnings(state = state)

        // Quick measurement card
        MeasurementCard(
            state = state,
            onStart = onStartMeasurement,
            onStop = onStopMeasurement
        )

        // Upload queue card
        UploadQueueCard(
            state = state,
            onUpload = onUploadNow
        )

        // Last measurement preview
        LastMeasurementCard(
            state = state,
            onOpenDetail = onNavigateToDetail
        )

        // Bottom spacing (extra for visual breathing room)
        Spacer(Modifier.height(spacing.xl))
    }
}

/**
 * Shows permission/settings warnings if collection is disabled.
 */
@Composable
private fun PermissionWarnings(state: HomeUiState) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        LocationServicesBanner {
            /* Nothing */
        }

        // Critical: No consent or collection disabled
        if (!state.canCollect) {
            InfoBanner(
                title = "Collection Disabled",
                body = "To run measurements, enable consent and data collection in Settings.",
                tone = BannerTone.Warning
            ) {
                IconButton(onClick = { /* TODO: Navigate to settings */ }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open settings"
                    )
                }
            }
        }
        // Warning: Collection enabled but uploads disabled
        else if (!state.uploadsEnabled) {
            InfoBanner(
                title = "Uploads Disabled",
                body = "Measurements will queue locally. Enable uploads in Settings to sync.",
                tone = BannerTone.Info
            )
        }
    }
}

/**
 * Card for running a single measurement.
 */
@Composable
private fun MeasurementCard(
    state: HomeUiState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val spacing = LocalSpacing.current

    AppCard(
        title = "Run Measurement",
        description = when {
            !state.canCollect -> "Enable collection in Settings to start"
            state.isMeasuring -> "Collecting network metrics..."
            else -> "Capture current network performance data"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Button(
                onClick = onStart,
                enabled = state.canCollect && !state.isMeasuring,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(spacing.xs))
                Text(if (state.isMeasuring) "Measuring..." else "Start")
            }

            if (state.isMeasuring) {
                OutlinedButton(
                    onClick = onStop,
                    enabled = state.isMeasuring
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(spacing.xs))
                    Text("Stop")
                }
            }
        }

        InlineStatusText(
            state = state.measurementState,
            successMessage = { "Measurement saved successfully" }
        )
    }
}

/**
 * Card for upload queue and manual upload.
 */
@Composable
private fun UploadQueueCard(
    state: HomeUiState,
    onUpload: () -> Unit
) {
    val spacing = LocalSpacing.current

    AppCard(
        title = "Upload Queue",
        description = when {
            !state.uploadsEnabled -> "Uploads are disabled in Settings"
            state.queuedCount == 0 -> "No measurements waiting to upload"
            state.queuedCount == 1 -> "1 measurement ready to upload"
            else -> "${state.queuedCount} measurements ready to upload"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queued measurements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.queuedCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.queuedCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        FilledTonalButton(
            onClick = onUpload,
            enabled = state.canUpload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null
            )
            Spacer(Modifier.width(spacing.xs))
            Text(
                text = when {
                    state.isUploading -> "Uploading..."
                    !state.uploadsEnabled -> "Uploads disabled"
                    state.queuedCount == 0 -> "Nothing to upload"
                    else -> "Upload now"
                }
            )
        }

        InlineStatusText(
            state = state.uploadState,
            successMessage = { count ->
                when (count) {
                    0 -> "Nothing to upload"
                    1 -> "Uploaded 1 measurement"
                    else -> "Uploaded $count measurements"
                }
            }
        )
    }
}

/**
 * Card showing preview of last measurement.
 */
@Composable
private fun LastMeasurementCard(
    state: HomeUiState,
    onOpenDetail: (String) -> Unit
) {

    AppCard(
        title = "Last Measurement",
        description = if (state.lastMeasurement == null) {
            "No measurements recorded yet"
        } else {
            "Most recent network snapshot"
        }
    ) {
        val measurement = state.lastMeasurement

        if (measurement == null) {
            AppEmptyState(
                title = "No measurements yet",
                description = "Tap 'Start measurement' to begin",
            )
        } else {
            MeasurementPreview(
                measurement = measurement,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

/**
 * Preview content for last measurement.
 */
@Composable
private fun MeasurementPreview(
    measurement: MeasurementUi,
    onOpenDetail: (String) -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // Timestamp
        val timestamp = formatTimestamp(measurement.header.timestampUtcMs)
        MetricRow(
            label = "Recorded",
            value = timestamp
        )

        // Transport type
        MetricRow(
            label = "Transport",
            value = measurement.context.transport.toString()
        )

        // Performance metrics (if available)
        measurement.performance.rttAvgMs?.let { rtt ->
            MetricRow(
                label = "RTT Average",
                value = "$rtt ms"
            )
        }

        measurement.performance.ttfbMs?.let { ttfb ->
            MetricRow(
                label = "Time to First Byte",
                value = "$ttfb ms"
            )
        }

        // Endpoint
        MetricRow(
            label = "Endpoint",
            value = measurement.performance.endpointId
        )

        // Network type indicator
        val networkType = when {
            measurement.cell != null -> "Cellular"
            measurement.wifi != null -> "Wi-Fi"
            else -> "Unknown"
        }
        MetricRow(
            label = "Network Type",
            value = networkType
        )

        // View details button
        TextButton(
            onClick = { onOpenDetail(measurement.header.measurementId) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Full Details")
        }
    }
}
/**
 * Format timestamp for display.
 * Shows date and time in local timezone.
 */
private fun formatTimestamp(timestampMs: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeContent(
        contentPadding = PaddingValues(0.dp),
        state = HomeUiState(
//            consentAccepted = true,
//            collectionEnabled = true,
//            canCollect = true,
//            uploadsEnabled = true,
//            queuedCount = 0,
//            lastMeasurement = null,
//            measurementState = UiState.Idle,
//            uploadState = UiState.Loading
        ),
        onStartMeasurement = {},
        onStopMeasurement = {},
        onUploadNow = {},
        onNavigateToDetail = {}
    )
}