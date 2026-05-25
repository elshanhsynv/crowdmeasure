package com.example.crowdmeasure.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.usecase.GetHistoryUseCase
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    private val queryText = MutableStateFlow("")
    private val manualRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(FlowPreview::class)
    private val appliedQuery: StateFlow<String?> = queryText
        .debounce(350)
        .map { it.trim().takeIf(String::isNotBlank) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    private val refreshes: Flow<Unit> =
        manualRefreshTrigger.onStart { emit(Unit) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemsState: StateFlow<UiState<List<HistoryItemUi>>> =
        combine(appliedQuery, refreshes) { query, _ -> query }
            .flatMapLatest { query ->
                // Fetch recent history WITHOUT tag filter, then filter locally by query.
                getHistoryUseCase(limit = 100, feedbackTag = null)
                    .map { items ->
                        val uiItems = items.map { it.toHistoryItemUi(dateFormatter) }
                        val filtered = filterAndRank(uiItems, query)
                        UiState.Success(filtered) as UiState<List<HistoryItemUi>>
                    }
                    .onStart { emit(UiState.Loading as UiState<List<HistoryItemUi>>) }
                    .catch { t ->
                        emit(
                            UiState.Error(
                                message = "Couldn't load history. Check connection and retry.",
                                throwable = t
                            ) as UiState<List<HistoryItemUi>>
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = UiState.Loading as UiState<List<HistoryItemUi>>
            )

    val uiState: StateFlow<HistoryUiState> = combine(
        queryText,
        appliedQuery,
        itemsState
    ) { query, applied, items ->
        HistoryUiState(
            queryText = query,
            appliedTag = applied, // keep field name for UI; now it represents applied query
            itemsState = items
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HistoryUiState()
    )

    fun onQueryChange(text: String) {
        queryText.value = text
    }

    fun clearFilter() {
        queryText.value = ""
    }

    fun refresh() {
        manualRefreshTrigger.tryEmit(Unit)
    }

    // ------------------------------------------------------------
    // Local search across multiple fields + ranking
    // ------------------------------------------------------------

    private fun filterAndRank(items: List<HistoryItemUi>, query: String?): List<HistoryItemUi> {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return items

        val tokens = q.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return items.mapNotNull { item ->
            val haystack = buildHaystack(item)
            val score = score(item, haystack, tokens)
            if (score <= 0) null else item to score
        }
            .sortedWith(compareByDescending<Pair<HistoryItemUi, Int>> { it.second }
                // stable-ish tie break: newer first if you ever add timestamp; otherwise timeText
                .thenByDescending { it.first.timeText })
            .map { it.first }
    }

    private fun buildHaystack(item: HistoryItemUi): String {
        // Keep everything you want searchable here
        return listOfNotNull(
            item.transportText,
            item.carrierName,
            item.registeredRat,
            item.dataNetworkType,
            item.protocol,
            item.endpointIdOrHost,
            item.rttText,
            item.ttfbText,
            if (item.hasLocation) "location" else null
        ).joinToString(" ").lowercase()
    }

    private fun score(item: HistoryItemUi, haystack: String, tokens: List<String>): Int {
        // require all tokens present (AND semantics)
        if (!tokens.all { haystack.contains(it) }) return 0

        val fullQuery = tokens.joinToString(" ")
        var score = 0

        fun bump(field: String?, exact: Int, prefix: Int, contains: Int) {
            val f = field?.lowercase() ?: return
            if (f == fullQuery) score += exact
            if (f.startsWith(fullQuery)) score += prefix
            if (f.contains(fullQuery)) score += contains
        }

        // Strong: carrier / RAT / endpoint host
        bump(item.carrierName, exact = 70, prefix = 45, contains = 25)
        bump(item.registeredRat, exact = 60, prefix = 40, contains = 20)
        bump(item.endpointIdOrHost, exact = 60, prefix = 40, contains = 20)

        // Medium: transport / protocol / data type
        bump(item.transportText, exact = 35, prefix = 20, contains = 12)
        bump(item.protocol, exact = 25, prefix = 15, contains = 10)
        bump(item.dataNetworkType, exact = 25, prefix = 15, contains = 10)

        // Small: metrics text matches
        bump(item.rttText, exact = 10, prefix = 8, contains = 6)
        bump(item.ttfbText, exact = 10, prefix = 8, contains = 6)

        // baseline so general matches still show up
        score += 5 * tokens.size

        return score
    }
}

private fun Measurement.toHistoryItemUi(formatter: SimpleDateFormat): HistoryItemUi {
    val endpointHost = safeEndpointHost(performance.endpointId) ?: performance.endpointId

    return HistoryItemUi(
        id = meta.measurementId,
        timeText = formatter.format(Date(meta.timestampUtcMs)),
        transportText = environment.network.transport.toString(),
        rttText = performance.rttAvgMs?.let { "$it ms" } ?: "—",
        ttfbText = performance.ttfbMs?.let { "$it ms" } ?: "—",
        hasLocation = environment.location != null,
        carrierName = environment.network.cell?.carrier?.carrierName,
        registeredRat = environment.network.cell?.rat,
        dataNetworkType = environment.network.cell?.dataNetworkType,
        protocol = performance.protocol.toString(),
        endpointIdOrHost = endpointHost
    )
}

private fun safeEndpointHost(endpointId: String?): String? {
    if (endpointId.isNullOrBlank()) return null
    return runCatching {
        // works for full URLs; if it's already an ID/host, URI may treat it oddly -> fallback null
        val uri = URI(endpointId)
        uri.host
    }.getOrNull()
}
