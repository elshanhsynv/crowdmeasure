# Cell Measurement Field Glossary

Plain-English reference for every field in the telephony measurement models.
"Null" means the value could not be determined (API limitation, no permission, or genuinely unavailable for that RAT).

---

## CellInfo (top-level measurement)

| Field | What it means |
|---|---|
| `carrier` | Your mobile operator's identity information |
| `rat` | **Radio Access Technology** — the cellular generation in use: `GSM`, `WCDMA`, `TD-SCDMA`, `LTE`, or `NR` |
| `nrState` | Whether 5G NR is active and how (see [NrState](#nrstate)) |
| `dataNetworkType` | Technology used for mobile data — may differ from voice (e.g. data on NR while voice falls back to LTE) |
| `voiceNetworkType` | Technology used for voice calls |
| `roaming` | `true` if the device is using a foreign/partner network outside the home carrier's coverage |
| `serving` | Full snapshot of the cell tower currently handling this device's connection |
| `neighbors` | Other visible (non-serving) towers. Useful for coverage mapping and handover analysis |
| `aggregation` | Carrier Aggregation info — when multiple frequency bands are bonded for higher speed |

---

## CarrierInfo

| Field | What it means |
|---|---|
| `carrierName` | Human-readable operator name (e.g. "Vodafone", "AT&T", "Azercell") |
| `mcc` | **Mobile Country Code** — 3-digit number identifying the country (e.g. `310` = USA, `400` = Azerbaijan) |
| `mnc` | **Mobile Network Code** — 2–3 digit number identifying the carrier within the country |
| `operatorId` | MCC + MNC combined into one string (e.g. `"31026"` = T-Mobile USA). Used as a globally unique carrier key |
| `countryIso` | 2-letter ISO 3166 country code from the SIM card (e.g. `"us"`, `"az"`, `"gb"`) |

---

## NrState

| Value | What it means |
|---|---|
| `NONE` | No 5G active. Device is on LTE or older technology |
| `NSA` | **Non-Standalone** — 5G NR secondary cell helps an LTE connection carry data. LTE still handles signaling and control. The most common "5G" deployment in 2023–2024 |
| `SA` | **Standalone** — True 5G. NR handles both control signaling and data without needing LTE as an anchor |

---

## CellRadioSnapshot

### Timing

| Field | What it means |
|---|---|
| `timestampOffsetMs` | How many milliseconds ago this cell reading was captured by the OS. `0` = just collected. Large values (> 5000 ms) mean the OS is returning stale cached data |

---

### Cell Identity
*Who this tower is. Most fields are RAT-specific — expect nulls for inapplicable RATs.*

| Field | RATs | What it means |
|---|---|---|
| `cellId` | LTE | **Cell Identity (CI)** — uniquely identifies an LTE cell sector globally when combined with MCC+MNC+TAC. Range: 0–268,435,455 |
| `cid` | GSM, WCDMA, TD-SCDMA | **Cell ID** — identifies the cell within a Location Area. Shorter range than LTE CI |
| `nci` | NR (5G) | **NR Cell Identity** — 36-bit ID for 5G cells. Very large range, hence `Long` |
| `lac` | GSM, WCDMA | **Location Area Code** — groups many cells together (like a postal district). Used in 2G/3G paging |
| `tac` | LTE, NR | **Tracking Area Code** — the LTE/NR equivalent of LAC |
| `pci` | LTE, NR | **Physical Cell ID** — a short local number (0–503) used to distinguish neighboring cells on the same frequency. Not globally unique |
| `psc` | WCDMA | **Primary Scrambling Code** — 3G equivalent of PCI (0–511) |
| `bsic` | GSM | **Base Station Identity Code** — 2G equivalent of PCI. Helps phones distinguish adjacent GSM cells |
| `band` | All | **Frequency band number** — e.g. Band 3 (1800 MHz), Band 20 (800 MHz), Band 78 (3.5 GHz 5G). Standardized by 3GPP |
| `arfcn` | LTE | **EARFCN** (E-UTRA Absolute Radio Frequency Channel Number) — pinpoints the exact LTE frequency within a band |
| `uarfcn` | WCDMA, TD-SCDMA | **UARFCN** (UTRA Absolute Radio Frequency Channel Number) — 3G equivalent of EARFCN |
| `nrarfcn` | NR (5G) | **NR-ARFCN** — 5G equivalent of EARFCN. Range: 0–3,279,165 |

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

## SecondaryCell

Fields are a subset of [CellRadioSnapshot] describing additional cells visible during carrier aggregation scanning. Carrier aggregation combines these with the serving cell for higher throughput.

| Field | What it means |
|---|---|
| `band` | Frequency band number of this secondary component carrier |
| `earfcn` | LTE frequency channel (identifies exact LTE frequency) |
| `nrarfcn` | NR frequency channel |
| `pci` | Physical Cell ID — distinguishes this cell from neighbors on the same frequency |
| `rsrp` | Signal strength (dBm) |
| `rsrq` | Signal quality (dB) |
| `sinr` | Signal-to-noise (dB) |
| `asuLevel` | Android's normalized signal unit |
| `dbm` | Unified signal strength |
| `bandwidthMhz` | Channel width of this component carrier |

---

## CarrierAggregationInfo

| Field | What it means |
|---|---|
| `active` | Whether carrier aggregation is currently active. `null` = cannot be determined from the public Android API. Seeing secondary cells in scan results is necessary but not sufficient to confirm active aggregation |
| `secondaryCells` | List of secondary component carriers seen alongside the serving cell |

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