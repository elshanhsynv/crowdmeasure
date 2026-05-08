package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.Build
import android.os.SystemClock
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
import timber.log.Timber
import android.telephony.CellInfo as AndroidCellInfo

/**
 * Assumption: minSdk >= Q (29). The [RequiresApi] annotations on private
 * functions are kept for lint correctness; they are unreachable on < Q.
 */
object TelephonyCollector {

    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.Q)
    fun collect(context: Context): CellInfo {
        val tm = context.getSystemService<TelephonyManager>() ?: return CellInfo(
            carrier = CarrierInfo(null, null, null, null, null),
            rat = null,
            nrState = NrState.NONE,
            serving = null,
            aggregation = null
        )

//        TelephonyManager::class.java.methods
//            .filter { it.parameterCount == 0 && it.name.startsWith("get") }
//            .forEach { method ->
//                try {
//                    val value = method.invoke(tm)
//                    Timber.d("${method.name} = $value")
//                } catch (e: Exception) {
//                    Timber.d("${method.name} = <error: ${e.message}>")
//                }
//            }

        val op = tm.networkOperator.orEmpty()
        val phoneGranted = AppPermissions.hasPhoneState(context)

        val dataType: Int? = if (phoneGranted) safe { tm.dataNetworkType } else null
        val voiceType: Int? = if (phoneGranted) safe { tm.voiceNetworkType } else null


        val carrier = CarrierInfo(
            carrierName = safe { tm.networkOperatorName },
            mcc = op.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op.takeIf { it.length >= 5 }?.substring(3),
            operatorId = op.takeIf { it.isNotEmpty() },
            countryIso = safe { tm.simCountryIso },
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
            return base
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

        val neighbors = infos.filter { it !== candidate }
            .mapNotNull { parseCell(it).snapshot }

        return CellInfo(
            carrier = carrier,
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

    private fun Int.validId(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE && it >= 0 }

    private fun Long.validId(): Long? =
        takeIf { it != Long.MAX_VALUE && it != Long.MIN_VALUE && it >= 0 }

    private fun Int.validSig(): Int? =
        takeIf { it != Int.MAX_VALUE && it != Int.MIN_VALUE }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deriveNrState(
        context: Context,
        tm: TelephonyManager,
        dataNetworkType: Int?,
        infos: List<AndroidCellInfo>,
    ): NrState {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayInfo = safe { getDisplayInfo(context, tm) }
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
        return when {
            dataNetworkType == TelephonyManager.NETWORK_TYPE_NR && !hasNrCell -> NrState.SA
            dataNetworkType == TelephonyManager.NETWORK_TYPE_NR -> NrState.NSA
            dataNetworkType == TelephonyManager.NETWORK_TYPE_LTE && hasNrCell -> NrState.NSA
            else -> NrState.NONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getDisplayInfo(context: Context, tm: TelephonyManager): TelephonyDisplayInfo? =
        runCatching {
            val field = TelephonyManager::class.java
                .getDeclaredField("mTelephonyDisplayInfo")
                .also { it.isAccessible = true }
            field.get(tm) as? TelephonyDisplayInfo
        }.getOrNull()

    private data class Parsed(
        val snapshot: CellRadioSnapshot?,
        val rat: String?,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseCell(ci: AndroidCellInfo): Parsed {
        return try {
            when (ci) {
                is CellInfoLte -> parseLte(ci)
                is CellInfoNr -> parseNr(ci)
                else -> Parsed(null, ci.javaClass.simpleName)
            }
        } catch (_: Throwable) {
            Parsed(null, ci.javaClass.simpleName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig = ci.cellSignalStrength

        Timber.tag("TelephonyCollector").d("LTE: %s", sig.timingAdvance)

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id.bands.firstOrNull()
        } else null

        val cqi: Int? = reflectInt(sig, "getCqi")?.validSig()

        // Calculate offset from when this cell info was captured vs now
        val offsetNs = SystemClock.elapsedRealtimeNanos() - ci.timeStamp
        val offsetMs = offsetNs / 1_000_000L

        // Bandwidth is reported in kHz. Convert to MHz.
        val bandwidthMhz = id.bandwidth.takeIf { it != Int.MAX_VALUE }?.let { it / 1000 }

        val snapshot = CellRadioSnapshot(
            timestampOffsetMs = offsetMs,
            cellId = id.ci.validId(),
            nci = null,
            band = band,
            arfcn = id.earfcn.validId(),
            nrarfcn = null,
            tac = id.tac.validId(),
            pci = id.pci.validId(),
            rsrpDbm = sig.rsrp.validSig(),
            rsrqDb = sig.rsrq.validSig(),
            sinrDb = sig.rssnr.validSig(),
            cqi = cqi,
            rssiDbm = sig.rssi.validSig(),
            bandwidthMhz = bandwidthMhz,
            mimoLayers = null,
            asuLevel = sig.asuLevel.validSig(),
            dbm = sig.dbm.validSig(),
            timingAdvance = sig.timingAdvance.validSig(),
            ssRsrpDbm = null,
            ssRsrqDb = null,
            ssSinrDb = null,
            csiRsrpDbm = null,
            csiRsrqDb = null,
            csiSinrDb = null,
            cid = null,
            lac = null,
            psc = null,
            bsic = null,
            uarfcn = null,
        )

        return Parsed(snapshot, "LTE")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(ci: CellInfoNr): Parsed {
        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        val band: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            id?.bands?.firstOrNull()
        } else null

        val nrCqi: Int? = (reflectInt(sig, "getCsiCqiReport")
            ?: reflectInt(sig, "getCqi"))?.validSig()

        val offsetNs = SystemClock.elapsedRealtimeNanos() - ci.timeStamp
        val offsetMs = offsetNs / 1_000_000L

        val snapshot = CellRadioSnapshot(
            timestampOffsetMs = offsetMs,
            cellId = null,
            nci = id?.nci?.validId(),
            band = band,
            arfcn = null,
            nrarfcn = id?.nrarfcn?.validId(),
            tac = id?.tac?.validId(),
            pci = id?.pci?.validId(),
            rsrpDbm = sig?.ssRsrp?.validSig(),
            rsrqDb = sig?.ssRsrq?.validSig(),
            sinrDb = sig?.ssSinr?.validSig(),
            cqi = nrCqi,
            rssiDbm = null, // NR API doesn't expose standard RSSI in the same way
            bandwidthMhz = null, // No standard bandwidth property on NR Identity prior to Android 14
            mimoLayers = null,
            dbm = null,
            timingAdvance = null,
            ssRsrpDbm = null,
            ssRsrqDb = null,
            ssSinrDb = null,
            csiRsrpDbm = null,
            csiRsrqDb = null,
            csiSinrDb = null,
            cid = null,
            lac = null,
            psc = null,
            bsic = null,
            uarfcn = null,
            asuLevel = null
        )

        return Parsed(snapshot, "NR")
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
            active = null, // Requires active tracking via TelephonyCallback.DisplayInfoListener
            secondaryCells = secondaries,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun toSecondaryCell(ci: AndroidCellInfo): SecondaryCell? = try {
        when (ci) {
            is CellInfoLte -> {
                val id = ci.cellIdentity
                val s = ci.cellSignalStrength
                val bwMhz = id.bandwidth.takeIf { it != Int.MAX_VALUE }?.let { it / 1000 }

                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id.bands.firstOrNull() else null,
                    earfcn = id.earfcn.validId(),
                    nrarfcn = null,
                    pci = id.pci.validId(),
                    rsrp = s.rsrp.validSig(),
                    rsrq = s.rsrq.validSig(),
                    sinr = s.rssnr.validSig(),
                    bandwidthMhz = bwMhz
                )
            }

            is CellInfoNr -> {
                val id = ci.cellIdentity as? CellIdentityNr
                val s = ci.cellSignalStrength as? CellSignalStrengthNr
                SecondaryCell(
                    band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id?.bands?.firstOrNull() else null,
                    earfcn = null,
                    nrarfcn = id?.nrarfcn?.toLong()?.validId(), // model asks for Long
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

    private fun reflectInt(obj: Any?, methodName: String): Int? = runCatching {
        if (obj == null) return null
        obj.javaClass.getMethod(methodName).invoke(obj) as? Int
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
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> null
        else -> null
    }
}