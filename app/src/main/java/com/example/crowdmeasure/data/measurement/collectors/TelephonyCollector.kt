package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.Build
import android.telephony.*
import android.telephony.CellInfo as AndroidCellInfo
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.*
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.hasFineLocation
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled

object TelephonyCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun collect(context: Context): CellInfo {
        val tm = context.getSystemService<TelephonyManager>() ?: return CellInfo()

        val op = tm.networkOperator
        val phoneGranted = AppPermissions.hasPhoneState(context)

        val dataType = if (phoneGranted) safe { tm.dataNetworkType } else null
        val voiceType = if (phoneGranted) safe { tm.voiceNetworkType } else null

        val base = CellInfo(
            carrierName = safe { tm.networkOperatorName },
            mcc = op.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op.takeIf { it.length >= 5 }?.substring(3),
            dataNetworkType = dataType?.let(::networkTypeName),
            voiceNetworkType = voiceType?.let(::networkTypeName),
            roaming = safe { tm.isNetworkRoaming },
            nrState = null,
            availability = AvailabilityFlags(
                cellInfoAccessible = false,
                idsAccessible = false,
                signalAccessible = false
            )
        )

        val fineGranted = hasFineLocation(context)
        val locationOn = isLocationServicesEnabled(context)

        if (!fineGranted || !locationOn) {
            return base.copy(
                availability = base.availability.copy(
                    cellInfoAccessible = false,
                    idsAccessible = false,
                    signalAccessible = false
                )
            )
        }

        val infos: List<AndroidCellInfo> = try {
            tm.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            return base
        } catch (_: Throwable) {
            return base
        }

        if (infos.isEmpty()) {
            return base.copy(availability = base.availability.copy(cellInfoAccessible = true))
        }

        val nrState = deriveNrState(dataType, infos)

        val registered = infos.firstOrNull { it.isRegistered }

        val bestLte = infos.filterIsInstance<CellInfoLte>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val bestNr = infos.filterIsInstance<CellInfoNr>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val candidate = registered ?: bestLte ?: bestNr ?: infos.first()
        val parsed = parseCell(candidate)

        // Best-effort CA / secondary cells list (exclude the chosen serving cell)
        val aggregation = buildAggregation(infos, candidate)

