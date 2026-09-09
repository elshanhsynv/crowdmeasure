package com.example.crowdmeasure.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSessionExport
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellRadio
import com.crowdmeasure.sdk.model.CellRadioSnapshot
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.NrSignal
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
            putOpt("app_name", m.meta.appName)
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
            putOpt("userPhoneNumber", m.meta.userPhoneNumber)
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

                            putOpt("serving_cell", c.serving?.let(::cellSnapshotToJson))

                            put("neighbors", JSONArray().apply {
                                c.neighbors.forEach { neighbor ->
                                    put(cellSnapshotToJson(neighbor))
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
            putOpt("stalls_count", m.performance.stallsCount)
            putOpt("max_stall_ms", m.performance.maxStallMs)
            putOpt("http_status", m.performance.httpStatus)
            putOpt("server_region", m.performance.serverRegion)
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
        putOpt(
            "serving",
            sample.cell.serving?.let(::cellSnapshotToJson)
        )

        put("neighbors", JSONArray().apply {
            sample.cell.neighbors.forEach { neighbor ->
                put(cellSnapshotToJson(neighbor))
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
        putOpt("card_id", carrier.cardId)
        putOpt("is_default_data", carrier.isDefaultData)
        putOpt("is_default_voice", carrier.isDefaultVoice)
        putOpt("is_default_sms", carrier.isDefaultSms)
        putOpt("is_active_data", carrier.isActiveData)
    }

    private fun cellSnapshotToJson(
        snapshot: CellRadioSnapshot
    ): JSONObject = JSONObject().apply {
        putOpt("timestamp_offset_ms", snapshot.timestampOffsetMs)
        putOpt("dbm", snapshot.dbm)
        putOpt("asu_level", snapshot.asuLevel)
        putOpt("level", snapshot.level)
        putOpt("subscription_id", snapshot.subscriptionId)
        putOpt("is_registered", snapshot.isRegistered)

        put("radio", cellRadioToJson(snapshot.radio))
    }

    private fun cellRadioToJson(
        radio: CellRadio
    ): JSONObject = JSONObject().apply {
        when (radio) {
            is CellRadio.Gsm -> {
                put("type", "gsm")
                putOpt("cell_id", radio.cellId)
                putOpt("lac", radio.lac)
                putOpt("bsic", radio.bsic)
                putOpt("arfcn", radio.arfcn)
                putOpt("rssi_dbm", radio.rssiDbm)
                putOpt("timing_advance", radio.timingAdvance)
            }

            is CellRadio.Wcdma -> {
                put("type", "wcdma")
                putOpt("cell_id", radio.cellId)
                putOpt("lac", radio.lac)
                putOpt("psc", radio.psc)
                putOpt("uarfcn", radio.uarfcn)
                putOpt("ec_no_db", radio.ecNoDb)
            }

            is CellRadio.Lte -> {
                put("type", "lte")
                putOpt("cell_id", radio.cellId)
                putOpt("tac", radio.tac)
                putOpt("pci", radio.pci)
                putOpt("earfcn", radio.earfcn)

                put("bands", JSONArray().apply {
                    radio.bands.forEach { put(it) }
                })

                putOpt("bandwidth_khz", radio.bandwidthKhz)
                putOpt("rsrp_dbm", radio.rsrpDbm)
                putOpt("rsrq_db", radio.rsrqDb)
                putOpt("rssnr_db", radio.rssnrDb)
                putOpt("rssi_dbm", radio.rssiDbm)
                putOpt("cqi", radio.cqi)
                putOpt("timing_advance", radio.timingAdvance)
                putOpt("mimo_layers", radio.mimoLayers)
            }

            is CellRadio.Nr -> {
                put("type", "nr")
                putOpt("cell_id", radio.cellId)
                putOpt("tac", radio.tac)
                putOpt("pci", radio.pci)
                putOpt("nrarfcn", radio.nrarfcn)

                put("bands", JSONArray().apply {
                    radio.bands.forEach { put(it) }
                })

                putOpt("ss", radio.ss?.let(::nrSignalToJson))
                putOpt("csi", radio.csi?.let(::nrSignalToJson))
                putOpt("bandwidth_khz", radio.bandwidthKhz)
                putOpt("mimo_layers", radio.mimoLayers)
            }
        }
    }

    private fun nrSignalToJson(
        signal: NrSignal
    ): JSONObject = JSONObject().apply {
        putOpt("rsrp_dbm", signal.rsrpDbm)
        putOpt("rsrq_db", signal.rsrqDb)
        putOpt("sinr_db", signal.sinrDb)
    }
}
