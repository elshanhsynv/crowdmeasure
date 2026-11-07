# Cell radio variants

The `radio` object changes with the technology, selected by `type`. The wrapper around it (`timestampOffsetMs`, `dbm`, `asuLevel`, `level`, `subscriptionId`, `isRegistered`) is the same for every variant.

## GSM

```json
{
  "type": "gsm",
  "cellId": 4321,
  "lac": 14120,
  "bsic": 22,
  "arfcn": 64,
  "rssiDbm": -113,
  "timingAdvance": 0
}
```

- GSM only gives RSSI. There is no RSRP or quality value.
- `timingAdvance` is in GSM symbol periods, not LTE units.

## WCDMA

```json
{
  "type": "wcdma",
  "cellId": 3341,
  "lac": 1204,
  "psc": 128,
  "uarfcn": 10700,
  "ecNoDb": -7
}
```

- `ecNoDb` is signal quality, not strength.
- The wrapper `dbm` holds RSCP.

## LTE

```json
{
  "type": "lte",
  "cellId": 105496462,
  "tac": 14121,
  "pci": 48,
  "earfcn": 1400,
  "bands": [3],
  "bandwidthKhz": 20000,
  "rsrpDbm": -64,
  "rsrqDb": -7,
  "rssnrDb": 14,
  "rssiDbm": -51,
  "cqi": 11,
  "timingAdvance": 2,
  "mimoLayers": 2
}
```

- `rsrpDbm` is the main strength value. The wrapper `dbm` copies it.
- `rsrqDb`, `rssnrDb` and `cqi` are quality values.
- `timingAdvance` is the raw Android value, not meters.
- An empty `bands` list means the bands were not reported.

## NR

```json
{
  "type": "nr",
  "cellId": 1234567890,
  "tac": 14121,
  "pci": 512,
  "nrarfcn": 628500,
  "bands": [78],
  "ss": {
    "rsrpDbm": -101,
    "rsrqDb": -12,
    "sinrDb": 9
  },
  "csi": {
    "rsrpDbm": -103,
    "rsrqDb": -13,
    "sinrDb": 7
  },
  "bandwidthKhz": 100000,
  "mimoLayers": 2
}
```

- `ss` is the synchronization-signal measurement. `csi` is the channel-state measurement.
- `cellId` is a `Long` because NR cell IDs are wider than LTE's.
- The wrapper `dbm` holds SS-RSRP.
