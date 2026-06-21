package com.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.telephony.CellIdentityNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
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
    const val MAX_CELL_AGE_MS = 30_000L

    fun deriveNrState(
        dataNetworkType: Int?,
        displayNetworkType: Int?,
        displayOverrideNetworkType: Int?,
        hasRegisteredNr: Boolean,
    ): NrState {
        if (displayOverrideNetworkType?.let(::isNrNsaOverride) == true) {
            return NrState.NSA
        }

        if (displayNetworkType == TelephonyManager.NETWORK_TYPE_NR) {
            return NrState.SA
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
                overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE

    private fun Int.validSig(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    @Suppress("DEPRECATION")
    private fun networkTypeName(type: Int): String? = when (type) {
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

/**
 * Collects a telephony snapshot supporting all active RATs:
 * GSM (2G), WCDMA / TD-SCDMA (3G), LTE (4G), NR (5G).
 *
 * Assumption: minSdk >= Q (29). [RequiresApi] annotations on private
 * functions are present for lint correctness; they are unreachable on < Q.
 */
object TelephonyCollector {

    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun collect(
        context: Context,
        cachedDisplayInfo: TelephonyDisplayInfo? = null,
        cachedSubscriptionDisplayInfo: SubscriptionDisplayInfo? = null,
    ): CellInfo {
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

        val serviceState = if (phoneGranted) safe { targetTm.serviceState } else null
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
            displayNetworkType = displayInfo?.networkType,
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

        val candidate = selectServingCell(infos, nrState)

        val parsedServing = candidate?.let { parseCell(it) }
        val neighbors = infos
            .filterNot { it.isRegistered }
            .filter { it.ageMs() <= TelephonyCollectorLogic.MAX_CELL_AGE_MS }
            .mapNotNull { parseCell(it).snapshot }

        return CellInfo(
            simCarriers = collectedSimCarriers,
            collectedSubscriptionId = collectedCarrier.subscriptionId,
            collectedSimSlotIndex = collectedCarrier.simSlotIndex,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { targetTm.isNetworkRoaming },
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
        // Opportunistic hidden/API-version-dependent signal; carrier selection falls
        // back to default data SIM when this is unavailable.
        val activeDataSubId = reflectStaticInt(
            SubscriptionManager::class.java,
            "getActiveDataSubscriptionId",
        ).validSubId()

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
                ?: listOfNotNull(mcc, mnc).takeIf { it.size == 2 }?.joinToString(separator = ""),
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
            isOpportunistic = safe { isOpportunistic },
            cardId = reflectInt(this, "getCardId")?.validId(),
            portIndex = reflectInt(this, "getPortIndex")?.validId(),
            isDefaultData = subId == defaultDataSubId,
            isDefaultVoice = subId == defaultVoiceSubId,
            isDefaultSms = subId == defaultSmsSubId,
            isActiveData = subId == activeDataSubId,
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

    private fun serviceStateDuplexMode(tm: TelephonyManager, phoneGranted: Boolean): String {
        if (!phoneGranted) return ""
        val serviceState = safe { tm.serviceState }

        return duplexModeString(serviceState)
    }

    private fun duplexModeString(serviceState: ServiceState?): String =
        when (serviceState?.duplexMode) {
            ServiceState.DUPLEX_MODE_FDD -> "FDD"
            ServiceState.DUPLEX_MODE_TDD -> "TDD"
            else -> ""
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

    private fun signalRank(dbm: Int): Int =
        TelephonyCollectorLogic.signalRank(dbm)

    @Suppress("DEPRECATION")
    private fun AndroidCellInfo.ageMs(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (SystemClock.elapsedRealtime() - timestampMillis).coerceAtLeast(0L)
        } else {
            ((SystemClock.elapsedRealtimeNanos() - timeStamp) / 1_000_000L).coerceAtLeast(0L)
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        displayInfo: TelephonyDisplayInfo?,
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>,
    ): NrState {
        val hasRegisteredNr = infos.any { it is CellInfoNr && it.isRegistered }
        return TelephonyCollectorLogic.deriveNrState(
            dataNetworkType = dataNetworkType,
            displayNetworkType = displayInfo?.networkType,
            displayOverrideNetworkType = displayInfo?.overrideNetworkType,
            hasRegisteredNr = hasRegisteredNr,
        )
    }

    private data class Parsed(
        val snapshot: CellRadioSnapshot?,
        val rat: String?,
    )

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
            is CellInfoTdscdma -> cellSignalStrength.dbm
            is CellInfoGsm -> cellSignalStrength.dbm
            else -> Int.MIN_VALUE
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(ci: AndroidCellInfo): Parsed = try {
        when (ci) {
            is CellInfoLte -> parseLte(ci)
            is CellInfoNr -> parseNr(ci)
            is CellInfoWcdma -> parseWcdma(ci)
            is CellInfoTdscdma -> parseTdscdma(ci)
            is CellInfoGsm -> parseGsm(ci)
            // CDMA/EVDO cell identity parsing is intentionally unsupported.
            else -> Parsed(null, ci.javaClass.simpleName)
        }
    } catch (_: SecurityException) {
        Parsed(null, ci.javaClass.simpleName)
    } catch (_: RuntimeException) {
        Parsed(null, ci.javaClass.simpleName)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id.bands.firstOrNull()
        } else null

        // getBandwidth() returns kHz (API 28, always available at minSdk=Q)
        val bandwidthMhz: Int? = id.bandwidth
            .takeIf { it != Int.MAX_VALUE && it > 0 }
            ?.let { it / 1000 }

        // getCqi() is @hide; may return null on newer OSes that removed it
        val cqi: Int? = reflectInt(sig, "getCqi")?.validSig()

        return Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = ci.ageMs(),
                cellId = id.ci.validId(),
                cid = null,
                nci = null,
                lac = null,
                tac = id.tac.validId(),
                pci = id.pci.validId(),
                psc = null,
                bsic = null,
                band = band,
                arfcn = id.earfcn.validId(),
                uarfcn = null,
                nrarfcn = null,
                rsrpDbm = sig.rsrp.validSig(),
                rsrqDb = sig.rsrq.validSig(),
                sinrDb = sig.rssnr.validSig(),
                rssiDbm = sig.rssi.validSig(),
                cqi = cqi,
                asuLevel = sig.asuLevel.validSig(),
                dbm = sig.dbm.validSig(),
                timingAdvance = sig.timingAdvance.validSig(),
                ssRsrpDbm = null,
                ssRsrqDb = null,
                ssSinrDb = null,
                csiRsrpDbm = null,
                csiRsrqDb = null,
                csiSinrDb = null,
                bandwidthMhz = bandwidthMhz,
                mimoLayers = null,
            ),
            rat = "LTE",
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(ci: CellInfoNr): Parsed {
        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id?.bands?.firstOrNull()
        } else null

        // NR CQI is exposed inconsistently across API/OEM builds; keep it best-effort.
        val nrCqi: Int? = reflectIntList(sig, "getCsiCqiReport")
            ?.firstNotNullOfOrNull { it.validSig() }
            ?: reflectInt(sig, "getCqi")?.validSig()

        // CSI-RSRP / CSI-RSRQ available from API 31 (S)
        val csiRsrpDbm: Int?
        val csiRsrqDb: Int?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            csiRsrpDbm = sig?.csiRsrp?.validSig()
            csiRsrqDb = sig?.csiRsrq?.validSig()
        } else {
            csiRsrpDbm = null
            csiRsrqDb = null
        }

        return Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = ci.ageMs(),
                cellId = null,
                cid = null,
                nci = id?.nci?.validId(),
                lac = null,
                tac = id?.tac?.validId(),
                pci = id?.pci?.validId(),
                psc = null,
                bsic = null,
                band = band,
                arfcn = null,
                uarfcn = null,
                nrarfcn = id?.nrarfcn?.validId(),
                // Generic signal fields: map from SS measurements (standard NR signal path)
                rsrpDbm = sig?.ssRsrp?.validSig(),
                rsrqDb = sig?.ssRsrq?.validSig(),
                sinrDb = sig?.ssSinr?.validSig(),
                rssiDbm = null, // No standard RSSI in NR public API
                cqi = nrCqi,
                asuLevel = sig?.asuLevel?.validSig(),
                dbm = sig?.dbm?.validSig(),           // Returns SS-RSRP as primary indicator
                timingAdvance = null,
                // NR-specific separated measurements
                ssRsrpDbm = sig?.ssRsrp?.validSig(),
                ssRsrqDb = sig?.ssRsrq?.validSig(),
                ssSinrDb = sig?.ssSinr?.validSig(),
                csiRsrpDbm = csiRsrpDbm,
                csiRsrqDb = csiRsrqDb,
                csiSinrDb = null, // Not available via public API
                bandwidthMhz = null, // NR bandwidth not in public API before API 34
                mimoLayers = null,
            ),
            rat = "NR",
        )
    }

    private fun parseWcdma(ci: CellInfoWcdma): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength


        val uarfcn = id.uarfcn.validId()
        val band = getWcdmaBand(uarfcn)

        // Ec/No: signal quality metric for WCDMA (API 30+)
        val ecNo: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sig.ecNo.validSig()
        } else null

        return Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = ci.ageMs(),
                cellId = null,
                cid = id.cid.validId(),
                nci = null,
                lac = id.lac.validId(),
                tac = null,
                pci = null,
                psc = id.psc.validId(),
                bsic = null,
                band = band,
                arfcn = null,
                uarfcn = uarfcn,
                nrarfcn = null,
                rsrpDbm = sig.dbm.validSig(),  // RSCP — signal strength equivalent
                rsrqDb = ecNo,                  // Ec/No — quality equivalent
                sinrDb = ecNo,                  // Ec/No also serves as SINR proxy
                rssiDbm = null,                 // No separate RSSI in WCDMA public API
                cqi = null,
                asuLevel = sig.asuLevel.validSig(),
                dbm = sig.dbm.validSig(),
                timingAdvance = null,
                ssRsrpDbm = null,
                ssRsrqDb = null,
                ssSinrDb = null,
                csiRsrpDbm = null,
                csiRsrqDb = null,
                csiSinrDb = null,
                bandwidthMhz = null,            // WCDMA uses fixed 5 MHz channels
                mimoLayers = null,
            ),
            rat = "WCDMA",
        )
    }

    private fun getWcdmaBand(uarfcn: Int?): Int? {
        if (uarfcn == null) return null
        return when (uarfcn) {
            in 10562..10838 -> 1
            in 9662..9938, in 412..687, in 1012..1087, in 1112..1187 -> 2
            in 1162..1513 -> 3
            in 1537..1738, in 1312..1513 -> 4
            in 4357..4458, in 1007..1012 -> 5
            in 4387..4413, in 1012..1062 -> 6
            in 2237..2563, in 2587..2612 -> 7
            in 2937..3088, in 2712..2862 -> 8
            in 9237..9387 -> 9
            in 3112..3388 -> 10
            in 3712..3787 -> 11
            in 3837..3912 -> 12
            in 4037..4112 -> 13
            in 4137..4212 -> 14
            in 712..763 -> 19
            in 4512..4637 -> 20
            in 862..912 -> 21
            in 4662..5037 -> 22
            in 5112..5412 -> 25
            in 5737..5987 -> 26
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseTdscdma(ci: CellInfoTdscdma): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        return Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = ci.ageMs(),
                cellId = null,
                cid = id.cid.validId(),
                nci = null,
                lac = id.lac.validId(),
                tac = null,
                pci = null,
                psc = id.cpid.validId(),   // CPID plays same role as PSC
                bsic = null,
                band = null,               // No band API for TD-SCDMA
                arfcn = null,
                uarfcn = id.uarfcn.validId(),
                nrarfcn = null,
                rsrpDbm = sig.rscp.validSig(),   // RSCP: signal strength
                rsrqDb = null,
                sinrDb = null,
                rssiDbm = null,
                cqi = null,
                asuLevel = sig.asuLevel.validSig(),
                dbm = sig.dbm.validSig(),
                timingAdvance = null,
                ssRsrpDbm = null,
                ssRsrqDb = null,
                ssSinrDb = null,
                csiRsrpDbm = null,
                csiRsrqDb = null,
                csiSinrDb = null,
                bandwidthMhz = null,
                mimoLayers = null,
            ),
            rat = "TD-SCDMA",
        )
    }

    private fun parseGsm(ci: CellInfoGsm): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        val arfcn = id.arfcn.validId()
        val band = getGsmBand(arfcn)

        return Parsed(
            snapshot = CellRadioSnapshot(
                timestampOffsetMs = ci.ageMs(),
                cellId = null,
                cid = id.cid.validId(),
                nci = null,
                lac = id.lac.validId(),
                tac = null,
                pci = null,
                psc = null,
                bsic = id.bsic.validId(),
                band = band,
                arfcn = arfcn,   // GERAN ARFCN (API 24, always available at minSdk=Q)
                uarfcn = null,
                nrarfcn = null,
                rsrpDbm = null,                // LTE/NR-specific; no RSRP in GSM
                rsrqDb = null,
                sinrDb = null,
                rssiDbm = sig.dbm.validSig(),  // GSM signal level in dBm (closest to RSSI)
                cqi = null,
                asuLevel = sig.asuLevel.validSig(),
                dbm = sig.dbm.validSig(),
                timingAdvance = sig.timingAdvance.validSig(), // API 29, fine at minSdk=Q
                ssRsrpDbm = null,
                ssRsrqDb = null,
                ssSinrDb = null,
                csiRsrpDbm = null,
                csiRsrqDb = null,
                csiSinrDb = null,
                bandwidthMhz = null,           // GSM uses fixed 200 kHz channels
                mimoLayers = null,
            ),
            rat = "GSM",
        )
    }

    private fun getGsmBand(arfcn: Int?): Int? {
        if (arfcn == null) return null
        return when (arfcn) {
            in 1..124, 0, in 955..1023 -> 900  // P-GSM, E-GSM, R-GSM 900
            in 128..251 -> 850                 // GSM 850
            in 512..885 -> null                // Ambiguous: DCS 1800 or PCS 1900 by region
            in 259..293 -> 450                 // GSM 450
            in 306..340 -> 480                 // GSM 480
            in 438..511 -> 750                 // GSM 750
            else -> null
        }
    }

    private fun reflectInt(obj: Any?, methodName: String): Int? = runCatching {
        if (obj == null) return null
        obj.javaClass.getMethod(methodName).invoke(obj) as? Int
    }.getOrNull()

    private fun reflectIntList(obj: Any?, methodName: String): List<Int>? = runCatching {
        if (obj == null) return null
        when (val value = obj.javaClass.getMethod(methodName).invoke(obj)) {
            is IntArray -> value.toList()
            is List<*> -> value.filterIsInstance<Int>()
            else -> null
        }
    }.getOrNull()

    private fun reflectStaticInt(clazz: Class<*>, methodName: String): Int? = runCatching {
        clazz.getMethod(methodName).invoke(null) as? Int
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun networkTypeName(type: Int): String? = when (type) {
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
