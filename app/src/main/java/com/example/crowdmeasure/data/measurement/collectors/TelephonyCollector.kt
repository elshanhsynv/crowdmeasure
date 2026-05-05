package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.CarrierAggregationInfo
import com.example.crowdmeasure.domain.model.CarrierInfo
import com.example.crowdmeasure.domain.model.CellInfo
import com.example.crowdmeasure.domain.model.CellRadioSnapshot
import com.example.crowdmeasure.domain.model.NrState
import com.example.crowdmeasure.domain.model.SecondaryCell
import com.example.crowdmeasure.presentation.util.AppPermissions
import android.telephony.CellInfo as AndroidCellInfo

/**
 * Assumption: minSdk >= Q (29). The [RequiresApi] annotations on private
 * functions are kept for lint correctness; they are unreachable on < Q.
 */
object TelephonyCollector {

    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.Q)
    fun collect(context: Context): CellInfo {
        val tm = context.getSystemService<TelephonyManager>() ?: return CellInfo()

        val op = tm.networkOperator.orEmpty()
        val phoneGranted = AppPermissions.hasPhoneState(context)

        val dataType: Int? = if (phoneGranted) safe { tm.dataNetworkType } else null
        val voiceType: Int? = if (phoneGranted) safe { tm.voiceNetworkType } else null

        val carrier = CarrierInfo(
            carrierName = safe { tm.networkOperatorName },
            mcc = op.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op.takeIf { it.length >= 5 }?.substring(3)
        )

        val base = CellInfo(
            carrier = carrier,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { tm.isNetworkRoaming },
            rat = null,
            nrState = NrState.NONE,
            serving = null,
            neighbors = emptyList(),
            aggregation = null,
        )

        val fineGranted = AppPermissions.hasFineLocation(context)
        val locationOn = AppPermissions.isLocationServicesEnabled(context)

        if (!fineGranted || !locationOn) {
            return base   // availability defaults to all-false
        }

        val infos: List<AndroidCellInfo> = try {
            tm.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            return base
        } catch (_: Throwable) {
            return base
        }

        val nrState = deriveNrState(context, tm, dataType, infos)

        // Serving cell selection:
        //  1. Registered cell (most authoritative)
        //  2. Best NR cell by signal (preferred over LTE when data is on NR)
        //  3. Best LTE cell by signal
        //  4. First available cell (last resort)
        val registered = infos.firstOrNull { it.isRegistered }
        val bestNr = infos.filterIsInstance<CellInfoNr>()
            .maxByOrNull { it.cellSignalStrength.dbm }
        val bestLte = infos.filterIsInstance<CellInfoLte>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val candidate = registered
            ?: (if (nrState != NrState.NONE) bestNr else null)
            ?: bestLte
            ?: bestNr
            ?: infos.first()

        val parsed = parseCell(candidate)
        val aggregation = buildAggregation(infos, candidate)

        val cellInfo = CellInfo(
            carrier = carrier,
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { tm.isNetworkRoaming },
            rat = parsed.rat,
            nrState = nrState,
            serving = parsed.servingCell,
            neighbors = parsed.neighbors ?: emptyList(),
            aggregation = aggregation,
        )
        return cellInfo
    }

    // -------------------------------------------------------------------------
    // Safe accessors
    // -------------------------------------------------------------------------

    private inline fun <T> safe(block: () -> T): T? = try {
        block()
    } catch (_: SecurityException) {
        null
    } catch (_: Throwable) {
        null
    }

    private fun Int.validId(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE && it >= 0 }

    private fun Long.validId(): Long? =
        takeIf { it != Long.MAX_VALUE && it != Long.MIN_VALUE && it >= 0 }

    private fun Int.validSig(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    // -------------------------------------------------------------------------
    // NR state derivation
    // -------------------------------------------------------------------------

    /**
     * Derives the NR operating mode with a two-tier approach:
     *
     * **Tier 1 — [TelephonyDisplayInfo] (API 30+, preferred):**
     * `overrideNetworkType` distinguishes NSA (`NR_NSA*`) from SA (`NR_ADVANCED` or
     * plain `NR` when `dataNetworkType == NETWORK_TYPE_NR` without an LTE anchor).
     *
     * **Tier 2 — Heuristic fallback (API < 30 or DisplayInfo unavailable):**
     *  - `dataNetworkType == NR` + visible NR cell → [NrState.NSA] (conservative;
     *    we cannot confirm SA without DisplayInfo).
     *  - `dataNetworkType == NR` + no NR cell in scan → [NrState.SA] (NR scan might
     *    have been suppressed, but data type confirms NR).
     *  - `dataNetworkType == LTE` + visible NR cell → [NrState.NSA].
     *  - Anything else → [NrState.NONE].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        context: Context,
        tm: TelephonyManager,
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>,
    ): NrState {
        // Tier 1: TelephonyDisplayInfo (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayInfo = safe { getDisplayInfo(context, tm) }
            if (displayInfo != null) {
                return when (displayInfo.overrideNetworkType) {
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
                        -> NrState.NSA

                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
                        -> NrState.SA

                    else -> when (displayInfo.networkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> NrState.SA
                        else -> NrState.NONE
                    }
                }
            }
        }

        // Tier 2: Heuristic fallback
        val hasNrCell = infos.any { it is CellInfoNr }
        return when {
            dataNetworkType == TelephonyManager.NETWORK_TYPE_NR && !hasNrCell -> NrState.SA
            dataNetworkType == TelephonyManager.NETWORK_TYPE_NR -> NrState.NSA
            dataNetworkType == TelephonyManager.NETWORK_TYPE_LTE && hasNrCell -> NrState.NSA
            else -> NrState.NONE
        }
    }

    /**
     * [TelephonyManager.listen] / [TelephonyManager.registerTelephonyCallback] are
     * async. For a one-shot collector we read the last-known [TelephonyDisplayInfo]
     * synchronously via reflection on the TelephonyManager's cached value.
     * This is best-effort: returns null if unavailable.
     *
     * **Alternative**: callers that already maintain a persistent
     * `TelephonyCallback.DisplayInfoListener` can pass the cached value in directly.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getDisplayInfo(context: Context, tm: TelephonyManager): TelephonyDisplayInfo? =
        runCatching {
            // mTelephonyDisplayInfo is cached by the framework after first registration.
            val field = TelephonyManager::class.java
                .getDeclaredField("mTelephonyDisplayInfo")
                .also { it.isAccessible = true }
            field.get(tm) as? TelephonyDisplayInfo
        }.getOrNull()

    // -------------------------------------------------------------------------
    // Cell parsing dispatch
    // -------------------------------------------------------------------------

    private data class Parsed(
        val servingCell: CellRadioSnapshot?,
        val rat: String?,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(ci: AndroidCellInfo?): Parsed {
        if (ci == null) return emptyParsed()
        return try {
            when (ci) {
                is CellInfoLte -> parseLte(ci)
                is CellInfoNr -> parseNr(ci)
                else -> emptyParsed(rat = ci.javaClass.simpleName)
            }
        } catch (_: Throwable) {
            emptyParsed(rat = ci.javaClass.simpleName)
        }
    }

    private fun emptyParsed(rat: String? = null) = Parsed(
        servingCell = null,
        rat = rat,
    )

    // -------------------------------------------------------------------------
    // LTE
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id.bands.firstOrNull()
        } else null

        val timingAdvance: Int? = safe { sig.timingAdvance }?.validSig()

        // getCqi() is @hide; best-effort via reflection. Returns null safely when absent.
        val cqi: Int? = reflectInt(sig, "getCqi")?.validSig()

        val serving = CellRadioSnapshot(
            cellId = id.ci.validId(),
            tac = id.tac.validId(),
            pci = id.pci.validId(),
            arfcn = id.earfcn.validId(),
            band = band,
        )

        val signal = SignalInfo(
            rsrp = sig.rsrp.validSig(),
            rsrq = sig.rsrq.validSig(),
            rssi = sig.rssi.validSig(),
            sinr = sig.rssnr.validSig(),
            cqi = cqi,
            timingAdvance = timingAdvance,
        )

        return Parsed(
            servingCell = serving,
            signal = signal,
            radioMetrics = RadioMetrics(lteCqi = cqi),
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = serving.ci != null || serving.tac != null
                        || serving.pci != null || serving.earfcn != null || serving.band != null,
                signalAccessible = signal.rsrp != null || signal.rsrq != null
                        || signal.rssi != null || signal.sinr != null
                        || signal.cqi != null || signal.timingAdvance != null,
            ),
            rat = "LTE",
        )
    }

    // -------------------------------------------------------------------------
    // NR
    // -------------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(ci: CellInfoNr): Parsed {
        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id?.bands?.firstOrNull()
        } else null

        // NR CQI is not a public API; try known @hide method names defensively.
        val nrCqi: Int? = (reflectInt(sig, "getCsiCqiReport")
            ?: reflectInt(sig, "getCqi"))?.validSig()

        val serving = ServingCell(
            nci = id?.nci?.validId(),
            tac = id?.tac?.validId(),
            pci = id?.pci?.validId(),
            // nrarfcn is Long in our model; the OS API returns Int but values can be large.
            nrarfcn = id?.nrarfcn?.toLong()?.validId(),
            band = band,
        )

        val signal = SignalInfo(
            rsrp = sig?.ssRsrp?.validSig(),
            rsrq = sig?.ssRsrq?.validSig(),
            sinr = sig?.ssSinr?.validSig(),
        )

        return Parsed(
            servingCell = serving,
            signal = signal,
            radioMetrics = RadioMetrics(nrCqi = nrCqi),
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = id != null && (serving.nci != null || serving.tac != null
                        || serving.pci != null || serving.nrarfcn != null || serving.band != null),
                signalAccessible = sig != null && (signal.rsrp != null || signal.rsrq != null
                        || signal.sinr != null || nrCqi != null),
            ),
            rat = "NR",
        )
    }

    // -------------------------------------------------------------------------
    // Carrier aggregation (best-effort)
    // -------------------------------------------------------------------------

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

        // `active` is genuinely unknowable from public API.
        // Seeing secondary cells is necessary but not sufficient for CA confirmation.
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
                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id.bands.firstOrNull() else null,
                    earfcn = id.earfcn.validId(),
                    pci = id.pci.validId(),
                    rsrp = s.rsrp.validSig(),
                    rsrq = s.rsrq.validSig(),
                    sinr = s.rssnr.validSig(),
                )
            }

            is CellInfoNr -> {
                val id = ci.cellIdentity as? CellIdentityNr
                val s = ci.cellSignalStrength as? CellSignalStrengthNr
                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id?.bands?.firstOrNull() else null,
                    nrarfcn = id?.nrarfcn?.toLong()?.validId(),
                    pci = id?.pci?.validId(),
                    rsrp = s?.ssRsrp?.validSig(),
                    rsrq = s?.ssRsrq?.validSig(),
                    sinr = s?.ssSinr?.validSig(),
                )
            }

            else -> null
        }
    } catch (_: Throwable) {
        null
    }

    // -------------------------------------------------------------------------
    // Reflection helper (for @hide APIs; fails safely)
    // -------------------------------------------------------------------------

    private fun reflectInt(obj: Any?, methodName: String): Int? = runCatching {
        if (obj == null) return null
        obj.javaClass.getMethod(methodName).invoke(obj) as? Int
    }.getOrNull()

    // -------------------------------------------------------------------------
    // Network type mapping
    // -------------------------------------------------------------------------

    /**
     * Returns a stable string identifier for known network types.
     * Unknown types return null — embedding a raw int in a serialized payload
     * ("UNKNOWN(35)") makes server-side parsing fragile.
     */
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
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> null
        else -> null
    }
}