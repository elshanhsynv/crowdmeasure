package com.example.crowdmeasure.presentation.screens.callsampling

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.blue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.presentation.screens.callsampling.Metric
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSessionsScreen(
    contentPadding: PaddingValues,
    viewModel: CallSessionsViewModel = hiltViewModel<CallSessionsViewModel>()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val samples by viewModel.samples.collectAsStateWithLifecycle()

    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSampleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSamplesSheet by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SessionSort.Latest) }
    var networkFilter by rememberSaveable { mutableStateOf(NetworkFilter.All) }

    val visibleSessions = remember(sessions, query, sort, networkFilter) {
        sessions
            .filter { it.matches(query, networkFilter) }
            .let { list ->
                when (sort) {
                    SessionSort.Latest -> list.sortedByDescending(CallSession::startedAtUtcMs)
                    SessionSort.Oldest -> list.sortedBy(CallSession::startedAtUtcMs)
                }
            }
    }
    val selectedSession = remember(sessions, selectedSessionId) {
        sessions.firstOrNull { it.sessionId == selectedSessionId }
    }
    val selectedSessionIndex = remember(visibleSessions, selectedSessionId) {
        visibleSessions.indexOfFirst { it.sessionId == selectedSessionId }
            .takeIf { it >= 0 }
            ?.plus(1)
            ?: 1
    }

    LaunchedEffect(samples) {
        selectedSampleId = samples.firstOrNull { it.id == selectedSampleId }?.id
            ?: samples.firstOrNull()?.id
    }

    LaunchedEffect(selectedSession, showSamplesSheet) {
        if (showSamplesSheet && selectedSession == null) showSamplesSheet = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            item(key = "search") {
                CompactSearch(
                    query = query,
                    onQueryChange = { query = it },
                    showFilters = showFilters,
                    onToggleFilters = { showFilters = !showFilters },
                    onClear = { showClearDialog = true }
                )
            }

            if (showFilters) {
                item(key = "filters") {
                    FilterStrip(
                        sort = sort,
                        onSortChange = { sort = it },
                        networkFilter = networkFilter,
                        onNetworkFilterChange = { networkFilter = it }
                    )
                }
            }

            item(key = "count_sort") {
                CountSortRow(
                    visibleCount = visibleSessions.size,
                    totalCount = sessions.size,
                    sort = sort,
                    onSortChange = {
                        sort = when (sort) {
                            SessionSort.Latest -> SessionSort.Oldest
                            SessionSort.Oldest -> SessionSort.Latest
                        }
                    }
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
                itemsIndexed(
                    items = visibleSessions,
                    key = { _, session -> session.sessionId }
                ) { index, session ->
                    SessionCard(
                        session = session,
                        index = index + 1,
                        selected = session.sessionId == selectedSessionId && showSamplesSheet,
                        onClick = {
                            selectedSessionId = session.sessionId
                            selectedSampleId = null
                            viewModel.selectSession(session.sessionId)
                            showSamplesSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showSamplesSheet && selectedSession != null) {
        ModalBottomSheet(
            onDismissRequest = { showSamplesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = { WindowInsets(0.dp) },
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .size(width = 48.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                )
            }
        ) {
            SamplesSheet(
                session = selectedSession,
                index = selectedSessionIndex,
                samples = samples,
                selectedSampleId = selectedSampleId,
                onSelectSample = { selectedSampleId = it },
                onClose = { showSamplesSheet = false }
            )
        }
    }
}

@Composable
fun CallSessionsTopBarActions(
    onClear: () -> Unit
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { showClearDialog = true }) {
        Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Clear call data",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                        onClear()
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
private fun CompactSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            placeholder = {
                Text(
                    text = "Search sessions...",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

//        IconButton(onClick = onToggleFilters) {
//            Icon(
//                imageVector = Icons.Filled.FilterList,
//                contentDescription = if (showFilters) "Hide filters" else "Show filters",
//                tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }

//        IconButton(onClick = onClear) {
//            Icon(
//                imageVector = Icons.Filled.DeleteOutline,
//                contentDescription = "Clear call data",
//                tint = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
    }
}

@Composable
private fun CountSortRow(
    visibleCount: Int,
    totalCount: Int,
    sort: SessionSort,
    onSortChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.SignalCellularAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (visibleCount == totalCount) "$totalCount Sessions" else "$visibleCount of $totalCount Sessions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSortChange) {
            Text(
                text = if (sort == SessionSort.Latest) "Latest first" else "Oldest first",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FilterStrip(
    sort: SessionSort,
    onSortChange: (SessionSort) -> Unit,
    networkFilter: NetworkFilter,
    onNetworkFilterChange: (NetworkFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = sort == SessionSort.Latest,
            onClick = { onSortChange(SessionSort.Latest) },
            label = { Text("Latest") }
        )
        FilterChip(
            selected = sort == SessionSort.Oldest,
            onClick = { onSortChange(SessionSort.Oldest) },
            label = { Text("Oldest") }
        )
        NetworkFilter.entries.forEach { filter ->
            FilterChip(
                selected = networkFilter == filter,
                onClick = { onNetworkFilterChange(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: CallSession,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val latest = session.latestSample

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumberBadge(index = index, selected = selected)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = formatTime(session.startedAtUtcMs),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sessionSubtitle(session),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DurationPill(durationLabel(session))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

//            if (selected || latest != null) {
//                MetricChips(
//                    sample = latest,
//                    compact = true
//                )
//            }
        }
    }
}

@Composable
private fun NumberBadge(index: Int, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DurationPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricChips(
    sample: CallCellSample?,
    compact: Boolean
) {
    val serving = sample?.cell?.serving
    val metrics = listOf(
        Metric("RSSI", serving?.rssiDbm?.toString(), Icons.Filled.SignalCellularAlt),
        Metric(
            "RSRP",
            sample?.rsrpDbm?.toString(),
            Icons.Filled.SignalCellularAlt,
            signalColor(sample?.rsrpDbm)
        ),
        Metric("RSRQ", sample?.rsrqDb?.toString(), Icons.Filled.CellTower),
        Metric(
            "SINR",
            sample?.sinrDb?.toString(),
            Icons.Filled.SignalCellularAlt,
            sinrColor(sample?.sinrDb)
        ),
//        Metric("Band", sample?.band?.let { "Band $it" }, Icons.Filled.CellTower)
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        metrics.forEach { metric ->
            MetricChip(metric = metric, compact = compact)
        }
    }
}

@Composable
private fun MetricChip(
    metric: Metric,
    compact: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.defaultMinSize(
            minWidth = if (compact) 70.dp else 94.dp,
            minHeight = if (compact) 38.dp else 62.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 7.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 17.dp else 18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (!compact) {
                    Text(
                        text = metric.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = metric.value ?: "-",
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = metric.color ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SamplesSheet(
    session: CallSession,
    index: Int,
    samples: List<CallCellSample>,
    selectedSampleId: Long?,
    onSelectSample: (Long) -> Unit,
    onClose: () -> Unit
) {
    val selectedSample = samples.firstOrNull { it.id == selectedSampleId } ?: samples.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "sheet_header") {
            SheetHeader(
                session = session,
                index = index,
                onClose = onClose
            )
        }

//        item(key = "sample_count") {
//            Text(
//                text = "${samples.size} ${"sample".pluralize(samples.size)}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }

        item(key = "divider") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        }

        if (samples.isEmpty()) {
            item(key = "no_samples") {
                Text(
                    text = "No samples captured for this session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item(key = "sample_times") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sample times: ${samples.size} ${"sample".pluralize(samples.size)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    SampleTimeline(
                        samples = samples,
                        selectedSampleId = selectedSample?.id ?: -1L,
                        onSelectSample = onSelectSample
                    )
                }
            }

            selectedSample?.let { sample ->
                item(key = "cell") {
                    SectionHeader("Cell", Icons.Filled.CellTower)
                    Spacer(Modifier.height(8.dp))
                    MetricGrid(
                        metrics = listOf(
                            SheetMetric("Cell ID", sample.cell.serving?.cellId.toString()),
                            SheetMetric("PCI", sample.pci?.toString()),
                            SheetMetric("TAC", sample.tac?.toString()),
                            SheetMetric("Band", sample.band?.toString()),
                            SheetMetric("NR", sample.nrState),
                            SheetMetric("Neighbors", sample.cell.neighbors.size.toString()),
                            SheetMetric("RSSI", sample.cell.serving?.rssiDbm?.toString()),
                            SheetMetric("RSRP", sample.cell.serving?.rsrpDbm?.toString()),
                            SheetMetric("RSRQ", sample.cell.serving?.rsrqDb?.toString()),
                            SheetMetric("SINR", sample.cell.serving?.sinrDb?.toString()),
                            SheetMetric("Band", sample.cell.serving?.band?.let { "Band $it" })
                        )
                    )
                }

                sample.location?.let { location ->
                    item(key = "location") {
                        SectionHeader("Location", Icons.Filled.LocationOn)
                        Spacer(Modifier.height(8.dp))
                        MetricGrid(
                            metrics = listOf(
                                SheetMetric("Latitude", location.lat.approxCoordinate()),
                                SheetMetric("Longitude", location.lon.approxCoordinate()),
                                SheetMetric("Accuracy", "${location.accuracyMeters.toInt()} m")
                            )
                        )
                    }
                }

                sample.dataUsage?.let { usage ->
                    item(key = "dataUsageInfo") {
                        SectionHeader("Data Usage", Icons.Filled.DataUsage)
                        Spacer(Modifier.height(8.dp))
                        MetricGrid(
                            metrics = listOf(
                                SheetMetric("Down", usage.dlMB.oneDecimal("MB")),
                                SheetMetric("Up", usage.ulMB.oneDecimal("MB")),
                                SheetMetric("Down rate", usage.dlKbps.oneDecimal("Kbps")),
                                SheetMetric("Up rate", usage.ulKbps.oneDecimal("Kbps"))
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    session: CallSession,
    index: Int,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NumberBadge(index = index, selected = true)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatTime(session.startedAtUtcMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sessionSubtitle(session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DurationPill(durationLabel(session))
        IconButton(onClick = onClose, modifier = Modifier) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close samples"
            )
        }
    }
}

@Composable
private fun SampleTimeline(
    samples: List<CallCellSample>,
    selectedSampleId: Long,
    onSelectSample: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val fadeWidth = 28.dp

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()

                val fadeWidthPx = fadeWidth.toPx()

                val viewportEnd = listState.layoutInfo.viewportEndOffset
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()

                val showLeftFade =
                    listState.firstVisibleItemIndex > 0 ||
                            listState.firstVisibleItemScrollOffset > 0

                val showRightFade = lastVisibleItem != null &&
                        (lastVisibleItem.index < listState.layoutInfo.totalItemsCount - 1 ||
                                lastVisibleItem.offset + lastVisibleItem.size > viewportEnd)

                if (showLeftFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startX = 0f,
                            endX = fadeWidthPx
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }

                if (showRightFade) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startX = size.width - fadeWidthPx,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - fadeWidthPx, 0f),
                        blendMode = BlendMode.DstIn
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = samples,
            key = { it.id }
        ) { sample ->
            FilterChip(
                selected = sample.id == selectedSampleId,
                onClick = { onSelectSample(sample.id) },
                label = { Text("${sample.elapsedMs / 1_000L}s") },
                modifier = Modifier.defaultMinSize(minWidth = 52.dp, minHeight = 34.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetricGrid(metrics: List<SheetMetric>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { metric ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .widthIn(min = 110.dp)
                    .defaultMinSize(minHeight = 50.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private enum class SessionSort {
    Latest,
    Oldest
}

private enum class NetworkFilter(val label: String) {
    All("All"),
    Lte("LTE"),
    Nr("NR")
}

private data class Metric(
    val label: String,
    val value: String?,
    val icon: ImageVector,
    val color: Color? = null
)

private data class SheetMetric(
    val label: String,
    val value: String?
)

private val timeFormatter = DateTimeFormatter
    .ofPattern("MMM dd yyyy HH:mm:ss")
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

private fun sessionSubtitle(session: CallSession): String =
    listOf(
        session.latestSample?.rat ?: "Unknown",
        session.callSource.name.readableEnum(),
        session.callType.name.readableEnum()
    ).joinToString("  -  ")

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

private fun Double.oneDecimal(unit: String): String =
    String.format(Locale.US, "%.1f %s", this, unit)

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
