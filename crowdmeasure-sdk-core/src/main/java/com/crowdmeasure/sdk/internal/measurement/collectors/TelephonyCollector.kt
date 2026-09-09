package com.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.telephony.CellIdentityNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthNr
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.telephony.CellInfo as AndroidCellInfo
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.CellRadio
import com.crowdmeasure.sdk.model.NrSignal
import com.crowdmeasure.sdk.model.CellRadioSnapshot
import com.crowdmeasure.sdk.model.NrState

internal enum class TelephonyRat {
    LTE,
    NR,
    OTHER,
}

internal data class CellSelectionCandidate(
    val rat: TelephonyRat,
    val registered: Boolean,
    val connectionStatus: Int,
    val ageMs: Long,
    val signalDbm: Int,
)

data class SubscriptionDisplayInfo(
    val subscriptionId: Int?,
    val displayInfo: TelephonyDisplayInfo,
)

internal object TelephonyCollectorLogic {
    const val MAX_CELL_AGE_MS = 60_000L // 1 minute

    fun deriveNrState(
        dataNetworkType: Int?,
        displayNetworkType: Int?,
        displayOverrideNetworkType: Int?,
        hasRegisteredNr: Boolean,
    ): NrState {
        if (dataNetworkType == TelephonyManager.NETWORK_TYPE_NR ||
            displayNetworkType == TelephonyManager.NETWORK_TYPE_NR
        ) return NrState.SA

        if (displayOverrideNetworkType?.let(::isNrNsaOverride) == true) {
            return NrState.NSA
        }

        return when (dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> NrState.SA
            TelephonyManager.NETWORK_TYPE_LTE -> if (hasRegisteredNr) NrState.NSA else NrState.NONE
            else -> NrState.NONE
        }
    }

    fun selectServingIndex(
        nrState: NrState,
        candidates: List<CellSelectionCandidate>,
    ): Int? {
        fun best(items: Iterable<IndexedValue<CellSelectionCandidate>>): Int? {
            val itemList = items.toList()
            val fresh = itemList.filter { it.value.ageMs <= MAX_CELL_AGE_MS }.ifEmpty { itemList }

            return fresh.sortedWith(
                compareByDescending<IndexedValue<CellSelectionCandidate>> {
                    connectionStatusRank(it.value.connectionStatus)
                }
                    .thenByDescending { signalRank(it.value.signalDbm) }
                    .thenBy { it.value.ageMs }
                    .thenBy { it.index }
            ).firstOrNull()?.index
        }

        val registered = candidates.withIndex().filter { it.value.registered }

        val compatible = registered.filter { (_, cell) ->
            when (nrState) {
                NrState.SA -> cell.rat == TelephonyRat.NR
                NrState.NSA -> cell.rat == TelephonyRat.LTE
                NrState.NONE -> true
            }
        }

        return best(compatible)
    }

    fun coarseRatName(dataNetworkType: Int?, displayNetworkType: Int?): String? =
        dataNetworkType?.let(::networkTypeName) ?: displayNetworkType?.let(::networkTypeName)

    fun signalRank(dbm: Int): Int =
        dbm.validSig() ?: Int.MIN_VALUE

    fun connectionStatusRank(connectionStatus: Int): Int =
        when (connectionStatus) {
            AndroidCellInfo.CONNECTION_PRIMARY_SERVING -> 3
            AndroidCellInfo.CONNECTION_SECONDARY_SERVING -> 2
            AndroidCellInfo.CONNECTION_UNKNOWN -> 1
            else -> 0
        }

    @Suppress("DEPRECATION")
    fun isNrNsaOverride(overrideNetworkType: Int): Boolean =
        overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE ||
                overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED

