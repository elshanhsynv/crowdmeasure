package com.example.crowdmeasure.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSessionExport
import com.example.crowdmeasure.domain.model.CarrierInfo
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

    suspend fun exportCallSessionsToJson(
        sessions: List<CallSessionExport>,
        filePrefix: String = "crowdmeasure_call_sessions",
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "${filePrefix}_$ts.json")

            val root = JSONObject().apply {
                put("schema_version", 1)
                put("exported_at_utc_ms", System.currentTimeMillis())
                put("session_count", sessions.size)
                put("sessions", JSONArray().apply {
                    sessions.forEach { put(callSessionToJson(it.session, it.samples)) }
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
            putOpt("app_version", m.meta.appVersion)
            putOpt("android_release", m.meta.androidRelease)
            putOpt("android_sdk", m.meta.androidSdk)
            putOpt("device_model", m.meta.deviceModel)

            putOpt("brand", m.meta.brand)
            putOpt("device_manufacturer", m.meta.deviceManufacturer)
            putOpt("device_os", m.meta.deviceOS)
            putOpt("build_id", m.meta.buildID)
            putOpt("hardware", m.meta.hardware)
            putOpt("chipset", m.meta.chipset)
            putOpt("chipset_manufacturer", m.meta.chipsetManufacturer)
            putOpt("session_id", m.meta.sessionId)
            putOpt("user_id", m.meta.userIdHash)
        }

        val environment = JSONObject().apply {

            putOpt("location", m.environment.location?.let { loc ->
                JSONObject().apply {
                    put("lat", loc.lat)
                    put("lon", loc.lon)
                    put("accuracy_meters", loc.accuracyMeters)
                }
            })
            putOpt("network", m.environment.network.let { net ->
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
                            putOpt("collected_subscription_id", c.collectedSubscriptionId)
                            putOpt("collected_sim_slot_index", c.collectedSimSlotIndex)
                            put("sim_carriers", JSONArray().apply {
                                c.simCarriers.forEach {
                                    put(carrierToJson(it))
                                }
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
            putOpt("connect_ms", m.performance.connectMs)
            putOpt("tls_ms", m.performance.tlsMs)
            putOpt("ttfb_ms", m.performance.ttfbAvgMs)
            putOpt("http_latency_avg_ms", m.performance.httpLatencyAvgMs)
            putOpt("http_latency_p95_ms", m.performance.httpLatencyP95Ms)
            putOpt("jitter_ms", m.performance.jitterMs)
            putOpt("packet_loss_pct", m.performance.probeFailurePct)
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

    private fun callSessionToJson(
        session: CallSession,
        samples: List<CallCellSample>
    ): JSONObject =
        JSONObject().apply {
            put("session_id", session.sessionId)
            put("started_at_utc_ms", session.startedAtUtcMs)
            putOpt("ended_at_utc_ms", session.endedAtUtcMs)
            put("call_type", session.callType.name)
            put("call_source", session.callSource.name)
            put("sample_interval_seconds", session.sampleIntervalSeconds)
            put("sample_count", session.sampleCount)
            putOpt("end_reason", session.endReason)
            put("samples", JSONArray().apply {
                samples.forEach { put(callSampleToJson(it)) }
            })
        }

    private fun callSampleToJson(sample: CallCellSample): JSONObject =
        JSONObject().apply {
            put("id", sample.id)
            put("session_id", sample.sessionId)
            put("sampled_at_utc_ms", sample.sampledAtUtcMs)
            put("elapsed_ms", sample.elapsedMs)
            putOpt("rat", sample.rat)
            putOpt("nr_state", sample.nrState)
            putOpt("dbm", sample.dbm)
            putOpt("rsrp_dbm", sample.rsrpDbm)
            putOpt("rsrq_db", sample.rsrqDb)
            putOpt("sinr_db", sample.sinrDb)
            putOpt("pci", sample.pci)
            putOpt("tac", sample.tac)
            putOpt("band", sample.band)
            putOpt("collected_subscription_id", sample.cell.collectedSubscriptionId)
            putOpt("collected_sim_slot_index", sample.cell.collectedSimSlotIndex)
            put("sim_carriers", JSONArray().apply {
                sample.cell.simCarriers.forEach {
                    put(carrierToJson(it))
                }
            })
            putOpt("data_network_type", sample.cell.dataNetworkType)
            putOpt("voice_network_type", sample.cell.voiceNetworkType)
            putOpt("roaming", sample.cell.roaming)
            putOpt("serving", sample.cell.serving?.let { serving ->
                JSONObject().apply {
                    putOpt("cell_id", serving.cellId)
                    putOpt("cid", serving.cid)
                    putOpt("nci", serving.nci)
                    putOpt("lac", serving.lac)
                    putOpt("tac", serving.tac)
                    putOpt("pci", serving.pci)
                    putOpt("band", serving.band)
                    putOpt("arfcn", serving.arfcn)
                    putOpt("uarfcn", serving.uarfcn)
                    putOpt("nrarfcn", serving.nrarfcn)
                    putOpt("rssi_dbm", serving.rssiDbm)
                    putOpt("rsrp_dbm", serving.rsrpDbm)
                    putOpt("rsrq_db", serving.rsrqDb)
                    putOpt("sinr_db", serving.sinrDb)
                    putOpt("asu_level", serving.asuLevel)
                    putOpt("dbm", serving.dbm)
                    putOpt("timing_advance", serving.timingAdvance)
                    putOpt("ss_rsrp_dbm", serving.ssRsrpDbm)
                    putOpt("ss_rsrq_db", serving.ssRsrqDb)
                    putOpt("ss_sinr_db", serving.ssSinrDb)
                    putOpt("bandwidth_mhz", serving.bandwidthMhz)
                }
            })
            put("neighbor_count", sample.cell.neighbors.size)
        }

    private fun carrierToJson(carrier: CarrierInfo): JSONObject =
        JSONObject().apply {
            putOpt("carrier_name", carrier.carrierName)
            putOpt("mcc", carrier.mcc)
            putOpt("mnc", carrier.mnc)
            putOpt("sim_operator_id", carrier.simOperatorId)
            putOpt("sim_operator_name", carrier.simOperatorName)
            putOpt("country_iso", carrier.countryIso)
            putOpt("duplex_mode", carrier.duplexMode)
            putOpt("subscription_id", carrier.subscriptionId)
            putOpt("sim_slot_index", carrier.simSlotIndex)
            putOpt("display_name", carrier.displayName)
            putOpt("carrier_id", carrier.carrierId)
            putOpt("data_roaming", carrier.dataRoaming)
            putOpt("is_embedded", carrier.isEmbedded)
            putOpt("is_opportunistic", carrier.isOpportunistic)
            putOpt("card_id", carrier.cardId)
            putOpt("port_index", carrier.portIndex)
            putOpt("is_default_data", carrier.isDefaultData)
            putOpt("is_default_voice", carrier.isDefaultVoice)
            putOpt("is_default_sms", carrier.isDefaultSms)
            putOpt("is_active_data", carrier.isActiveData)
        }
}
