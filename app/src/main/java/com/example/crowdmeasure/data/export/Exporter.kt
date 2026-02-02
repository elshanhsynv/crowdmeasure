package com.example.crowdmeasure.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.crowdmeasure.domain.model.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Exporter(
    private val context: Context
) {
    suspend fun exportMeasurementsToJson(
        measurements: List<Measurement>,
        filePrefix: String = "crowdmeasure_export",
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "${filePrefix}_$ts.json")

            val root = JSONObject().apply {
                put("schema_version", 1)
                put("exported_at_utc_ms", System.currentTimeMillis())
                put("count", measurements.size)
                put("measurements", JSONArray().apply {
                    measurements.forEach { put(measurementToJson(it)) }
                })
            }

            file.writeText(root.toString(2))

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    private fun measurementToJson(m: Measurement): JSONObject {
        val header = JSONObject().apply {
            put("timestamp_utc_ms", m.header.timestampUtcMs)
            put("measurement_id", m.header.measurementId)
            put("app_version", m.header.appVersion)
            put("android_version", m.header.androidVersion)
            put("device_model", m.header.deviceModel)
            put("user_consent_version", m.header.userConsentVersion)
        }

        val contextObj = JSONObject().apply {
            put("transport", m.context.transport)
            putOpt("validated_internet", m.context.validatedInternet)
            putOpt("captive_portal", m.context.captivePortal)
            putOpt("metered", m.context.metered)
            putOpt("vpn_present", m.context.vpnPresent)
            put("battery_saver", m.context.batterySaver)
            put("charging", m.context.charging)
            put("screen_on", m.context.screenOn)
            put("foreground", m.context.foreground)
            putOpt("coarse_location", m.context.coarseLocation?.let { loc ->
                JSONObject().apply {
                    put("lat", loc.lat)
                    put("lon", loc.lon)
                    put("accuracy_meters", loc.accuracyMeters)
                }
            })
        }

        val perf = JSONObject().apply {
            put("endpoint_id", m.performance.endpointId)
            putOpt("dns_ms", m.performance.dnsMs)
            putOpt("tcp_ms", m.performance.tcpMs)
            putOpt("tls_ms", m.performance.tlsMs)
            putOpt("ttfb_ms", m.performance.ttfbMs)
            putOpt("rtt_avg_ms", m.performance.rttAvgMs)
            putOpt("rtt_p95_ms", m.performance.rttP95Ms)
            putOpt("jitter_ms", m.performance.jitterMs)
            putOpt("packet_loss_pct", m.performance.packetLossPct)
            putOpt("down_mbps", m.performance.downMbps)
            putOpt("up_mbps", m.performance.upMbps)
            putOpt("test_payload_bytes", m.performance.testPayloadBytes)
            put("protocol", m.performance.protocol)
        }

        val wifi = m.wifi?.let { w ->
            JSONObject().apply {
                putOpt("rssi", w.rssi)
                putOpt("link_speed_mbps", w.linkSpeedMbps)
                putOpt("frequency_mhz", w.frequencyMhz)
                putOpt("channel_width_mhz", w.channelWidthMhz)
            }
        }

        val cell = m.cell?.let { c ->
            JSONObject().apply {
                putOpt("carrier_name", c.carrierName)
                putOpt("mcc", c.mcc)
                putOpt("mnc", c.mnc)
                putOpt("data_network_type", c.dataNetworkType)
                putOpt("voice_network_type", c.voiceNetworkType)
                putOpt("roaming", c.roaming)
                putOpt("registered_rat", c.registeredRat)
                putOpt("serving_cell", c.servingCell?.let { sc ->
                    JSONObject().apply {
                        putOpt("ci", sc.ci)
                        putOpt("nci", sc.nci)
                        putOpt("tac", sc.tac)
                        putOpt("pci", sc.pci)
                        putOpt("earfcn", sc.earfcn)
                        putOpt("nrarfcn", sc.nrarfcn)
                        putOpt("band", sc.band)
                    }
                })
                putOpt("signal", c.signal?.let { s ->
                    JSONObject().apply {
                        putOpt("rsrp", s.rsrp)
                        putOpt("rsrq", s.rsrq)
                        putOpt("sinr", s.sinr)
                        putOpt("rssi", s.rssi)
                    }
                })
                put("availability", JSONObject().apply {
                    put("cellInfoAccessible", c.availability.cellInfoAccessible)
                    put("idsAccessible", c.availability.idsAccessible)
                    put("signalAccessible", c.availability.signalAccessible)
                })
            }
        }

        return JSONObject().apply {
            put("header", header)
            put("context", contextObj)
            putOpt("wifi", wifi)
            putOpt("cell", cell)
            put("performance", perf)
            // If you have feedback tags in your model, keep it only if it already exists.
            // (Not adding new fields beyond your current schema.)
        }
    }
}