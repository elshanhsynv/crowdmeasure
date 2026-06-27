package com.example.crowdmeasure.presentation.screens.callsampling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun CallSessionsScreen(
    contentPadding: PaddingValues,
    viewModel: CallSessionsViewModel = hiltViewModel<CallSessionsViewModel>()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val samples by viewModel.samples.collectAsStateWithLifecycle()

    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSampleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SessionSort.Latest) }
    var networkFilter by rememberSaveable { mutableStateOf(NetworkFilter.All) }

    val visibleSessions = remember(sessions, query, sort, networkFilter) {
        sessions
            .filter { session -> session.matches(query, networkFilter) }
            .let { list ->
                when (sort) {
                    SessionSort.Latest -> list.sortedByDescending(CallSession::startedAtUtcMs)
                    SessionSort.Oldest -> list.sortedBy(CallSession::startedAtUtcMs)
                }
            }
    }

    LaunchedEffect(visibleSessions, selectedSessionId) {
        val nextSelected = visibleSessions.firstOrNull()?.sessionId
        if (selectedSessionId == null || visibleSessions.none { it.sessionId == selectedSessionId }) {
            selectedSessionId = nextSelected
            expandedSessionId = nextSelected
            selectedSampleId = null
            nextSelected?.let(viewModel::selectSession)
        }
    }

    LaunchedEffect(samples) {
        selectedSampleId = samples.firstOrNull { it.id == selectedSampleId }?.id
            ?: samples.firstOrNull()?.id
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sessions.isEmpty()) {
            item(key = "empty") {
                AppEmptyState(
                    modifier = Modifier.fillParentMaxHeight(),
                    title = "No call sessions",
                    description = "Sessions appear here after a sampled call.",
                    icon = Icons.Outlined.PhoneAndroid
                )
            }
        } else {
            item(key = "summary") {
                CallSessionsSummary(sessions = sessions)
            }

            item(key = "search") {
                SearchAndFilters(
                    query = query,
                    onQueryChange = { query = it },
                    sort = sort,
                    onSortChange = { sort = it },
                    networkFilter = networkFilter,
                    onNetworkFilterChange = { networkFilter = it },
                    visibleCount = visibleSessions.size,
                    totalCount = sessions.size,
                    onClear = { showClearDialog = true }
                )
            }

            if (visibleSessions.isEmpty()) {
                item(key = "no_matches") {
                    AppEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        title = "No matching sessions",
                        description = "Try a different date, network, or cell suffix.",
                        icon = Icons.Filled.Search
                    )
                }
            } else {
                items(visibleSessions, key = { it.sessionId }) { session ->
                    val expanded = session.sessionId == expandedSessionId
                    SessionCard(
                        session = session,
                        expanded = expanded,
                        samples = if (expanded && selectedSessionId == session.sessionId) samples else emptyList(),
                        selectedSampleId = selectedSampleId,
                        onSelectSample = { selectedSampleId = it },
                        onClick = {
                            selectedSessionId = session.sessionId
                            expandedSessionId = if (expanded) null else session.sessionId
                            selectedSampleId = null
                            viewModel.selectSession(session.sessionId)
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
            title = { Text("Clear call data?") },
            text = { Text("This permanently deletes all local call sessions and samples.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearData()
                        selectedSessionId = null
                        expandedSessionId = null
                        selectedSampleId = null
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CallSessionsSummary(sessions: List<CallSession>) {
    val latest = sessions.maxByOrNull { it.startedAtUtcMs }
    val averageRsrp = sessions
        .mapNotNull { it.latestSample?.rsrpDbm }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryMetric(
                label = "Total Calls",
                value = sessions.size.toString(),
                helper = "All time",
                modifier = Modifier.weight(0.8f)
            )

            VerticalRule()

            SummaryMetric(
                label = "Latest Call",
                value = latest?.let { formatTime(it.startedAtUtcMs) } ?: "No calls",
                helper = latest?.latestSample?.rat.orEmpty().ifBlank { durationLabel(latest) },
                modifier = Modifier.weight(1.5f)
            )

            VerticalRule()

            SummaryMetric(
                label = "Avg Signal (RSRP)",
                value = averageRsrp?.let { "$it dBm" } ?: "-",
                helper = "Recent sessions",
                valueColor = signalColor(averageRsrp),
                modifier = Modifier.weight(1f)
            )

            SignalIcon()
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VerticalRule() {
    Box(
        modifier = Modifier
            .height(64.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun SignalIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SignalCellularAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    sort: SessionSort,
    onSortChange: (SessionSort) -> Unit,
    networkFilter: NetworkFilter,
    onNetworkFilterChange: (NetworkFilter) -> Unit,
    visibleCount: Int,
    totalCount: Int,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.extraLarge,
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = {
                Text(
                    text = "Search by date, network, cell suffix...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = sort == SessionSort.Latest,
                onClick = { onSortChange(SessionSort.Latest) },
                leadingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Latest") }
            )
            FilterChip(
                selected = sort == SessionSort.Oldest,
                onClick = { onSortChange(SessionSort.Oldest) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Oldest") }
            )
            NetworkFilter.entries.forEach { filter ->
                FilterChip(
                    selected = networkFilter == filter,
                    onClick = { onNetworkFilterChange(filter) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filter == NetworkFilter.All) Icons.Filled.FilterList else Icons.Filled.SignalCellularAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(filter.label) }
                )
            }
            TextButton(onClick = onClear) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jump to latest",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (visibleCount == totalCount) "$totalCount calls" else "$visibleCount of $totalCount calls",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: CallSession,
    expanded: Boolean,
    samples: List<CallCellSample>,
    selectedSampleId: Long?,
    onSelectSample: (Long) -> Unit,
    onClick: () -> Unit
) {
    val latest = session.latestSample

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (expanded) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SessionHeader(session = session, expanded = expanded)

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

                    if (samples.isEmpty()) {
                        Text(
                            text = "No samples captured for this session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val selectedSample = samples.firstOrNull { it.id == selectedSampleId } ?: samples.first()

                        Text(
                            text = "Samples",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        SampleTimeline(
                            samples = samples,
                            selectedSampleId = selectedSample.id,
                            onSelectSample = onSelectSample
                        )

                        SampleDetails(sample = selectedSample)
                    }
                }
            }

            if (!expanded && latest == null) {
                Text(
                    text = "No signal sample yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionHeader(session: CallSession, expanded: Boolean) {
    val latest = session.latestSample

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SignalCellularAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTime(session.startedAtUtcMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                NetworkBadge(text = latest?.rat ?: session.callSource.name.readableEnum())
            }

            Text(
                text = "${durationLabel(session)}  -  ${session.sampleCount} ${"sample".pluralize(session.sampleCount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        CompactMetric(
            label = "RSRP",
            value = latest?.rsrpDbm?.toString() ?: "-",
            color = signalColor(latest?.rsrpDbm)
        )
        CompactMetric(
            label = "SINR",
            value = latest?.sinrDb?.toString() ?: "-",
            color = sinrColor(latest?.sinrDb)
        )

        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse session" else "Expand session",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun NetworkBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactMetric(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.widthIn(min = 46.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun SampleTimeline(
    samples: List<CallCellSample>,
    selectedSampleId: Long,
    onSelectSample: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        samples.forEach { sample ->
            FilterChip(
                selected = sample.id == selectedSampleId,
                onClick = { onSelectSample(sample.id) },
                label = { Text("${sample.elapsedMs / 1_000L}s") },
                modifier = Modifier.defaultMinSize(minWidth = 72.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun SampleDetails(sample: CallCellSample) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricSection(
            title = "Signal",
            icon = Icons.Filled.SignalCellularAlt,
            metrics = listOf(
                Metric("RSSI", sample.cell.serving?.rssiDbm?.let { "$it dBm" }),
                Metric("RSRP", sample.rsrpDbm?.let { "$it dBm" }, signalColor(sample.rsrpDbm)),
                Metric("RSRQ", sample.rsrqDb?.let { "$it dB" }),
                Metric("SINR", sample.sinrDb?.let { "$it dB" }, sinrColor(sample.sinrDb)),
                Metric("RSCP", sample.dbm?.let { "$it dBm" })
            )
        )

        MetricSection(
            title = "Cell",
            icon = Icons.Filled.CellTower,
            metrics = listOf(
                Metric("Cell ID", sample.cell.serving?.cellId?.maskedId()),
                Metric("PCI", sample.pci?.toString()),
                Metric("TAC", sample.tac?.maskedId()),
                Metric("Band", sample.band?.toString()),
                Metric("EARFCN", sample.cell.serving?.arfcn?.toString()),
                Metric("NR", sample.nrState),
                Metric("Neighbors", sample.cell.neighbors.size.toString())
            )
        )

        sample.location?.let { location ->
            MetricSection(
                title = "Location",
                icon = Icons.Filled.FilterList,
                metrics = listOf(
                    Metric("Latitude", location.lat.approxCoordinate()),
                    Metric("Longitude", location.lon.approxCoordinate()),
                    Metric("Accuracy", "${location.accuracyMeters.toInt()} m")
                )
            )
        }

        sample.dataUsage?.let { usage ->
            MetricSection(
                title = "Data",
                icon = Icons.AutoMirrored.Filled.Sort,
                metrics = listOf(
                    Metric("Down", usage.dlMB.oneDecimal("MB")),
                    Metric("Up", usage.ulMB.oneDecimal("MB")),
                    Metric("Down rate", usage.dlKbps.oneDecimal("Kbps")),
                    Metric("Up rate", usage.ulKbps.oneDecimal("Kbps"))
                )
            )
        }
    }
}

@Composable
private fun MetricSection(
    title: String,
    icon: ImageVector,
    metrics: List<Metric>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                metrics.forEach { metric ->
                    MetricTile(metric = metric)
                }
            }
        }
    }
}

@Composable
private fun MetricTile(metric: Metric) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .widthIn(min = 104.dp)
            .defaultMinSize(minHeight = 64.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = metric.color ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class SessionSort {
    Latest,
    Oldest
}

private enum class NetworkFilter(val label: String) {
    All("All networks"),
    Lte("LTE"),
    Nr("NR")
}

private data class Metric(
    val label: String,
    val value: String?,
    val color: Color? = null
)

private val timeFormatter = DateTimeFormatter
    .ofPattern("MMM dd, yyyy HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatTime(timestampUtcMs: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(timestampUtcMs))

private fun durationLabel(session: CallSession?): String {
    if (session == null) return "-"
    val ended = session.endedAtUtcMs ?: System.currentTimeMillis()
    val seconds = max(0L, (ended - session.startedAtUtcMs) / 1_000L)
    return when {
        seconds < 60 -> "${seconds}s"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}

private fun CallSession.matches(query: String, networkFilter: NetworkFilter): Boolean {
    val rat = latestSample?.rat.orEmpty()
    val networkMatches = when (networkFilter) {
        NetworkFilter.All -> true
        NetworkFilter.Lte -> rat.equals("LTE", ignoreCase = true)
        NetworkFilter.Nr -> rat.equals("NR", ignoreCase = true)
    }
    if (!networkMatches) return false

    val cleanedQuery = query.trim().lowercase()
    if (cleanedQuery.isBlank()) return true

    val searchable = listOfNotNull(
        formatTime(startedAtUtcMs),
        rat,
        latestSample?.nrState,
        callSource.name.readableEnum(),
        callType.name.readableEnum(),
        latestSample?.cell?.serving?.cellId?.maskedId(),
        latestSample?.cell?.serving?.cellId?.lastDigits()
    ).joinToString(" ").lowercase()

    return searchable.contains(cleanedQuery)
}

private fun String.readableEnum(): String =
    lowercase()
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun String.pluralize(count: Int): String = if (count == 1) this else "${this}s"

private fun Int.maskedId(): String = toLong().maskedId()

private fun Long.maskedId(): String = "..." + lastDigits()

private fun Int.lastDigits(): String = toLong().lastDigits()

private fun Long.lastDigits(): String = kotlin.math.abs(this).toString().takeLast(4)

private fun Double.approxCoordinate(): String = String.format(Locale.US, "%.4f", this)

private fun Double.oneDecimal(unit: String): String = String.format(Locale.US, "%.1f %s", this, unit)

@Composable
private fun signalColor(value: Int?): Color = when {
    value == null -> MaterialTheme.colorScheme.onSurface
    value >= -95 -> MaterialTheme.colorScheme.primary
    value >= -110 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun sinrColor(value: Int?): Color = when {
    value == null -> MaterialTheme.colorScheme.onSurface
    value >= 10 -> MaterialTheme.colorScheme.primary
    value >= 0 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
