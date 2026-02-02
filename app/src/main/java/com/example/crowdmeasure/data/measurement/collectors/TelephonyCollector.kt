package com.example.crowdmeasure.data.measurement.collectors

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo as AndroidCellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.AvailabilityFlags
import com.example.crowdmeasure.domain.model.CellInfo
import com.example.crowdmeasure.domain.model.ServingCell
import com.example.crowdmeasure.domain.model.SignalInfo

object TelephonyCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun collect(context: Context): CellInfo {
        val tm = context.getSystemService<TelephonyManager>()

        val op = tm?.networkOperator
        val base = CellInfo(
            carrierName = tm?.networkOperatorName,
            mcc = op?.takeIf { it.length >= 3 }?.substring(0, 3),
            mnc = op?.takeIf { it.length >= 5 }?.substring(3),
            dataNetworkType = tm?.dataNetworkType?.let(::networkTypeName),
            voiceNetworkType = tm?.voiceNetworkType?.let(::networkTypeName),
            roaming = tm?.isNetworkRoaming
        )

        // allCellInfo often requires location permission; handle gracefully.
        val infos: List<AndroidCellInfo> = try {
            tm?.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            return base.copy(availability = AvailabilityFlags(cellInfoAccessible = false))
        } catch (_: Throwable) {
            return base.copy(availability = AvailabilityFlags(cellInfoAccessible = false))
        }

        val registered = infos.firstOrNull { it.isRegistered }

        val (servingCell, signal, flags, rat) = parseRegisteredCell(registered)

        return base.copy(
            registeredRat = rat,
            servingCell = servingCell,
            signal = signal,
            availability = flags
        )
    }

    private data class Parsed(
        val servingCell: ServingCell?,
        val signal: SignalInfo?,
        val flags: AvailabilityFlags,
        val rat: String?
    )

    @RequiresApi(Build.VERSION_CODES.Q)
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
            when (ci) {
                is CellInfoLte -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    parseLte(ci)
                } else {
                    TODO("VERSION.SDK_INT < R")
                }

                is CellInfoNr -> parseNr(ci)
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun parseLte(ci: CellInfoLte): Parsed {
        val id = ci.cellIdentity
        val sig: CellSignalStrengthLte = ci.cellSignalStrength

        // Android telephony uses Int.MAX_VALUE for many "unknown" LTE identity fields.
        fun Int.takeIfValidIntMax(): Int? = takeIf { it != Int.MAX_VALUE }

        val serving = ServingCell(
            ci = id.ci.takeIfValidIntMax(),
            tac = id.tac.takeIfValidIntMax(),
            pci = id.pci.takeIfValidIntMax(),
            earfcn =
                id.earfcn.takeIfValidIntMax(),
            band =
                id.bands.firstOrNull()
        )

        fun Int.takeIfValidSignal(): Int? = takeIf { it != CellSignalStrengthLte.SIGNAL_STRENGTH_NONE_OR_UNKNOWN && it != Int.MAX_VALUE }

        val signal = SignalInfo(
            rsrp = sig.rsrp.takeIf { it != Int.MAX_VALUE },
            rsrq = sig.rsrq.takeIf { it != Int.MAX_VALUE },
            rssi = sig.rssi.takeIf { it != Int.MAX_VALUE },
            sinr = sig.rssnr.takeIf { it != Int.MAX_VALUE }
        )

        return Parsed(
            servingCell = serving,
            signal = signal,
            flags = AvailabilityFlags(
                cellInfoAccessible = true,
                idsAccessible = serving != ServingCell(),
                signalAccessible = signal != SignalInfo()
            ),
            rat = "LTE"
        )
    }

    private fun parseNr(ci: CellInfoNr): Parsed {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Parsed(
                servingCell = null,
                signal = null,
                flags = AvailabilityFlags(cellInfoAccessible = true),
                rat = "NR"
            )
        }

        val id = ci.cellIdentity as? CellIdentityNr
        val sig = ci.cellSignalStrength as? CellSignalStrengthNr

        fun Int.takeIfValidIntMax(): Int? = takeIf { it != Int.MAX_VALUE }
        fun Long.takeIfValidLongMax(): Long? = takeIf { it != Long.MAX_VALUE }

        val serving = ServingCell(
            nci = id?.nci?.takeIfValidLongMax(),
            tac = id?.tac?.takeIfValidIntMax(),
            pci = id?.pci?.takeIfValidIntMax(),
            nrarfcn = id?.nrarfcn?.takeIfValidIntMax()
        )

        val signal = SignalInfo(
            rsrp = sig?.ssRsrp?.takeIf { it != Int.MAX_VALUE },
            rsrq = sig?.ssRsrq?.takeIf { it != Int.MAX_VALUE },
            sinr = sig?.ssSinr?.takeIf { it != Int.MAX_VALUE },
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