    private fun Int.validSig(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    @Suppress("DEPRECATION")
    fun networkTypeName(type: Int): String? = when (type) {
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "NR"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
        TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
        TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
        TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
        TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
        TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
        TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
        TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
        TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
        TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
        TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
        TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> null
        else -> null
    }
}

object TelephonyCollector {

    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun collect(
        context: Context,
        cachedDisplayInfo: TelephonyDisplayInfo? = null,
        cachedSubscriptionDisplayInfo: SubscriptionDisplayInfo? = null,
        measurementStartElapsedRealtimeMs: Long? = null,
    ): CellInfo {
        require(measurementStartElapsedRealtimeMs == null || measurementStartElapsedRealtimeMs >= 0)
        val tm = context.getSystemService<TelephonyManager>()
            ?: return CellInfo(
                simCarriers = emptyList(),
                rat = null,
                nrState = NrState.NONE,
                serving = null,
            )

        val sm = context.getSystemService<SubscriptionManager>()
        val phoneGranted = PlatformChecks.hasPhoneState(context)

        val simCarriers = collectSimCarriers(tm, sm, phoneGranted)
        val selectedCarrier = simCarriers.collectedCarrier()
        val targetTm = selectedCarrier?.subscriptionId
            ?.let { safe { tm.createForSubscriptionId(it) } }
            ?: tm

        val serviceState = serviceStateOrNull(targetTm, phoneGranted)
        val fallbackCarrier = targetTm.toCarrierInfo(serviceState)
        val collectedCarrier = selectedCarrier ?: fallbackCarrier
        val collectedSimCarriers = simCarriers.ifEmpty { listOf(collectedCarrier) }

        val dataType: Int? = if (phoneGranted) safe { targetTm.dataNetworkType } else null

        val voiceType: Int? = if (phoneGranted) safe { targetTm.voiceNetworkType } else null

        val displayInfo = cachedSubscriptionDisplayInfo
            ?.takeIf { it.subscriptionId == collectedCarrier.subscriptionId }
            ?.displayInfo
            ?: cachedDisplayInfo.takeIf {
                cachedSubscriptionDisplayInfo == null && collectedCarrier.subscriptionId == null
            }
        val coarseNrState = deriveNrState(
            displayInfo = displayInfo,
            dataNetworkType = dataType,
            infos = emptyList(),
        )
        val coarseRat = TelephonyCollectorLogic.coarseRatName(
            dataNetworkType = dataType,
            displayNetworkType = displayInfo.networkTypeOrNull(),
        )

        val base = CellInfo(
            simCarriers = collectedSimCarriers,
            collectedSubscriptionId = collectedCarrier.subscriptionId,
            collectedSimSlotIndex = collectedCarrier.simSlotIndex,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { targetTm.isNetworkRoaming },
            rat = coarseRat,
            nrState = coarseNrState,
            serving = null,
            neighbors = emptyList(),
        )

        val fineGranted = PlatformChecks.hasFineLocation(context)
        val locationOn = PlatformChecks.isLocationServicesEnabled(context)
        if (!fineGranted || !locationOn) return base

        val infos: List<AndroidCellInfo> = try {
            targetTm.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            return base
        } catch (_: RuntimeException) {
            return base
        }

        val nrState = deriveNrState(
            displayInfo = displayInfo,
            dataNetworkType = dataType,
            infos = infos,
        )

        // Unsupported or unparseable cells must not displace a supported serving cell.
        val parsedCells = infos.mapNotNull { info ->
            parseCell(info, measurementStartElapsedRealtimeMs)?.let { parsed ->
                info to parsed
            }
        }
        val supportedInfos = parsedCells.map { it.first }
        val candidate = selectServingCell(supportedInfos, nrState)
        val parsedServing = parsedCells.firstOrNull { it.first === candidate }?.second
        val neighbors = selectTopNeighbors(supportedInfos).mapNotNull { info ->
            parsedCells.firstOrNull { it.first === info }?.second?.snapshot
        }

        return base.copy(
            rat = parsedServing?.rat ?: coarseRat,
            nrState = nrState,
            serving = parsedServing?.snapshot,
            neighbors = neighbors,
        )
    }

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (_: SecurityException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun collectSimCarriers(
        tm: TelephonyManager,
        sm: SubscriptionManager?,
        phoneGranted: Boolean,
    ): List<CarrierInfo> {
        if (!phoneGranted || sm == null) return emptyList()

        val defaultDataSubId =
            safe { SubscriptionManager.getDefaultDataSubscriptionId() }.validSubId()
        val defaultVoiceSubId =
            safe { SubscriptionManager.getDefaultVoiceSubscriptionId() }.validSubId()
        val defaultSmsSubId =
            safe { SubscriptionManager.getDefaultSmsSubscriptionId() }.validSubId()
        val activeDataSubId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            safe { SubscriptionManager.getActiveDataSubscriptionId() }.validSubId()
        } else null

        return safe { sm.activeSubscriptionInfoList.orEmpty() }
            .orEmpty()
            .sortedWith(
                compareBy<SubscriptionInfo> { it.simSlotIndex.validSlotIndex() ?: Int.MAX_VALUE }
                    .thenBy { it.subscriptionId }
            )
            .map { info ->
                info.toCarrierInfo(
                    tm = tm,
                    defaultDataSubId = defaultDataSubId,
                    defaultVoiceSubId = defaultVoiceSubId,
                    defaultSmsSubId = defaultSmsSubId,
                    activeDataSubId = activeDataSubId,
                )
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun SubscriptionInfo.toCarrierInfo(
        tm: TelephonyManager,
        defaultDataSubId: Int?,
        defaultVoiceSubId: Int?,
        defaultSmsSubId: Int?,
        activeDataSubId: Int?,
    ): CarrierInfo {
        val subId = subscriptionId
        val subTm = safe { tm.createForSubscriptionId(subId) } ?: tm
        val networkOperator = safe { subTm.networkOperator }.orEmpty()
        val mcc = networkOperator.takeIf { it.length >= 3 }?.substring(0, 3)
            ?: safe { mccString }.takeIfNotBlank()
        val mnc = networkOperator.takeIf { it.length >= 5 }?.substring(3)
            ?: safe { mncString }.takeIfNotBlank()

        return CarrierInfo(
            carrierName = safe { subTm.networkOperatorName }.takeIfNotBlank()
                ?: safe { carrierName?.toString() }.takeIfNotBlank(),
            mcc = mcc,
            mnc = mnc,
            simOperatorId = safe { subTm.simOperator }.takeIfNotBlank()
                ?: run {
                    val homeMcc = safe { mccString }.takeIfNotBlank()
                    val homeMnc = safe { mncString }.takeIfNotBlank()
                    if (homeMcc != null && homeMnc != null) homeMcc + homeMnc else null
                },
            simOperatorName = safe { subTm.simOperatorName }.takeIfNotBlank()
                ?: safe { carrierName?.toString() }.takeIfNotBlank(),
            countryIso = safe { subTm.simCountryIso }.takeIfNotBlank()
                ?: safe { countryIso }.takeIfNotBlank(),
            duplexMode = serviceStateDuplexMode(subTm, phoneGranted = true),
            subscriptionId = subId.validSubId(),
            simSlotIndex = simSlotIndex.validSlotIndex(),
            displayName = safe { displayName?.toString() }.takeIfNotBlank(),
            carrierId = carrierId.validId(),
            dataRoaming = when (dataRoaming) {
                SubscriptionManager.DATA_ROAMING_ENABLE -> true
                SubscriptionManager.DATA_ROAMING_DISABLE -> false
                else -> null
            },
            isEmbedded = safe { isEmbedded },
            cardId = safe { cardId }?.validId(),
            isDefaultData = defaultDataSubId?.let { subId == it },
            isDefaultVoice = defaultVoiceSubId?.let { subId == it },
            isDefaultSms = defaultSmsSubId?.let { subId == it },
            isActiveData = activeDataSubId?.let { subId == it },
        )
    }

    private fun List<CarrierInfo>.collectedCarrier(): CarrierInfo? =
        firstOrNull { it.isActiveData == true }
            ?: firstOrNull { it.isDefaultData == true }
            ?: firstOrNull { it.simSlotIndex != null }
            ?: firstOrNull()

    private fun TelephonyManager.toCarrierInfo(serviceState: ServiceState?): CarrierInfo {
        val op = safe { networkOperator }.orEmpty()
        return CarrierInfo(
            carrierName = safe { networkOperatorName },
            mcc = op.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op.takeIf { it.length >= 5 }?.substring(3),
            simOperatorId = safe { simOperator },
            simOperatorName = safe { simOperatorName },
            countryIso = safe { simCountryIso },
            duplexMode = duplexModeString(serviceState),
        )
    }

    private const val MAX_NEIGHBORS = 5

    private data class NeighborCandidate(
        val info: AndroidCellInfo,
        val ageMs: Long,
        val signalDbm: Int,
        val signalLevel: Int,
        val originalIndex: Int,
    )

    private fun AndroidCellInfo.neighborSignalDbm(): Int? =
        when (this) {
            is CellInfoLte ->
                cellSignalStrength.rsrp.validSig()

            is CellInfoNr ->
                cellSignalStrength.dbm.validSig()

            is CellInfoWcdma ->
                cellSignalStrength.dbm.validSig()

            is CellInfoGsm ->
                cellSignalStrength.dbm.validSig()

            else -> null
        }

    private fun selectTopNeighbors(
        infos: List<AndroidCellInfo>,
    ): List<AndroidCellInfo> {
        return infos
            .withIndex()
            .asSequence()
            .filter { !it.value.isRegistered }
            .mapNotNull { indexed ->
                val info = indexed.value
                val dbm = info.neighborSignalDbm() ?: return@mapNotNull null

                NeighborCandidate(
                    info = info,
                    ageMs = info.ageMs(),
                    signalDbm = dbm,
                    signalLevel = info.signalStrengthOrNull()?.level ?: 0,
                    originalIndex = indexed.index,
                )
            }
            .sortedWith(
                compareByDescending<NeighborCandidate> { it.signalDbm }
                    .thenByDescending { it.signalLevel }
                    .thenBy { it.ageMs }
                    .thenBy { it.originalIndex }
            )
            .take(MAX_NEIGHBORS)
            .map { it.info }
            .toList()
    }

    private fun serviceStateDuplexMode(tm: TelephonyManager, phoneGranted: Boolean): String? {
        return duplexModeString(serviceStateOrNull(tm, phoneGranted))
    }

    @SuppressLint("MissingPermission")
    private fun serviceStateOrNull(tm: TelephonyManager, phoneGranted: Boolean): ServiceState? =
        if (phoneGranted) safe { tm.serviceState } else null

    private fun duplexModeString(serviceState: ServiceState?): String? =
        when (serviceState?.duplexMode) {
            ServiceState.DUPLEX_MODE_FDD -> "FDD"
            ServiceState.DUPLEX_MODE_TDD -> "TDD"
            else -> null
        }

    private fun String?.takeIfNotBlank(): String? =
        takeIf { !it.isNullOrBlank() }

    private fun Int?.validSubId(): Int? =
        this?.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID && it >= 0 }

    private fun Int.validSlotIndex(): Int? =
        takeIf { it != SubscriptionManager.INVALID_SIM_SLOT_INDEX && it >= 0 }

    private fun Int.validId(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE && it >= 0 }

    private fun Long.validId(): Long? =
        takeIf { it != Long.MAX_VALUE && it != Long.MIN_VALUE && it >= 0 }

    private fun Int.validSig(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    @Suppress("DEPRECATION")
    private fun AndroidCellInfo.observedElapsedRealtimeMs(): Long? = safe {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            timestampMillis.takeIf { it >= 0 && it != Long.MAX_VALUE }
        } else {
            timeStamp.takeIf { it >= 0 && it != Long.MAX_VALUE }?.div(1_000_000L)
        }
    }

    private fun AndroidCellInfo.ageMs(): Long =
        observedElapsedRealtimeMs()?.let { observed ->
            val now = SystemClock.elapsedRealtime()
            if (observed <= now) now - observed else Long.MAX_VALUE
        } ?: Long.MAX_VALUE

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        displayInfo: TelephonyDisplayInfo?,
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>,
    ): NrState {
        val hasRegisteredNr = infos.any { it is CellInfoNr && it.isRegistered }
        return TelephonyCollectorLogic.deriveNrState(
            dataNetworkType = dataNetworkType,
            displayNetworkType = displayInfo.networkTypeOrNull(),
            displayOverrideNetworkType = displayInfo.overrideNetworkTypeOrNull(),
            hasRegisteredNr = hasRegisteredNr,
        )
    }

