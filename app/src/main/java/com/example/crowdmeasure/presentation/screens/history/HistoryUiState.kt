package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.runtime.Immutable
import com.example.crowdmeasure.presentation.util.UiState

/**
 * UI state for History screen.
 *
 * @property queryText Current search text (shown in search field)
 * @property appliedTag Actual applied filter (debounced, null = no filter)
 * @property itemsState State of the measurements list
 */
@Immutable
data class HistoryUiState(
    val queryText: String = "",
    val appliedTag: String? = null,
    val itemsState: UiState<List<HistoryItemUi>> = UiState.Loading
) {
    /**
     * Whether the list is currently loading.
     */
    val isLoading: Boolean
        get() = itemsState is UiState.Loading

    /**
     * Whether an error occurred.
     */
    val hasError: Boolean
        get() = itemsState is UiState.Error

    /**
     * The actual list items (null if loading/error).
     */
    val items: List<HistoryItemUi>?
        get() = (itemsState as? UiState.Success)?.data

    /**
     * Whether the list is empty (after successful load).
     */
    val isEmpty: Boolean
        get() = items?.isEmpty() == true
}

/**
 * UI model for a single measurement in the history list.
 *
 * Contains only the data needed for list display (not full measurement).
 * Tap to navigate to detail screen for full data.
 */
@Immutable
data class HistoryItemUi(
    val id: String,
    val timeText: String,
    val transportText: String,
    val rttText: String,
    val ttfbText: String,
    val hasLocation: Boolean,
    val carrierName: String? = null,
    val registeredRat: String? = null,
    val dataNetworkType: String? = null,
    val protocol: String? = null,
    val endpointIdOrHost: String? = null
)