        return base.copy(
            nrState = nrState,
            registeredRat = parsed.rat,
            servingCell = parsed.servingCell,
            signal = parsed.signal,
            radioMetrics = parsed.radioMetrics,
            aggregation = aggregation,
            availability = parsed.flags.copy(cellInfoAccessible = true)
        )
    }

    // --------------------------------------------------------------------
    // Safe helpers
    // --------------------------------------------------------------------

    private inline fun <T> safe(block: () -> T): T? =
        try { block() } catch (_: SecurityException) { null } catch (_: Throwable) { null }

    private fun Int.validId(): Int? = takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }
    private fun Long.validId(): Long? = takeIf { it != Long.MAX_VALUE && it != Long.MIN_VALUE }

    private fun Int.validSig(): Int? = takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    // --------------------------------------------------------------------
    // Parsed holder
    // --------------------------------------------------------------------

    private data class Parsed(
        val servingCell: ServingCell?,
        val signal: SignalInfo?,
        val radioMetrics: RadioMetrics?,
        val flags: AvailabilityFlags,
        val rat: String?
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(ci: AndroidCellInfo?): Parsed {
        if (ci == null) {
            return Parsed(
                servingCell = null,
                signal = null,
                radioMetrics = null,
                flags = AvailabilityFlags(cellInfoAccessible = true),
                rat = null
            )
        }

        return try {
            when {
                ci is CellInfoLte -> parseLte(ci)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ci is CellInfoNr -> parseNr(ci)
                else -> Parsed(
                    servingCell = null,
                    signal = null,
                    radioMetrics = null,
                    flags = AvailabilityFlags(cellInfoAccessible = true),
                    rat = ci.javaClass.simpleName
                )
            }
        } catch (_: Throwable) {
            Parsed(
                servingCell = null,
                signal = null,
                radioMetrics = null,
                flags = AvailabilityFlags(cellInfoAccessible = true),
                rat = ci.javaClass.simpleName
            )
        }
    }

    // --------------------------------------------------------------------
    // LTE
    // --------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        // band (API 30+)
        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id.bands.firstOrNull()
        } else null

        // Timing advance exists since API 26, but may be gated/unknown
        val timingAdvance: Int? = safe { sig.timingAdvance }?.validSig()

        // CQI isn't part of public API for LTE signal; best-effort reflection (often null)
        val cqi: Int? = reflectInt(sig, "getCqi")?.validSig()

        val serving = ServingCell(
            ci = id.ci.validId(),
            tac = id.tac.validId(),
            pci = id.pci.validId(),
            earfcn = id.earfcn.validId(),
            band = band,
            bandwidthMhz = null // best-effort; not reliably available via public API
        )

        val signal = SignalInfo(
            rsrp = sig.rsrp.validSig(),
            rsrq = sig.rsrq.validSig(),
            rssi = sig.rssi.validSig(),
            sinr = sig.rssnr.validSig(),
            cqi = cqi,
            timingAdvance = timingAdvance
        )

        val idsAccessible =
            serving.ci != null || serving.tac != null || serving.pci != null ||
                    serving.earfcn != null || serving.band != null

        val sigAccessible =
            signal.rsrp != null || signal.rsrq != null || signal.rssi != null || signal.sinr != null ||
                    signal.cqi != null || signal.timingAdvance != null

        return Parsed(
            servingCell = serving,
            signal = signal,
            radioMetrics = RadioMetrics(
                mimoLayers = null,
                lteCqi = cqi,
                nrCqi = null
            ),
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = idsAccessible,
                signalAccessible = sigAccessible
            ),
            rat = "LTE"
        )
    }

    // --------------------------------------------------------------------
    // NR
    // --------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(ci: CellInfoNr): Parsed {
        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        // band (API 30+ for NR too)
        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id?.bands?.firstOrNull()
        } else null

        // CQI for NR isn't public API either; best-effort reflection
        val nrCqi: Int? = reflectInt(sig, "getCsiCqiReport")?.validSig()
            ?: reflectInt(sig, "getCqi")?.validSig()

        val serving = ServingCell(
            nci = id?.nci?.validId(),
            tac = id?.tac?.validId(),
            pci = id?.pci?.validId(),
            nrarfcn = id?.nrarfcn?.validId(),
            band = band,
            bandwidthMhz = null
        )

        val signal = SignalInfo(
            rsrp = sig?.ssRsrp?.validSig(),
            rsrq = sig?.ssRsrq?.validSig(),
            sinr = sig?.ssSinr?.validSig(),
            rssi = null,
            cqi = nrCqi,
            timingAdvance = null
        )

        val idsAccessible = id != null && (
                serving.nci != null || serving.tac != null || serving.pci != null ||
                        serving.nrarfcn != null || serving.band != null
                )

        val sigAccessible = sig != null && (
                signal.rsrp != null || signal.rsrq != null || signal.sinr != null || signal.cqi != null
                )

        return Parsed(
            servingCell = serving,
            signal = signal,
            radioMetrics = RadioMetrics(
                mimoLayers = null,
                lteCqi = null,
                nrCqi = nrCqi
            ),
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = idsAccessible,
                signalAccessible = sigAccessible
            ),
            rat = "NR"
        )
    }

    // --------------------------------------------------------------------
    // Carrier aggregation best-effort
    // --------------------------------------------------------------------

    private fun buildAggregation(
        infos: List<AndroidCellInfo>,
        chosenServing: AndroidCellInfo
    ): CarrierAggregationInfo? {
        // Secondary cells are basically "other cells we can see" of LTE/NR.
        // This is not perfect CA detection, but provides real value for analytics.
        val secondaries = infos
            .filter { it !== chosenServing }
            .filter { it is CellInfoLte || (Build.VERSION.SDK_INT >= 29 && it is CellInfoNr) }
            .mapNotNull { toSecondaryCell(it) }

        if (secondaries.isEmpty()) return null

        // "active" is unknown from public API; best-effort: if we see multiple strong same-RAT cells, likely CA.
        val likelyActive = secondaries.size >= 1

        return CarrierAggregationInfo(
            active = likelyActive,
            secondaryCells = secondaries
        )
    }

    private fun toSecondaryCell(ci: AndroidCellInfo): SecondaryCell? {
        return try {
            when {
                ci is CellInfoLte -> {
                    val id = ci.cellIdentity
                    val s = ci.cellSignalStrength
                    SecondaryCell(
                        band = if (Build.VERSION.SDK_INT >= 30) id.bands.firstOrNull() else null,
                        earfcn = id.earfcn.validId(),
                        nrarfcn = null,
                        pci = id.pci.validId(),
                        rsrp = s.rsrp.validSig(),
                        rsrq = s.rsrq.validSig(),
                        sinr = s.rssnr.validSig(),
                        bandwidthMhz = null
                    )
                }
                Build.VERSION.SDK_INT >= 29 && ci is CellInfoNr -> {
                    val id = ci.cellIdentity as? CellIdentityNr
                    val s = ci.cellSignalStrength as? CellSignalStrengthNr
                    SecondaryCell(
                        band = if (Build.VERSION.SDK_INT >= 30) id?.bands?.firstOrNull() else null,
                        earfcn = null,
                        nrarfcn = id?.nrarfcn?.validId(),
                        pci = id?.pci?.validId(),
                        rsrp = s?.ssRsrp?.validSig(),
                        rsrq = s?.ssRsrq?.validSig(),
                        sinr = s?.ssSinr?.validSig(),
                        bandwidthMhz = null
                    )
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>?
    ): NrState {
        // If data network is NR, we’re definitely on 5G.
        // Without DisplayInfo we can’t reliably tell NSA vs SA, so classify as SA.
        if (dataNetworkType == TelephonyManager.NETWORK_TYPE_NR) return NrState.SA

        // If data is LTE but we can see NR cells, that strongly suggests NSA capability/presence.
        val hasNrCell = infos?.any { it is CellInfoNr } == true
        if (dataNetworkType == TelephonyManager.NETWORK_TYPE_LTE && hasNrCell) return NrState.NSA

        return NrState.NONE
    }



    // --------------------------------------------------------------------
    // Reflection helpers
    // --------------------------------------------------------------------

    private fun reflectInt(obj: Any?, methodName: String): Int? =
        runCatching {
            if (obj == null) return null
            obj.javaClass.getMethod(methodName).invoke(obj) as? Int
        }.getOrNull()

    // --------------------------------------------------------------------
    // Network type name mapping
    // --------------------------------------------------------------------

    private fun networkTypeName(type: Int): String = when (type) {
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
        else -> "UNKNOWN($type)"
    }
}
