package com.example.crowdmeasure.presentation.screens.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.crowdmeasure.sdk.model.Measurement
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.GetHistoryUseCase
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
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
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val appPreferences: AppPreferences,
    userSessionRepository: UserSessionRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    private val queryText = MutableStateFlow("")
    private val transportFilter = MutableStateFlow(HistoryTransportFilter.All)
    private val manualRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val batteryOptimizationIgnored = MutableStateFlow(
        AppPermissions.ignoresBatteryOptimizations(context)
    )
    private val batteryOptimizationRefreshTime = MutableStateFlow(System.currentTimeMillis())

    @OptIn(FlowPreview::class)
    private val appliedQuery: StateFlow<String?> = queryText
        .debounce(350.milliseconds)
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
        combine(appliedQuery, transportFilter, refreshes) { query, filter, _ -> query to filter }
            .flatMapLatest { (query, filter) ->
                // Fetch recent history WITHOUT tag filter, then filter locally by query.
                getHistoryUseCase(limit = 100, feedbackTag = null)
                    .map { items ->
                        val uiItems = items.map { it.toHistoryItemUi(dateFormatter) }
                        val filtered = filterAndRank(uiItems, query, filter)
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
        transportFilter,
        itemsState,
        combine(
            userSessionRepository.settings,
            appPreferences.batteryOptimizationRecommendationDismissedUntil,
            batteryOptimizationIgnored,
            batteryOptimizationRefreshTime
        ) { settings, dismissedUntil, ignored, refreshTime ->
            val backgroundFeatureEnabled = settings.autoRunEnabled ||
                settings.callSamplingEnabled ||
                settings.voipCallSamplingEnabled
            backgroundFeatureEnabled &&
                !ignored &&
                refreshTime >= dismissedUntil
        }
    ) { query, applied, filter, items, batteryRecommendationEligible ->
        HistoryUiState(
            queryText = query,
            appliedTag = applied, // keep field name for UI; now it represents applied query
            transportFilter = filter,
            itemsState = items,
            showBatteryOptimizationRecommendation =
                batteryRecommendationEligible &&
                    (items as? UiState.Success)?.data?.isNotEmpty() == true
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

    fun clearFilters() {
        queryText.value = ""
        transportFilter.value = HistoryTransportFilter.All
    }

    fun setTransportFilter(filter: HistoryTransportFilter) {
        transportFilter.value = filter
    }

    fun refresh() {
        manualRefreshTrigger.tryEmit(Unit)
    }

    fun refreshBatteryOptimizationStatus() {
        batteryOptimizationIgnored.value = AppPermissions.ignoresBatteryOptimizations(context)
        batteryOptimizationRefreshTime.value = System.currentTimeMillis()
    }

    fun dismissBatteryOptimizationRecommendation() {
        viewModelScope.launch {
            appPreferences.setBatteryOptimizationRecommendationDismissedUntil(
                System.currentTimeMillis() + BATTERY_RECOMMENDATION_COOLDOWN_MS
            )
        }
    }

    // ------------------------------------------------------------
    // Local search across multiple fields + ranking
    // ------------------------------------------------------------

    private fun filterAndRank(
        items: List<HistoryItemUi>,
        query: String?,
        transportFilter: HistoryTransportFilter
    ): List<HistoryItemUi> {
        val transportFiltered = items.filter { item ->
            when (transportFilter) {
                HistoryTransportFilter.All -> true
                HistoryTransportFilter.Wifi -> item.isWifi
                HistoryTransportFilter.Cellular -> !item.isWifi
            }
        }
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return transportFiltered

        val tokens = q.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return transportFiltered.mapNotNull { item ->
            val haystack = buildHaystack(item)
            val score = score(item, haystack, tokens)
            if (score <= 0) null else item to score
        }
            .sortedWith(compareByDescending<Pair<HistoryItemUi, Int>> { it.second }
                // stable-ish tie-break: newer first if you ever add timestamp; otherwise timeText
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
            item.httpLatText,
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
        bump(item.httpLatText, exact = 10, prefix = 8, contains = 6)
        bump(item.ttfbText, exact = 10, prefix = 8, contains = 6)

        // baseline so general matches still show up
        score += 5 * tokens.size

        return score
    }

    private companion object {
        const val BATTERY_RECOMMENDATION_COOLDOWN_MS = 1L * 24 * 60 * 60 * 1_000
    }
}

private fun Measurement.toHistoryItemUi(formatter: SimpleDateFormat): HistoryItemUi {
    val endpointHost = safeEndpointHost(performance.endpointId) ?: performance.endpointId

    return HistoryItemUi(
        id = meta.measurementId,
        timeText = formatter.format(Date(meta.timestampUtcMs)),
        transportText = environment.network.transport.toString(),
        httpLatText = performance.httpLatencyAvgMs?.let { "$it ms" } ?: "—",
        ttfbText = performance.ttfbAvgMs?.let { "$it ms" } ?: "—",
        dnsText = performance.dnsMs?.let { "$it ms" } ?: "—",
        hasLocation = environment.location != null,
        carrierName = environment.network.cell?.let { cell ->
            cell.simCarriers.firstOrNull { sim ->
                sim.subscriptionId != null && sim.subscriptionId == cell.collectedSubscriptionId
            }?.carrierName
                ?: cell.simCarriers.firstOrNull { sim ->
                    sim.simSlotIndex != null && sim.simSlotIndex == cell.collectedSimSlotIndex
                }?.carrierName
                ?: cell.simCarriers.firstOrNull()?.carrierName
        },
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
