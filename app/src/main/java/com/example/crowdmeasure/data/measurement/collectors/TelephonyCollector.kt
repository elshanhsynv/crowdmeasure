package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo as AndroidCellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.AvailabilityFlags
import com.example.crowdmeasure.domain.model.CellInfo
import com.example.crowdmeasure.domain.model.ServingCell
import com.example.crowdmeasure.domain.model.SignalInfo
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

        Log.d("TelephonyCollector", "cells=${infos.size}, registered=${infos.count { it.isRegistered }}")
        infos.forEach {
            Log.d("TelephonyCollector", "${it.javaClass.simpleName} dbm=${it.cellSignalStrength.dbm} reg=${it.isRegistered}")
        }

        val registered = infos.firstOrNull { it.isRegistered }

        val bestLte = infos.filterIsInstance<CellInfoLte>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val bestNr = infos.filterIsInstance<CellInfoNr>()
            .maxByOrNull { it.cellSignalStrength.dbm }

        val candidate = registered ?: bestLte ?: bestNr ?: infos.first()
        val parsed = parseRegisteredCell(candidate)

        return base.copy(
            registeredRat = parsed.rat,
            servingCell = parsed.servingCell,
            signal = parsed.signal,
            availability = parsed.flags.copy(cellInfoAccessible = true)
        )
    }

    private inline fun <T> safe(block: () -> T): T? =
        try { block() } catch (_: SecurityException) { null } catch (_: Throwable) { null }

    private data class Parsed(
        val servingCell: ServingCell?,
        val signal: SignalInfo?,
        val flags: AvailabilityFlags,
        val rat: String?
    )

    private fun parseRegisteredCell(ci: AndroidCellInfo?): Parsed {
        if (ci == null) {
            return Parsed(
                servingCell = null,
                signal = null,
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
                    flags = AvailabilityFlags(cellInfoAccessible = true),
                    rat = ci.javaClass.simpleName
                )
            }
        } catch (_: Throwable) {
            Parsed(
                servingCell = null,
                signal = null,
                flags = AvailabilityFlags(cellInfoAccessible = true),
                rat = ci.javaClass.simpleName
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig: CellSignalStrengthLte = ci.cellSignalStrength

        // Many identity fields come back as Int.MAX_VALUE when unknown
        fun Int.valid(): Int? = takeIf { it != Int.MAX_VALUE }

        // bands available only API 30+
        val band: Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) id.bands.firstOrNull() else null

        val serving = ServingCell(
            ci = id.ci.valid(),
            tac = id.tac.valid(),
            pci = id.pci.valid(),
            earfcn = id.earfcn.valid(),
            band = band
        )

        // Signal fields can be Int.MAX_VALUE when unknown. Also some OEMs gate these.
        fun Int.validSig(): Int? = takeIf { it != Int.MAX_VALUE }

        val signal = SignalInfo(
            rsrp = sig.rsrp.validSig(),
            rsrq = sig.rsrq.validSig(),
            rssi = sig.rssi.validSig(),
            sinr = sig.rssnr.validSig()
        )

        return Parsed(
            servingCell = serving,
            signal = signal,
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = serving.ci != null || serving.tac != null || serving.pci != null || serving.earfcn != null || serving.band != null,
                signalAccessible = signal.rsrp != null || signal.rsrq != null || signal.rssi != null || signal.sinr != null
            ),
            rat = "LTE"
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(ci: CellInfoNr): Parsed {
        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        fun Int.valid(): Int? = takeIf { it != Int.MAX_VALUE }
        fun Long.valid(): Long? = takeIf { it != Long.MAX_VALUE }

        val serving = ServingCell(
            nci = id?.nci?.valid(),
            tac = id?.tac?.valid(),
            pci = id?.pci?.valid(),
            nrarfcn = id?.nrarfcn?.valid()
        )

        val signal = SignalInfo(
            rsrp = sig?.ssRsrp?.valid(),
            rsrq = sig?.ssRsrq?.valid(),
            sinr = sig?.ssSinr?.valid(),
            rssi = null
        )

        return Parsed(
            servingCell = serving,
            signal = signal,
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = id != null,
                signalAccessible = sig != null
            ),
            rat = "NR"
        )
    }

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