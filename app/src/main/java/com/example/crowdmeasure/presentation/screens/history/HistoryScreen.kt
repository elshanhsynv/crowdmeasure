package com.example.crowdmeasure.presentation.screens.history

import com.example.crowdmeasure.presentation.ui.components.input.AppSearchBar
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel>()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        contentPadding = contentPadding,
        state = uiState,
        onQueryChange = viewModel::onQueryChange,
        onClearFilter = viewModel::clearFilter,
        onRefresh = viewModel::refresh,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
private fun HistoryContent(
    contentPadding: PaddingValues,
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onClearFilter: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        // Search filter (always visible at top)
        item(key = "search") {
            SearchField(
                query = state.queryText,
                onQueryChange = onQueryChange,
                onClear = onClearFilter,
                resultCount = state.items?.size
            )
        }

        // Content based on state
        when (val itemsState = state.itemsState) {
            UiState.Loading -> {
                item(key = "loading") {
                    LoadingState()
                }
            }

            is UiState.Error -> {
                item(key = "error") {
                    ErrorState(
                        message = itemsState.message,
                        onRetry = onRefresh
                    )
                }
            }

            is UiState.Success -> {
                val items = itemsState.data

                if (items.isEmpty()) {
                    item(key = "empty") {
                        HistoryEmptyState(
                            hasFilter = state.appliedTag != null,
                            onClearFilter = onClearFilter
                        )
                    }
                } else {
                    // List header
                    item(key = "list_header") {
                        ListHeader(count = items.size)
                    }

                    // Measurement items
                    items(
                        items = items,
                        key = { it.id }
                    ) { item ->
                        MeasurementListItem(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }

            UiState.Idle -> { /* Should not happen */
            }
        }

        // Bottom spacing
        item(key = "bottom_spacer") {
            Spacer(Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    resultCount: Int?
) {
    AppSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onClear = onClear
    )
}

@Composable
private fun ListHeader(count: Int) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Measurements",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun MeasurementListItem(
    item: HistoryItemUi,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                // Timestamp
                Text(
                    text = item.timeText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Transport + metrics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Text(
                        text = item.transportText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "RTT ${item.rttText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "TTFB ${item.ttfbText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Location indicator + feedback tag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.hasLocation) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    item.feedbackTag?.let { tag ->
                        if (item.hasLocation) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Chevron
            Spacer(Modifier.width(spacing.sm))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun LoadingState() {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Loading measurements...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(
    hasFilter: Boolean,
    onClearFilter: () -> Unit
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (hasFilter) {
                // No results for this search
                AppEmptyState(
                    title = "No results",
                    description = "Clear filter to see all measurements",
                    icon = Icons.Filled.Close,
                    action = {
                        TextButton(onClick = onClearFilter) {
                            Text("Clear filter")
                        }
                    }
                )
            } else {
                // No measurements at all
                AppEmptyState(
                    title = "No measurements yet",
                    description = "Tap 'Start measurement' to begin",
                    icon = Icons.Filled.Search
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryContent(
        contentPadding = PaddingValues(0.dp),
        state = HistoryUiState(
            itemsState = UiState.Success(
                data = listOf()
            )
        ),
        onQueryChange = {},
        onClearFilter = {},
        onRefresh = {},
        onNavigateToDetail = {}
    )
}