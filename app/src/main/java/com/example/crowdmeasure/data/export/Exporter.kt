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
                put("schema_version", 2) // UPDATED
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
        val meta = JSONObject().apply {
            putOpt("measurement_id", m.meta.measurementId)
            putOpt("timestamp_utc_ms", m.meta.timestampUtcMs)
            putOpt("device_model", m.meta.deviceModel)
            putOpt("os_version", m.meta.osVersion)
            putOpt("sdk_int", m.meta.sdkInt)
            putOpt("app_version", m.meta.appVersion)
            putOpt("session_id", m.meta.sessionId)
            putOpt("user_id_hash", m.meta.userIdHash)
        }

        val environment = JSONObject().apply {

            putOpt("location", m.environment.location?.let { loc ->
                JSONObject().apply {
                    put("lat", loc.lat)
                    put("lon", loc.lon)
                    put("accuracy_meters", loc.accuracyMeters)
                }
            })
            putOpt("network", m.environment.network?.let { net ->
                JSONObject().apply {
                    put("transport", net.transport)
                    putOpt("ip", net.ip)
                    putOpt("validated_internet", net.validatedInternet)
                    putOpt("captive_portal", net.captivePortal)
                    putOpt("vpn", net.vpn)
                    putOpt("metered", net.metered)
                    putOpt("wifi", net.wifi?.let { w ->
                        JSONObject().apply {
                            putOpt("bssid_hash", w.bssidHash)
                            putOpt("ssid_hash", w.ssidHash)
                            put("standard", w.standard)
                            putOpt("frequency_mhz", w.frequencyMhz)
                            putOpt("channel_width_mhz", w.channelWidthMhz)
                            putOpt("rssi_dbm", w.rssiDbm)
                            putOpt("link_speed_mbps", w.linkSpeedMbps)
                            putOpt("tx_link_speed_mbps", w.txLinkSpeedMbps)
                            putOpt("rx_link_speed_mbps", w.rxLinkSpeedMbps)
                        }
                    })
                    putOpt("cell", net.cell?.let { c ->
                        JSONObject().apply {
                            putOpt("carrier", c.carrier.apply {
                                putOpt("carrier_name", c.carrier.carrierName)
                                putOpt("mcc", c.carrier.mcc)
                                putOpt("mnc", c.carrier.mnc)
                            })
                            putOpt("rat", c.rat)
                            putOpt("nr_state", c.nrState)
                            putOpt("data_network_type", c.dataNetworkType)
                            putOpt("voice_network_type", c.voiceNetworkType)
                            putOpt("roaming", c.roaming)
                            putOpt("serving_cell", c.serving?.let {
                                putOpt("cell_id", c.serving.cellId)
                                putOpt("nci", c.serving.nci)
                                putOpt("band", c.serving.band)
                                putOpt("arfcn", c.serving.arfcn)
                                putOpt("nrarfcn", c.serving.nrarfcn)
                                putOpt("tac", c.serving.tac)
                                putOpt("pci", c.serving.pci)
                                putOpt("rsrp_dbm", c.serving.rsrpDbm)
                                putOpt("rsrq_db", c.serving.rsrqDb)
                                putOpt("sinr_db", c.serving.sinrDb)
                                putOpt("cqi", c.serving.cqi)
                                putOpt("rssi", c.serving.rssiDbm)
                                putOpt("bandwidth_mhz", c.serving.bandwidthMhz)
                                putOpt("mimo_layers", c.serving.mimoLayers)
                            })
                            putOpt("neighbors", JSONArray().apply {
                                c.neighbors.forEach {
                                    put(JSONObject().apply {
                                        putOpt("cell_id", it.cellId)
                                        putOpt("nci", it.nci)
                                        putOpt("band", it.band)
                                        putOpt("arfcn", it.arfcn)
                                        putOpt("nrarfcn", it.nrarfcn)
                                        putOpt("tac", it.tac)
                                        putOpt("pci", it.pci)
                                        putOpt("rsrp_dbm", it.rsrpDbm)
                                        putOpt("rsrq_db", it.rsrqDb)
                                        putOpt("sinr_db", it.sinrDb)
                                        putOpt("cqi", it.cqi)
                                        putOpt("rssi", it.rssiDbm)
                                        putOpt("bandwidth_mhz", it.bandwidthMhz)
                                        putOpt("mimo_layers", it.mimoLayers)
                                    })
                                }
                            })
                            putOpt("aggregation", c.aggregation?.let { ag ->
                                putOpt("active", ag.active)
                                putOpt("secondary_cells", JSONArray().apply {
                                    ag.secondaryCells.forEach {
                                        put(JSONObject().apply {
                                            putOpt("band", it.band)
                                            putOpt("earfcn", it.earfcn)
                                            putOpt("nrarfcn", it.nrarfcn)
                                            putOpt("pci", it.pci)
                                            putOpt("rsrp", it.rsrp)
                                            putOpt("rsrq", it.rsrq)
                                            putOpt("sinr", it.sinr)
                                            putOpt("bandwidth_mhz", it.bandwidthMhz)
                                        })
                                    }
                                })
                            })
                        }
                    })
                }
            })
        }

