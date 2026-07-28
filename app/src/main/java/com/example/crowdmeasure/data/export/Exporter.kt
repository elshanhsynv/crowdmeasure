package com.example.crowdmeasure.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSessionExport
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.Measurement
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
                context, "${context.packageName}.fileprovider", file
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
                    putOpt("data_usage", net.dataUsage?.let { usage ->
                        JSONObject().apply {
                            put("dl_kbps", usage.dlKbps)
                            put("ul_kbps", usage.ulKbps)
                        }
                    })
                    putOpt("wifi", net.wifi?.let { w ->
                        JSONObject().apply {
                            putOpt("bssid_hash", w.bssidHash)
                            putOpt("ssid", w.ssid)
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
                        }
                    })
                }
            })
        }

        val perf = JSONObject().apply {
            put("endpoint_id", m.performance.endpointId)
            putOpt("dns_ms", m.performance.dnsMs)
            putOpt("connect_ms", m.performance.connectMs)
            putOpt("tls_ms", m.performance.tlsMs)
            putOpt("ttfb_ms", m.performance.ttfbAvgMs)
            putOpt("http_latency_avg_ms", m.performance.httpLatencyAvgMs)
            putOpt("http_latency_p95_ms", m.performance.httpLatencyP95Ms)
            putOpt("jitter_ms", m.performance.jitterMs)
            putOpt("ping_avg_ms", m.performance.pingAvgMs)
            putOpt("ping_min_ms", m.performance.pingMinMs)
            putOpt("ping_max_ms", m.performance.pingMaxMs)
            putOpt("ping_jitter_ms", m.performance.pingJitterMs)
            putOpt("ping_packet_loss_pct", m.performance.pingPacketLossPct)
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
        }
    }

    private fun callSessionToJson(
        session: CallSession, samples: List<CallCellSample>
    ): JSONObject = JSONObject().apply {
        put("session_id", session.sessionId)
        put("started_at_utc_ms", session.startedAtUtcMs)
        putOpt("ended_at_utc_ms", session.endedAtUtcMs)
        put("call_type", session.callType.name)
        put("call_source", session.callSource.name)
        put("sample_interval_seconds", session.sampleIntervalSeconds)
        put("sample_count", session.sampleCount)
        putOpt("end_reason", session.endReason)
        put("sim_carriers", JSONArray().apply {
            session.simCarriers.forEach { put(carrierToJson(it)) }
        })
        put("samples", JSONArray().apply {
            samples.forEach { put(callSampleToJson(it)) }
        })
    }

    private fun callSampleToJson(sample: CallCellSample): JSONObject = JSONObject().apply {
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
        putOpt("transport_type", sample.transportType?.name)
        putOpt("location", sample.location?.let {
            JSONObject().apply {
                put("lat", it.lat)
                put("lon", it.lon)
                put("accuracy_meters", it.accuracyMeters)
            }
        })
        putOpt("data_usage", sample.dataUsage?.let { usage ->
            JSONObject().apply {
                put("dl_mb", usage.dlMB)
                put("ul_mb", usage.ulMB)
                put("dl_kbps", usage.dlKbps)
                put("ul_kbps", usage.ulKbps)
            }
        })
        putOpt("collected_subscription_id", sample.cell.collectedSubscriptionId)
        putOpt("collected_sim_slot_index", sample.cell.collectedSimSlotIndex)
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
        put("neighbors", JSONArray().apply {
            sample.cell.neighbors.forEach { nbr ->
                put(JSONObject().apply {
                    putOpt("cell_id", nbr.cellId)
                    putOpt("cid", nbr.cid)
                    putOpt("nci", nbr.nci)
                    putOpt("lac", nbr.lac)
                    putOpt("tac", nbr.tac)
                    putOpt("pci", nbr.pci)
                    putOpt("band", nbr.band)
                    putOpt("arfcn", nbr.arfcn)
                    putOpt("uarfcn", nbr.uarfcn)
                    putOpt("nrarfcn", nbr.nrarfcn)
                    putOpt("rssi_dbm", nbr.rssiDbm)
                    putOpt("rsrp_dbm", nbr.rsrpDbm)
                    putOpt("rsrq_db", nbr.rsrqDb)
                    putOpt("sinr_db", nbr.sinrDb)
                    putOpt("asu_level", nbr.asuLevel)
                    putOpt("dbm", nbr.dbm)
                    putOpt("timing_advance", nbr.timingAdvance)
                    putOpt("ss_rsrp_dbm", nbr.ssRsrpDbm)
                    putOpt("ss_rsrq_db", nbr.ssRsrqDb)
                    putOpt("ss_sinr_db", nbr.ssSinrDb)
                    putOpt("bandwidth_mhz", nbr.bandwidthMhz)
                })
            }
        })
    }

    private fun carrierToJson(carrier: CarrierInfo): JSONObject = JSONObject().apply {
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
