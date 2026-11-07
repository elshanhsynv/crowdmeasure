# Measurement Schema

Field reference for the `Measurement` JSON that the SDK stores and exports. Use it to design the backend API.

Source of truth: `crowdmeasure-sdk-core/src/main/java/com/crowdmeasure/sdk/model/`.
A complete example is in `app/src/main/java/com/example/crowdmeasure/docs/demo_measurement_complete.json`.

## Conventions

- **One measurement is one JSON object** with top-level keys `meta`, `environment`, `performance`.
- **Nullable means null or absent.** The SDK encoders use `explicitNulls = false`, so a `null` value is not written; the key is missing.
- **Defaults are omitted too.** The encoders don't write fields equal to their default. A missing key is `null` or the default shown in the table (`0`, `[]`, `UNKNOWN`).
- **Types:** `Int` is int32, `Long` is int64, `Float` and `Double` are JSON numbers, `String` is a string.
- **Enums** are written by name (for example `"WIFI"`), except `NrState`, which uses the lowercase values listed below.
- **Timestamps** are Unix epoch milliseconds in UTC. Timing values are milliseconds.
- **Radio snapshots** use a `radio` object whose `type` field selects the technology. See [Cell radio variants](#cell-radio-variants).

Column meanings: **Null?** is whether the value can be `null`. **Default** is the value used when the key is missing and the field has one.

---

## Measurement

| Key | Type | Null? | Notes |
|---|---|---|---|
| `meta` | object (`Meta`) | no | |
| `environment` | object (`EnvironmentInfo`) | no | |
| `performance` | object (`PerformanceInfo`) | no | |

---

## meta

| Key | Type | Null? | Notes |
|---|---|---|---|
| `measurementId` | string | no | UUID, primary key |
| `timestampUtcMs` | int64 | no | Epoch ms when the run started |
| `appName` | string | no | Host app name; `unknown` if it can't be read |
| `appVersion` | string | no | App `versionName`, e.g. `1.14` |
| `androidRelease` | string | no | e.g. `16` |
| `androidSdk` | int32 | no | API level, e.g. `36` |
| `deviceModel` | string | no | `"<manufacturer> <model>"`, e.g. `samsung SM-S918B` |
| `brand` | string | no | `Build.BRAND` |
| `deviceManufacturer` | string | no | `Build.MANUFACTURER` |
| `deviceOS` | string | no | Always `"Android"` |
| `buildID` | string | no | `Build.ID` |
| `hardware` | string | no | `Build.HARDWARE` |
| `chipset` | string | no | `Build.SOC_MODEL` on API 31+ |
| `chipsetManufacturer` | string | no | `Build.SOC_MANUFACTURER` on API 31+ |
| `userPhoneNumber` | string | **yes** | |

---

## environment

| Key | Type | Null? | Notes |
|---|---|---|---|
| `location` | object (`Location`) | **yes** | Null when no fix |
| `network` | object (`NetworkEnvironment`) | no | |
| `device` | object (`DeviceEnvironment`) | no | |

### environment.location

| Key | Type | Null? | Notes |
|---|---|---|---|
| `lat` | double | no | Degrees |
| `lon` | double | no | Degrees |
| `accuracyMeters` | float | no | Horizontal accuracy |

### environment.network

| Key | Type | Null? | Notes |
|---|---|---|---|
| `transport` | enum `TransportType` | no | `WIFI`, `CELL`, `OTHER`, `NONE`, `MIXED` |
| `ip` | object (`IpInfo`) | no | Fields inside may each be null |
| `validatedInternet` | bool | **yes** | |
| `captivePortal` | bool | **yes** | |
| `vpn` | bool | **yes** | |
| `metered` | bool | **yes** | |
| `wifi` | object (`WifiInfo`) | **yes** | Null when Wi-Fi isn't the collected link |
| `cell` | object (`CellInfo`) | **yes** | Null when cellular isn't collected |
| `dataUsage` | object (`DataUsageInfo`) | **yes** | |

### environment.network.ip

| Key | Type | Null? | Notes |
|---|---|---|---|
| `publicIp` | string | **yes** | Public IP address |
| `ispName` | string | **yes** | Lookup service's ISP name |
| `asn` | int32 | **yes** | Autonomous System Number |

All three are null when the lookup failed or timed out.

### environment.network.dataUsage

| Key | Type | Null? | Notes |
|---|---|---|---|
| `dlMB` | double | no | Downloaded megabytes |
| `ulMB` | double | no | Uploaded megabytes |
| `dlKbps` | double | no | Download rate, kilobits per second |
| `ulKbps` | double | no | Upload rate, kilobits per second |

### environment.network.wifi

| Key | Type | Null? | Notes |
|---|---|---|---|
| `bssidHash` | string | **yes** | |
| `ssid` | string | **yes** | Network name |
| `standard` | enum `WifiStandard` | no | `UNKNOWN`, `WIFI_4`, `WIFI_5`, `WIFI_6`, `WIFI_6E`, `WIFI_7` |
| `frequencyMhz` | int32 | **yes** | e.g. `5180` |
| `channelWidthMhz` | int32 | **yes** | `20`, `40`, `80`, `160`, `320` |
| `rssiDbm` | int32 | **yes** | |
| `linkSpeedMbps` | int32 | **yes** | |
| `txLinkSpeedMbps` | int32 | **yes** | |
| `rxLinkSpeedMbps` | int32 | **yes** | |

### environment.network.cell

| Key | Type | Null? | Notes |
|---|---|---|---|
| `simCarriers` | array of `CarrierInfo` | no | Default `[]` |
| `collectedSubscriptionId` | int32 | **yes** | Subscription the radio data came from |
| `collectedSimSlotIndex` | int32 | **yes** | |
| `dataNetworkType` | string | **yes** | e.g. `LTE` |
| `voiceNetworkType` | string | **yes** | e.g. `LTE` |
| `roaming` | bool | **yes** | |
| `rat` | string | **yes** | Serving technology, e.g. `LTE` |
| `nrState` | enum `NrState` | no | Lowercase: `none`, `nsa`, `sa` |
| `serving` | object (`CellRadioSnapshot`) | **yes** | Serving cell |
| `neighbors` | array of `CellRadioSnapshot` | no | Default `[]` |

### environment.network.cell.simCarriers[] (`CarrierInfo`)

All fields are nullable and default to null.

| Key | Type | Notes |
|---|---|---|
| `carrierName` | string | |
| `mcc` | string | Keep as string; leading zeros matter, e.g. `"400"` |
| `mnc` | string | Keep as string, e.g. `"02"` |
| `simOperatorId` | string | e.g. `"40002"` |
| `simOperatorName` | string | |
| `countryIso` | string | SIM country code, e.g. `"az"` |
| `duplexMode` | string | e.g. `"FDD"` |
| `subscriptionId` | int32 | |
| `simSlotIndex` | int32 | |
| `displayName` | string | |
| `carrierId` | int32 | |
| `dataRoaming` | bool | |
| `isEmbedded` | bool | |
| `cardId` | int32 | |
| `isDefaultData` | bool | |
| `isDefaultVoice` | bool | |
| `isDefaultSms` | bool | |
| `isActiveData` | bool | |

### CellRadioSnapshot (`serving` and each item of `neighbors`)

| Key | Type | Null? | Notes |
|---|---|---|---|
| `timestampOffsetMs` | int64 | **yes** | Observation time minus measurement start |
| `radio` | `CellRadio` (see below) | no | Discriminated by `type` |
| `dbm` | int32 | **yes** | Technology's display value: RSSI, RSCP, RSRP or SS-RSRP |
| `asuLevel` | int32 | **yes** | Arbitrary strength unit |
| `level` | int32 | **yes** | 0–4. `0` means none or unknown, not missing |
| `subscriptionId` | int32 | **yes** | |
| `isRegistered` | bool | **yes** | `true` for the serving cell |

### Cell radio variants

`radio` changes with its `type` field. The variants are listed in [cell_radio_variants.md](../../app/src/main/java/com/example/crowdmeasure/docs/cell_radio_variants.md) with example JSON.

**gsm** (`type: "gsm"`)

| Key | Type | Null? | Notes |
|---|---|---|---|
| `cellId` | int32 | **yes** | |
| `lac` | int32 | **yes** | Location area code |
| `bsic` | int32 | **yes** | |
| `arfcn` | int32 | **yes** | |
| `rssiDbm` | int32 | **yes** | |
| `timingAdvance` | int32 | **yes** | GSM symbol periods |

**wcdma** (`type: "wcdma"`)

| Key | Type | Null? | Notes |
|---|---|---|---|
| `cellId` | int32 | **yes** | |
| `lac` | int32 | **yes** | |
| `psc` | int32 | **yes** | Primary scrambling code |
| `uarfcn` | int32 | **yes** | |
| `ecNoDb` | int32 | **yes** | Signal quality |

**lte** (`type: "lte"`)

| Key | Type | Null? | Default | Notes |
|---|---|---|---|---|
| `cellId` | int32 | **yes** | | |
| `tac` | int32 | **yes** | | Tracking area code |
| `pci` | int32 | **yes** | | Physical cell ID |
| `earfcn` | int32 | **yes** | | |
| `bands` | array of int32 | no | `[]` | Empty means not reported |
| `bandwidthKhz` | int32 | **yes** | | |
| `rsrpDbm` | int32 | **yes** | | |
| `rsrqDb` | int32 | **yes** | | |
| `rssnrDb` | int32 | **yes** | | |
| `rssiDbm` | int32 | **yes** | | |
| `cqi` | int32 | **yes** | | |
| `timingAdvance` | int32 | **yes** | | Raw Android value, not meters |
| `mimoLayers` | int32 | **yes** | | |

**nr** (`type: "nr"`)

| Key | Type | Null? | Default | Notes |
|---|---|---|---|---|
| `cellId` | int64 | **yes** | | |
| `tac` | int32 | **yes** | | |
| `pci` | int32 | **yes** | | |
| `nrarfcn` | int32 | **yes** | | |
| `bands` | array of int32 | no | `[]` | |
| `ss` | object (`NrSignal`) | **yes** | | Synchronization-signal measurement |
| `csi` | object (`NrSignal`) | **yes** | | Channel-state measurement |
| `bandwidthKhz` | int32 | **yes** | | |
| `mimoLayers` | int32 | **yes** | | |

**NrSignal** (`ss`, `csi`): `rsrpDbm` int32, `rsrqDb` int32, `sinrDb` int32. All nullable.

### environment.device

| Key | Type | Null? | Notes |
|---|---|---|---|
| `batteryPct` | int32 | **yes** | 0–100 |
| `charging` | bool | no | |
| `batterySaver` | bool | **yes** | |
| `screenOn` | bool | no | |
| `dozeMode` | bool | **yes** | |
| `dataSaver` | bool | **yes** | |
| `thermalState` | string | **yes** | `NONE`, `LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`, `EMERGENCY`, `SHUTDOWN`, `UNKNOWN` |
| `cpuUsagePct` | double | **yes** | Coarse estimate |
| `memoryUsagePct` | double | **yes** | |

---

## performance

| Key | Type | Null? | Default | Notes |
|---|---|---|---|---|
| `endpointId` | string | no | | Endpoint URL probed, e.g. `https://www.bakcell.com/` |
| `dnsMs` | int64 | **yes** | | First DNS lookup in the run |
| `connectMs` | int64 | **yes** | | First new connection |
| `tlsMs` | int64 | **yes** | | First TLS handshake |
| `ttfbAvgMs` | int64 | **yes** | | Average time to first byte |
| `ttfbP95Ms` | int64 | **yes** | | 95th percentile TTFB |
| `httpLatencyAvgMs` | int64 | **yes** | | Average time to first body byte |
| `httpLatencyP95Ms` | int64 | **yes** | | |
| `jitterMs` | int64 | **yes** | | Average change between consecutive HTTP samples |
| `pingAvgMs` | int64 | **yes** | | TCP connect round trip |
| `pingMinMs` | int64 | **yes** | | |
| `pingMaxMs` | int64 | **yes** | | |
| `pingJitterMs` | int64 | **yes** | | |
| `pingPacketLossPct` | double | **yes** | | Percentage of failed TCP connects |
| `probeFailurePct` | double | **yes** | | Percentage of failed HTTP probes |
| `probesAttempted` | int32 | no | `0` | |
| `probesSucceeded` | int32 | no | `0` | |
| `probesFailed` | int32 | no | `0` | |
| `stallsCount` | int32 | **yes** | | |
| `maxStallMs` | int64 | **yes** | | |
| `httpStatus` | int32 | **yes** | | First HTTP status; non-2xx is still valid |
| `serverRegion` | string | **yes** | | CDN edge code from a known header; otherwise the `Server` header value |
| `firstResponseBodyStarted` | bool | **yes** | | |
| `protocol` | enum `ProtocolType` | no | `UNKNOWN` | `HTTP1_1`, `HTTP2`, `UNKNOWN` |
