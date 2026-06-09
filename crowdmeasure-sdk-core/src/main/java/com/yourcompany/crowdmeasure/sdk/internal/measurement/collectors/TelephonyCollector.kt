package com.yourcompany.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
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
import com.yourcompany.crowdmeasure.sdk.model.CarrierAggregationInfo
import com.yourcompany.crowdmeasure.sdk.model.CarrierInfo
import com.yourcompany.crowdmeasure.sdk.model.CellInfo
import com.yourcompany.crowdmeasure.sdk.model.CellRadioSnapshot
import com.yourcompany.crowdmeasure.sdk.model.NrState
import com.yourcompany.crowdmeasure.sdk.model.SecondaryCell
import timber.log.Timber

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
    fun collect(context: Context): CellInfo {
        val tm = context.getSystemService<TelephonyManager>()
            ?: return CellInfo(
                simCarriers = emptyList(),
                rat = null,
                nrState = NrState.NONE,
                serving = null,
                aggregation = null,
            )

        val sm = context.getSystemService<SubscriptionManager>()

        val op = tm.networkOperator.orEmpty()
        val phoneGranted = PlatformChecks.hasPhoneState(context)

        val dataType: Int? = if (phoneGranted) {
            try {
                tm.dataNetworkType
            } catch (e: SecurityException) {
                null
            }
        } else null

        val voiceType: Int? = if (phoneGranted) {
            try {
                tm.voiceNetworkType
            } catch (e: SecurityException) {
                null
            }
        } else null

        val serviceState = if (phoneGranted) {
            try {
                tm.serviceState
            } catch (e: SecurityException) {
                null
            }
        } else null

        val duplexModeString: String = when (serviceState?.duplexMode) {
            ServiceState.DUPLEX_MODE_FDD -> "FDD"
            ServiceState.DUPLEX_MODE_TDD -> "TDD"
            else -> ""
        }

        val carrier = CarrierInfo(
            carrierName = safe { tm.networkOperatorName },
            mcc = op.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op.takeIf { it.length >= 5 }?.substring(3),
            simOperatorId = tm.simOperator,
            simOperatorName = tm.simOperatorName,
            countryIso = safe { tm.simCountryIso },
            duplexMode = duplexModeString
        )
        val simCarriers = collectSimCarriers(tm, sm, phoneGranted)
        val collectedCarrier = simCarriers.collectedCarrier() ?: carrier
        val collectedSimCarriers = simCarriers.ifEmpty { listOf(collectedCarrier) }

        val base = CellInfo(
            simCarriers = collectedSimCarriers,
            collectedSubscriptionId = collectedCarrier.subscriptionId,
            collectedSimSlotIndex = collectedCarrier.simSlotIndex,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { tm.isNetworkRoaming },
            rat = null,
            nrState = NrState.NONE,
            serving = null,
            neighbors = emptyList(),
            aggregation = null,
        )

        val fineGranted = PlatformChecks.hasFineLocation(context)
        val locationOn = PlatformChecks.isLocationServicesEnabled(context)
        if (!fineGranted || !locationOn) return base

        val infos: List<AndroidCellInfo> = try {
            tm.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            return base
        } catch (_: Throwable) {
            return base
        }

        val nrState = deriveNrState(context, tm, dataType, infos)

        // Serving cell selection order:
        //  1. Registered cell (most authoritative — the OS has confirmed it)
        //  2. Best NR cell by dbm when NR is active (avoids reporting LTE when on 5G SA)
        //  3. Best LTE cell by dbm
        //  4. Best NR cell (fallback if no LTE either)
        //  5. First available cell (last resort)
        val registered = infos.firstOrNull { it.isRegistered }
        val bestNr = infos.filterIsInstance<CellInfoNr>()
            .maxByOrNull { it.cellSignalStrength.dbm }
        val bestLte = infos.filterIsInstance<CellInfoLte>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val candidate = registered
            ?: (if (nrState != NrState.NONE) bestNr else null)
            ?: bestLte
            ?: bestNr
            ?: infos.firstOrNull()

        val parsedServing = candidate?.let { parseCell(it) }
        val aggregation = candidate?.let { buildAggregation(infos, it) }
        val neighbors = infos
            .filterNot { it.isRegistered }
            .mapNotNull { parseCell(it).snapshot }

        return CellInfo(
            simCarriers = collectedSimCarriers,
            collectedSubscriptionId = collectedCarrier.subscriptionId,
            collectedSimSlotIndex = collectedCarrier.simSlotIndex,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { tm.isNetworkRoaming },
            rat = parsedServing?.rat,
            nrState = nrState,
            serving = parsedServing?.snapshot,
            neighbors = neighbors,
            aggregation = aggregation,
        )
    }

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (_: SecurityException) {
        null
    } catch (_: Throwable) {
        null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collectSimCarriers(
        tm: TelephonyManager,
        sm: SubscriptionManager?,
        phoneGranted: Boolean,
    ): List<CarrierInfo> {
        if (!phoneGranted || sm == null) return emptyList()

        val defaultDataSubId = safe { SubscriptionManager.getDefaultDataSubscriptionId() }.validSubId()
        val defaultVoiceSubId = safe { SubscriptionManager.getDefaultVoiceSubscriptionId() }.validSubId()
        val defaultSmsSubId = safe { SubscriptionManager.getDefaultSmsSubscriptionId() }.validSubId()
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

    private fun serviceStateDuplexMode(tm: TelephonyManager, phoneGranted: Boolean): String {
        if (!phoneGranted) return ""
        val serviceState = try {
            tm.serviceState
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }

        return when (serviceState?.duplexMode) {
            ServiceState.DUPLEX_MODE_FDD -> "FDD"
            ServiceState.DUPLEX_MODE_TDD -> "TDD"
            else -> ""
        }
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
    private fun AndroidCellInfo.ageMs(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (SystemClock.elapsedRealtime() - timestampMillis).coerceAtLeast(0L)
        } else {
            ((SystemClock.elapsedRealtimeNanos() - timeStamp) / 1_000_000L).coerceAtLeast(0L)
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        context: Context,
        tm: TelephonyManager,
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>,
    ): NrState {
        // Tier 1: TelephonyDisplayInfo (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayInfo = safe { getDisplayInfo(tm) }
            if (displayInfo != null) {
                return when (displayInfo.overrideNetworkType) {
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> NrState.NSA

                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> NrState.SA

                    else -> when (displayInfo.networkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> NrState.SA
                        else -> NrState.NONE
                    }
                }
            }
        }

        val hasNrCell = infos.any { it is CellInfoNr }
        return when (dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> if (hasNrCell) NrState.NSA else NrState.SA
            TelephonyManager.NETWORK_TYPE_LTE -> if (hasNrCell) NrState.NSA else NrState.NONE
            else -> if (hasNrCell) NrState.NSA else NrState.NONE
        }
    }

    /**
     * Reads the last-known [TelephonyDisplayInfo] from the TelephonyManager's
     * cached field. This is the only synchronous option for a one-shot collector.
     *
     * The cleaner alternative is maintaining a persistent
     * `TelephonyCallback.DisplayInfoListener` in a long-lived scope and passing
     * the cached value to [collect].
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getDisplayInfo(tm: TelephonyManager): TelephonyDisplayInfo? =
        runCatching {
            TelephonyManager::class.java
                .getDeclaredField("mTelephonyDisplayInfo")
                .also { it.isAccessible = true }
                .get(tm) as? TelephonyDisplayInfo
        }.getOrNull()

    private data class Parsed(
        val snapshot: CellRadioSnapshot?,
        val rat: String?,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(ci: AndroidCellInfo): Parsed = try {
        when (ci) {
            is CellInfoLte -> parseLte(ci)
            is CellInfoNr -> parseNr(ci)
            is CellInfoWcdma -> parseWcdma(ci)
            is CellInfoTdscdma -> parseTdscdma(ci)
            is CellInfoGsm -> parseGsm(ci)
            else -> Parsed(null, ci.javaClass.simpleName)
        }
    } catch (_: Throwable) {
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

        // NR CQI is @hide; try known method names defensively
        val nrCqi: Int? = (reflectInt(sig, "getCsiCqiReport")
            ?: reflectInt(sig, "getCqi"))?.validSig()

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

        Timber.tag("TelephonyCollector").d(id.cid.toString())

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
            in 512..885 -> 1800                // DCS 1800 (Note: 512..810 overlaps with PCS 1900)
            in 259..293 -> 450                 // GSM 450
            in 306..340 -> 480                 // GSM 480
            in 438..511 -> 750                 // GSM 750
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildAggregation(
        infos: List<AndroidCellInfo>,
        serving: AndroidCellInfo,
    ): CarrierAggregationInfo? {
        val secondaries = infos
            .filter { it !== serving }
            .filter { it is CellInfoLte || it is CellInfoNr }
            .mapNotNull { toSecondaryCell(it) }

        if (secondaries.isEmpty()) return null
        return CarrierAggregationInfo(
            active = null,
            secondaryCells = secondaries,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun toSecondaryCell(ci: AndroidCellInfo): SecondaryCell? = try {
        when (ci) {
            is CellInfoLte -> {
                val id = ci.cellIdentity
                val s = ci.cellSignalStrength
                val bwMhz = id.bandwidth.takeIf { it != Int.MAX_VALUE && it > 0 }?.let { it / 1000 }
                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id.bands.firstOrNull() else null,
                    earfcn = id.earfcn.validId(),
                    nrarfcn = null,
                    pci = id.pci.validId(),
                    rsrp = s.rsrp.validSig(),
                    rsrq = s.rsrq.validSig(),
                    sinr = s.rssnr.validSig(),
                    asuLevel = s.asuLevel.validSig(),
                    dbm = s.dbm.validSig(),
                    bandwidthMhz = bwMhz,
                )
            }

            is CellInfoNr -> {
                val id = ci.cellIdentity as? CellIdentityNr
                val s = ci.cellSignalStrength as? CellSignalStrengthNr
                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id?.bands?.firstOrNull() else null,
                    earfcn = null,
                    nrarfcn = id?.nrarfcn?.validId(),
                    pci = id?.pci?.validId(),
                    rsrp = s?.ssRsrp?.validSig(),
                    rsrq = s?.ssRsrq?.validSig(),
                    sinr = s?.ssSinr?.validSig(),
                    asuLevel = s?.asuLevel?.validSig(),
                    dbm = s?.dbm?.validSig(),
                    bandwidthMhz = null,
                )
            }

            else -> null
        }
    } catch (_: Throwable) {
        null
    }

    private fun reflectInt(obj: Any?, methodName: String): Int? = runCatching {
        if (obj == null) return null
        obj.javaClass.getMethod(methodName).invoke(obj) as? Int
    }.getOrNull()

    private fun reflectStaticInt(clazz: Class<*>, methodName: String): Int? = runCatching {
        clazz.getMethod(methodName).invoke(null) as? Int
    }.getOrNull()

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
