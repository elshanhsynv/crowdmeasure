package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.AppCard
import com.example.crowdmeasure.presentation.ui.components.BannerTone
import com.example.crowdmeasure.presentation.ui.components.InfoBanner
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import com.example.crowdmeasure.presentation.ui.components.states.InlineStatusText
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.LocationServicesBanner
import com.example.crowdmeasure.presentation.util.SystemSettingsIntents
import com.example.crowdmeasure.presentation.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        PermissionWarnings(state = state)
        MeasurementCard(
            state = state,
            onStart = onStartMeasurement,
            onStop = onStopMeasurement
        )
        UploadQueueCard(state = state, onUpload = onUploadNow)
        LastMeasurementCard(state = state, onOpenDetail = onNavigateToDetail)
        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun PermissionWarnings(state: HomeUiState) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        LocationServicesBanner(
            locationServicesOn = state.locationServicesOn,
            onClick = { SystemSettingsIntents.openLocationSettings(context) }
        )

        if (!state.canCollect) {
            InfoBanner(
                title = "Collection disabled",
                body = "Enable consent and data collection in Settings to run measurements.",
                tone = BannerTone.Warning
            ) {
                IconButton(onClick = { /* Settings navigation belongs to app chrome. */ }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open settings"
                    )
                }
            }
        } else if (!state.uploadsEnabled) {
            InfoBanner(
                title = "Uploads paused",
                body = "Measurements will stay on this device until uploads are enabled.",
                tone = BannerTone.Info
            )
        }
    }
}

@Composable
fun MeasurementCard(
    state: HomeUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val description = when {
        !state.canCollect -> "Enable collection in Settings to start."
        state.isMeasuring -> "Collecting a fresh network snapshot."
        else -> "Capture current network performance data."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Run Measurement",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }

            AnimatedContent(
                targetState = state.isMeasuring,
                label = "measurement_button",
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                }
            ) { measuring ->
                if (measuring) {
                    FilledTonalButton(
                        onClick = onStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Measurement")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = state.canCollect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Measurement")
                    }
                }
            }

            InlineStatusText(
                state = state.measurementState,
                successMessage = { "Measurement saved" }
            )
        }
    }
}

@Composable
private fun UploadQueueCard(
    state: HomeUiState,
    onUpload: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasItems = state.queuedCount > 0
    val showButton = hasItems || state.uploadState is UiState.Error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Upload Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = uploadDescription(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = state.queuedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasItems) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "queued",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showButton) {
                FilledTonalButton(
                    onClick = onUpload,
                    enabled = state.canUpload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    AnimatedContent(
                        targetState = state.isUploading,
                        label = "upload_button",
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        }
                    ) { uploading ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalContentColor.current
                                )
                            } else {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            }
                            Text(
                                text = if (state.uploadState is UiState.Error) "Retry Upload" else "Upload Now"
                            )
                        }
                    }
                }
            }

            InlineStatusText(
                state = state.uploadState,
                successMessage = { count ->
                    when (count) {
                        1 -> "Uploaded 1 measurement"
                        else -> "Uploaded $count measurements"
                    }
                }
            )
        }
    }
}

@Composable
private fun LastMeasurementCard(
    state: HomeUiState,
    onOpenDetail: (String) -> Unit
) {
    val measurement = state.lastMeasurement

    AppCard(
        title = "Last Measurement",
        description = if (measurement == null) {
            "No measurements recorded yet"
        } else {
            "Most recent network snapshot"
        },
        headerTrailing = if (measurement == null) {
            null
        } else {
            {
                TextButton(onClick = { onOpenDetail(measurement.meta.measurementId) }) {
                    Text("View Details")
                }
            }
        }
    ) {
        if (measurement == null) {
            AppEmptyState(
                title = "Start a measurement to see a summary here.",
//                description = "Start a measurement to see a private summary here."
            )
        } else {
            MeasurementPreview(measurement = measurement)
        }
    }
}

@Composable
private fun MeasurementPreview(measurement: MeasurementUi) {
    val spacing = LocalSpacing.current

    val metrics = listOf(
        MetricTileData(
            label = "Recorded",
            value = formatTimestamp(measurement.meta.timestampUtcMs),
            icon = Icons.Filled.AccessTime
        ),
        MetricTileData(
            label = "Transport",
            value = measurement.environment.network.transport.toString(),
            icon = Icons.Filled.Timeline
        ),
        MetricTileData(
            label = "HTTP Lat",
            value = measurement.performance.httpLatencyAvgMs?.let { "$it ms" } ?: "-",
            icon = Icons.Filled.Speed
        ),
        MetricTileData(
            label = "TTFB Avg",
            value = measurement.performance.ttfbAvgMs?.let { "$it ms" } ?: "-",
            icon = Icons.Filled.Speed
        ),
        MetricTileData(
            label = "Jitter",
            value = measurement.performance.jitterMs?.let { "$it ms" } ?: "-",
            icon = Icons.Filled.SignalCellularAlt
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        MetricTile(
            data = metrics.first(),
            modifier = Modifier.fillMaxWidth()
        )
        metrics.drop(1).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                row.forEach { metric ->
                    MetricTile(
                        data = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    data: MetricTileData,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier.heightIn(min = 82.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IconBadge(
                icon = data.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = data.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private data class MetricTileData(
    val label: String,
    val value: String,
    val icon: ImageVector
)

private fun uploadDescription(state: HomeUiState): String {
    return when {
        !state.uploadsEnabled -> "Uploads are disabled in Settings"
        state.queuedCount == 0 -> "No measurements waiting to upload"
        state.queuedCount == 1 -> "1 measurement ready to sync"
        else -> "${state.queuedCount} measurements ready to sync"
    }
}

private fun formatTimestamp(timestampMs: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeContent(
        contentPadding = PaddingValues(0.dp),
        state = HomeUiState(canCollect = true),
        onStartMeasurement = {},
        onStopMeasurement = {},
        onUploadNow = {},
        onNavigateToDetail = {}
    )
}
