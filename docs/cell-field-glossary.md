# Cell Measurement Field Glossary

Plain-English reference for every field in the telephony measurement models.
"Null" means the value could not be determined (API limitation, no permission, or genuinely unavailable for that RAT).

---

## CellInfo (top-level measurement)

| Field | What it means |
|---|---|
| `simCarriers` | List of identities for SIM cards currently in the device |
| `collectedSubscriptionId` | The subscription ID that was actively being used for the measurement |
| `collectedSimSlotIndex` | The physical SIM slot index for the collected subscription |
| `rat` | **Radio Access Technology** — the cellular generation in use: `GSM`, `WCDMA`, `LTE`, or `NR` |
| `nrState` | Whether 5G NR is active and how (see [NrState](#nrstate)) |
| `dataNetworkType` | Technology used for mobile data — may differ from voice (e.g. data on NR while voice falls back to LTE) |
| `voiceNetworkType` | Technology used for voice calls |
| `roaming` | `true` if the device is using a foreign/partner network outside the home carrier's coverage |
| `serving` | Full snapshot of the cell tower currently handling this device's connection |
| `neighbors` | Other visible (non-serving) towers. Useful for coverage mapping and handover analysis |

---

## CarrierInfo

| Field | What it means |
|---|---|
| `carrierName` | Human-readable operator name (e.g. "Vodafone", "AT&T", "Azercell") |
| `mcc` | **Mobile Country Code** — 3-digit number identifying the country (e.g. `310` = USA, `400` = Azerbaijan) |
| `mnc` | **Mobile Network Code** — 2–3 digit number identifying the carrier within the country |
| `simOperatorId` | MCC + MNC combined into one string (e.g. `"31026"` = T-Mobile USA). Used as a globally unique carrier key |
| `simOperatorName` | Registered operator name for the SIM |
| `countryIso` | 2-letter ISO 3166 country code from the SIM card (e.g. `"us"`, `"az"`, `"gb"`) |
| `duplexMode` | TDD or FDD duplex mode if known |
| `subscriptionId` | Android platform subscription ID |
| `simSlotIndex` | Physical SIM slot (0 or 1) |
| `isDefaultData` | Whether this SIM is the primary source for mobile data |
| `isActiveData` | Whether this SIM was actively handling data during the measurement |

---

## NrState

| Value | What it means |
|---|---|
| `NONE` | No 5G active. Device is on LTE or older technology |
| `NSA` | **Non-Standalone** — 5G NR secondary cell helps an LTE connection carry data. LTE still handles signaling and control. The most common "5G" deployment in 2023–2024 |
| `SA` | **Standalone** — True 5G. NR handles both control signaling and data without needing LTE as an anchor |

---

## CellRadioSnapshot

### Generic Info

| Field | What it means |
|---|---|
| `timestampOffsetMs` | How many milliseconds ago this cell reading was captured by the OS. `0` = just collected. Large values (> 5000 ms) mean the OS is returning stale cached data |
| `radio` | Technology-specific details (see [CellRadio](#cellradio)) |
| `dbm` | **Unified signal strength in dBm** — the single best number for comparing signal across RATs. RSRP for LTE/NR, RSCP for WCDMA, signal level for GSM |
| `asuLevel` | **Arbitrary Strength Units** — Android's normalized 0–97 signal scale used for signal bar display |
| `level` | Android's 0..4 quality category (0 = none/unknown) |
| `subscriptionId` | Which subscription this cell was observed on |
| `isRegistered` | Whether the device is actively registered/attached to this cell |

---

## CellRadio

This is a sealed structure containing details specific to the radio technology.

### GSM
| Field | What it means |
|---|---|
| `cellId` | GSM Cell ID (CID) |
| `lac` | Location Area Code |
| `bsic` | Base Station Identity Code |
| `arfcn` | Absolute Radio Frequency Channel Number |
| `rssiDbm` | Signal strength in dBm |
| `timingAdvance` | Timing Advance (range 0-63) |

### WCDMA
| Field | What it means |
|---|---|
| `cellId` | WCDMA Cell ID |
| `lac` | Location Area Code |
| `psc` | Primary Scrambling Code |
| `uarfcn` | UTRA-ARFCN |
| `ecNoDb` | Signal quality metric |

### LTE
| Field | What it means |
|---|---|
| `cellId` | LTE Cell Identity (CI) |
| `tac` | Tracking Area Code |
| `pci` | Physical Cell ID |
| `earfcn` | E-UTRA ARFCN |
| `bands` | List of active frequency bands |
| `bandwidthKhz` | Channel bandwidth in kHz |
| `rsrpDbm` | Reference Signal Received Power |
| `rsrqDb` | Reference Signal Received Quality |
| `rssnrDb` | Signal-to-Noise Ratio (dB) |
| `rssiDbm` | Received Signal Strength Indicator |
| `cqi` | Channel Quality Indicator |
| `timingAdvance` | LTE Timing Advance |
| `mimoLayers` | Number of spatial streams |

### NR (5G)
| Field | What it means |
|---|---|
| `cellId` | 36-bit NR Cell Identity (NCI) |
| `tac` | Tracking Area Code |
| `pci` | Physical Cell ID |
| `nrarfcn` | NR-ARFCN |
| `bands` | List of active frequency bands |
| `ss` | Synchronization Signal metrics (RSRP, RSRQ, SINR) |
| `csi` | Channel State Information metrics (RSRP, RSRQ, SINR) |
| `bandwidthKhz` | Channel bandwidth in kHz |
| `mimoLayers` | Number of spatial streams |

---

### Signal Strength — Generic (all RATs)

| Field | What it means | Good range | Available on |
|---|---|---|---|
| `rsrpDbm` | **Reference Signal Received Power** — the tower's signal strength at the device. The primary quality indicator for LTE/NR. For WCDMA this is RSCP; for GSM the received level | > −85 dBm | LTE, NR, WCDMA (as RSCP), TD-SCDMA (as RSCP) |
| `rsrqDb` | **Reference Signal Received Quality** — signal quality after accounting for interference from other cells. For WCDMA this is Ec/No | > −10 dB | LTE, NR, WCDMA (as Ec/No) |
| `sinrDb` | **Signal-to-Interference-plus-Noise Ratio** — how clean the signal is relative to background noise. Higher = better | > 0 dB | LTE, NR, WCDMA (as Ec/No proxy) |
| `rssiDbm` | **Received Signal Strength Indicator** — total received power including interference and noise. Less precise than RSRP | > −85 dBm | GSM, LTE (supplementary) |
| `cqi` | **Channel Quality Indicator** — the device reports this (0–15) to tell the tower how good the downlink channel is. Higher = tower can use denser modulation = higher speeds | > 7 | LTE, NR (via reflection) |
| `asuLevel` | **Arbitrary Strength Units** — Android's normalized 0–97 signal scale used for signal bar display. Mapping to dBm differs by RAT | — | All |
| `dbm` | **Unified signal strength in dBm** — the single best number for comparing signal across RATs. RSRP for LTE/NR, RSCP for WCDMA/TD-SCDMA, signal level for GSM | > −85 dBm | All |

---

### LTE / GSM Specific

| Field | What it means |
|---|---|
| `timingAdvance` | **Timing Advance** — measures the round-trip radio propagation delay to the tower. Android uses this to synchronize transmissions. **Also useful as a distance proxy**: LTE: each unit ≈ 78 m (range 0–1282). GSM: each unit ≈ 550 m (range 0–63). A value of 10 in LTE ≈ 780 m from the tower |

---

### 5G NR — Synchronization Signal (SS) Beams

These are measured from the always-on SS/PBCH (Synchronization Signal / Physical Broadcast Channel) block that every 5G cell broadcasts. They're the most reliable and always-present NR measurements.

| Field | What it means |
|---|---|
| `ssRsrpDbm` | **SS-RSRP** — 5G signal strength from the synchronization beam (dBm). Good: > −110 dBm |
| `ssRsrqDb` | **SS-RSRQ** — 5G signal quality from the sync beam (dB). Accounts for cell load/interference |
| `ssSinrDb` | **SS-SINR** — signal-to-noise for the sync beam. Most reliable NR link quality indicator |

---

### 5G NR — Channel State Information (CSI) — API 31+

CSI measurements come from reference signals sent specifically for channel estimation. They're more precise but only available on API 31 (Android 12) and later.

| Field | What it means |
|---|---|
| `csiRsrpDbm` | **CSI-RSRP** — signal strength estimated from channel-state reference signals. More precise than SS-RSRP for beamforming scenarios |
| `csiRsrqDb` | **CSI-RSRQ** — channel-state signal quality |
| `csiSinrDb` | Not available via public Android API — always null. Reserved for future use |

---

### Capacity

| Field | What it means |
|---|---|
| `bandwidthMhz` | **Channel bandwidth in MHz** — how wide a frequency slice the cell is using. Wider = more data capacity. Common LTE values: 5, 10, 15, 20 MHz. NR can go up to 100 MHz (sub-6 GHz) or 400 MHz (mmWave) |
| `mimoLayers` | **MIMO spatial streams** — the number of independent data streams transmitted simultaneously using multiple antennas. 2 layers ≈ 2× peak throughput vs 1 layer. Not yet populated from public Android API |

---

## Signal Quality Quick Reference

| Metric | Excellent | Good | Fair | Poor |
|---|---|---|---|---|
| RSRP (LTE/NR) | > −80 dBm | −80 to −90 | −90 to −100 | < −100 dBm |
| RSRQ | > −10 dB | −10 to −15 | −15 to −20 | < −20 dB |
| SINR | > 20 dB | 13 to 20 | 0 to 13 | < 0 dB |
| RSSI (GSM/legacy) | > −70 dBm | −70 to −85 | −85 to −100 | < −100 dBm |
| CQI | 12–15 | 8–11 | 4–7 | 0–3 |

---

## RAT Coverage Matrix

Which fields are populated per Radio Access Technology:

| Field | GSM (2G) | WCDMA (3G) | TD-SCDMA (3G) | LTE (4G) | NR (5G) |
|---|---|---|---|---|---|
| `cid` | ✓ | ✓ | ✓ | — | — |
| `cellId` | — | — | — | ✓ | — |
| `nci` | — | — | — | — | ✓ |
| `lac` | ✓ | ✓ | ✓ | — | — |
| `tac` | — | — | — | ✓ | ✓ |
| `pci` | — | — | — | ✓ | ✓ |
| `psc` | — | ✓ | ✓ (cpid) | — | — |
| `bsic` | ✓ | — | — | — | — |
| `arfcn` | ✓ (GERAN) | — | — | ✓ (EARFCN) | — |
| `uarfcn` | — | ✓ | ✓ | — | — |
| `nrarfcn` | — | — | — | — | ✓ |
| `rsrpDbm` | — | ✓ (RSCP) | ✓ (RSCP) | ✓ | ✓ |
| `rsrqDb` | — | ✓ (Ec/No) | — | ✓ | ✓ |
| `sinrDb` | — | ✓ (Ec/No) | — | ✓ | ✓ |
| `rssiDbm` | ✓ | — | — | ✓ | — |
| `dbm` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `timingAdvance` | ✓ | — | — | ✓ | — |
| `ss*` fields | — | — | — | — | ✓ |
| `csi*` fields | — | — | — | — | ✓ (API 31+) |
| `bandwidthMhz` | — | — | — | ✓ | future |