    private fun TelephonyDisplayInfo?.networkTypeOrNull(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this?.networkType else null

    private fun TelephonyDisplayInfo?.overrideNetworkTypeOrNull(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this?.overrideNetworkType else null

    private fun selectServingCell(
        infos: List<AndroidCellInfo>,
        nrState: NrState,
    ): AndroidCellInfo? {
        val candidates = infos.map { info ->
            CellSelectionCandidate(
                rat = info.telephonyRat(),
                registered = info.isRegistered,
                connectionStatus = info.cellConnectionStatus,
                ageMs = info.ageMs(),
                signalDbm = info.signalDbm(),
            )
        }
        val index = TelephonyCollectorLogic.selectServingIndex(nrState, candidates)
        return index?.let(infos::getOrNull)
    }

    private fun AndroidCellInfo.telephonyRat(): TelephonyRat =
        when (this) {
            is CellInfoLte -> TelephonyRat.LTE
            is CellInfoNr -> TelephonyRat.NR
            else -> TelephonyRat.OTHER
        }

    private fun AndroidCellInfo.signalDbm(): Int =
        when (this) {
            is CellInfoLte -> cellSignalStrength.dbm
            is CellInfoNr -> cellSignalStrength.dbm
            is CellInfoWcdma -> cellSignalStrength.dbm
            is CellInfoGsm -> cellSignalStrength.dbm
            else -> Int.MIN_VALUE
        }

    private data class Parsed(
        val snapshot: CellRadioSnapshot,
        val rat: String,
    )

    private fun AndroidCellInfo.signalStrengthOrNull(): CellSignalStrength? = when (this) {
        is CellInfoGsm -> cellSignalStrength
        is CellInfoWcdma -> cellSignalStrength
        is CellInfoLte -> cellSignalStrength
        is CellInfoNr -> cellSignalStrength
        else -> null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(
        ci: AndroidCellInfo,
        measurementStartElapsedRealtimeMs: Long?,
    ): Parsed? = safe {
        val signal = ci.signalStrengthOrNull() ?: return@safe null
        val radio: CellRadio = when (ci) {
            is CellInfoGsm -> {
                val id = ci.cellIdentity
                val sig = ci.cellSignalStrength
                CellRadio.Gsm(
                    cellId = id.cid.validId(),
                    lac = id.lac.validId(),
                    bsic = id.bsic.validId(),
                    arfcn = id.arfcn.validId(),
                    rssiDbm = sig.dbm.validSig(),
                    timingAdvance = sig.timingAdvance.validId(),
                )
            }

            is CellInfoWcdma -> {
                val id = ci.cellIdentity
                val sig = ci.cellSignalStrength
                CellRadio.Wcdma(
                    cellId = id.cid.validId(),
                    lac = id.lac.validId(),
                    psc = id.psc.validId(),
                    uarfcn = id.uarfcn.validId(),
                    ecNoDb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        sig.ecNo.validSig()
                    } else null,
                )
            }

            is CellInfoLte -> {
                val id = ci.cellIdentity
                val sig = ci.cellSignalStrength
                CellRadio.Lte(
                    cellId = id.ci.validId(),
                    tac = id.tac.validId(),
                    pci = id.pci.validId(),
                    earfcn = id.earfcn.validId(),
                    bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        id.bands.validBands()
                    } else emptyList(),
                    bandwidthKhz = id.bandwidth.validId()?.takeIf { it > 0 },
                    rsrpDbm = sig.rsrp.validSig(),
                    rsrqDb = sig.rsrq.validSig(),
                    rssnrDb = sig.rssnr.validSig(),
                    rssiDbm = sig.rssi.validSig(),
                    cqi = sig.cqi.validId(),
                    timingAdvance = sig.timingAdvance.validId(),
                    mimoLayers = null,
                )
            }

            is CellInfoNr -> {
                val id = ci.cellIdentity as? CellIdentityNr ?: return@safe null
                val sig = signal as? CellSignalStrengthNr ?: return@safe null
                CellRadio.Nr(
                    cellId = id.nci.validId(),
                    tac = id.tac.validId(),
                    pci = id.pci.validId(),
                    nrarfcn = id.nrarfcn.validId(),
                    bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        id.bands.validBands()
                    } else emptyList(),
                    ss = nrSignalOrNull(sig.ssRsrp, sig.ssRsrq, sig.ssSinr),
                    // All three CSI measurements are public starting at API 29.
                    csi = nrSignalOrNull(sig.csiRsrp, sig.csiRsrq, sig.csiSinr),
                    bandwidthKhz = null,
                    mimoLayers = null,
                )
            }

            else -> return@safe null
        }

        val rat = when (radio) {
            is CellRadio.Gsm -> "GSM"
            is CellRadio.Wcdma -> "WCDMA"
            is CellRadio.Lte -> "LTE"
            is CellRadio.Nr -> "NR"
        }
        val asuRange = when (radio) {
            is CellRadio.Gsm -> 0..31
            is CellRadio.Wcdma -> 0..96
            is CellRadio.Lte, is CellRadio.Nr -> 0..97
        }
        val offsetMs = measurementStartElapsedRealtimeMs?.let { start ->
            ci.observedElapsedRealtimeMs()?.let { observed -> observed - start }
        }

        Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = offsetMs,
                radio = radio,
                dbm = signal.dbm.validSig(),
                asuLevel = signal.asuLevel.takeIf { it in asuRange },
                level = signal.level.takeIf { it in 0..4 },
                subscriptionId = null,
                isRegistered = ci.isRegistered,
            ),
            rat = rat,
        )
    }

    private fun IntArray.validBands(): List<Int> =
        filter { it > 0 && it != Int.MAX_VALUE }.distinct()

    private fun nrSignalOrNull(rsrp: Int, rsrq: Int, sinr: Int): NrSignal? =
        NrSignal(
            rsrpDbm = rsrp.validSig(),
            rsrqDb = rsrq.validSig(),
            sinrDb = sinr.validSig(),
        ).takeIf { it.rsrpDbm != null || it.rsrqDb != null || it.sinrDb != null }

    private fun networkTypeName(type: Int): String? =
        TelephonyCollectorLogic.networkTypeName(type)
}