//        val config = JSONObject().apply {
//            put("server", m.config.server.apply {
//                putOpt("server_id", m.config.server.serverId)
//                putOpt("host", m.config.server.host)
//                putOpt("ip", m.config.server.ip)
//                putOpt("distance_km", m.config.server.distanceKm)
//                putOpt("selection_method", m.config.server.selectionMethod)
//                putOpt("pretest_latency_ms", m.config.server.pretestLatencyMs)
//            })
//            put("protocol", m.config.protocol)
//            put("download", m.config.download.apply {
//                putOpt("streams", m.config.download.streams)
//                putOpt("duration_ms", m.config.download.durationMs)
//                putOpt("warmup_ms", m.config.download.warmupMs)
//                putOpt("payload_bytes", m.config.download.payloadBytes)
//            })
//            put("upload", m.config.upload.apply {
//                putOpt("streams", m.config.upload.streams)
//                putOpt("duration_ms", m.config.upload.durationMs)
//                putOpt("warmup_ms", m.config.upload.warmupMs)
//                putOpt("payload_bytes", m.config.upload.payloadBytes)
//            })
//            put("latency_probe", m.config.latencyProbe.apply {
//                putOpt("sample_count", m.config.latencyProbe.sampleCount)
//                putOpt("interval_ms", m.config.latencyProbe.intervalMs)
//                putOpt("during_load", m.config.latencyProbe.duringLoad)
//            })
//        }

//        val phases = JSONArray().apply {
//            m.phases.forEach {
//                put(JSONObject().apply {
//                    put("type", it.type)
//                    put("metrics", JSONObject().apply {
//                        putOpt("duration_ms", it.metrics.durationMs)
//                        putOpt("throughput_avg_mbps", it.metrics.throughputAvgMbps)
//                        putOpt("throughput_p95_mbps", it.metrics.throughputP95Mbps)
//                        putOpt("throughput_std_dev", it.metrics.throughputStdDev)
//                        putOpt("latency_avg_ms", it.metrics.latencyAvgMs)
//                        putOpt("latency_p95_ms", it.metrics.latencyP95Ms)
//                        putOpt("jitter_ms", it.metrics.jitterMs)
//                        putOpt("packet_loss_pct", it.metrics.packetLossPct)
//                    })
//                    putOpt("series", it.series?.let { s ->
//                        JSONObject().apply {
//                            putOpt("throughput", JSONArray().apply {
//                                s.throughput?.forEach {
//                                    put(JSONObject().apply {
//                                        putOpt("t_ms", it.tMs)
//                                        putOpt("mbps", it.mbps)
//                                    })
//                                }
//                                putOpt("latency", JSONArray().apply {
//                                    s.latency?.forEach {
//                                        put(JSONObject().apply {
//                                            putOpt("t_ms", it.tMs)
//                                            putOpt("rtt_ms", it.rttMs)
//                                        })
//                                    }
//                                })
//                            })
//                        }
//                    })
//                })
//            }
//        }
//
//        val summary = JSONObject().apply {
//            putOpt("downlink_mbps", m.summary.downlinkMbps)
//            putOpt("uplink_mbps", m.summary.uplinkMbps)
//            putOpt("latency_idle_ms", m.summary.latencyIdleMs)
//            putOpt("latency_download_ms", m.summary.latencyDownloadMs)
//            putOpt("latency_upload_ms", m.summary.uplinkMbps)
//            putOpt("jitter_ms", m.summary.jitterMs)
//            putOpt("packet_loss_pct", m.summary.packetLossPct)
//            putOpt("throughput_stability", m.summary.throughputStability)
//            putOpt("estimated_web_load_ms", m.summary.estimatedWebLoadMs)
//            putOpt("estimated_video_start_ms", m.summary.estimatedVideoStartMs)
//            putOpt("estimated_mos", m.summary.estimatedMos)
//        }

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
            putOpt("down_p95_mbps", m.performance.downP95Mbps)
            putOpt("down_stddev_mbps", m.performance.downStdDevMbps)
            putOpt("up_p95_mbps", m.performance.upP95Mbps)
            putOpt("up_stddev_mbps", m.performance.upStdDevMbps)
            putOpt("stalls_count", m.performance.stallsCount)
            putOpt("max_stall_ms", m.performance.maxStallMs)
            putOpt("http_status", m.performance.httpStatus)
            putOpt("server_region", m.performance.serverRegion)
            putOpt("test_payload_bytes", m.performance.testPayloadBytes)
            put("protocol", m.performance.protocol)
        }

        return JSONObject().apply {
            putOpt("meta", meta)
            putOpt("environment", environment)
            putOpt("performance", perf)
//            putOpt("config", config)
//            putOpt("phases", phases)
//            putOpt("summary", summary)
        }
    }
}
