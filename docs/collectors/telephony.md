# Telephony Collector

Adds cellular network details.

It records carrier identity, radio access technology, 5G state, serving cell, neighboring cells, signal strength, quality, and carrier aggregation details when Android exposes them.

Field availability varies heavily by Android version, chipset, carrier, SIM state, and permissions. Use the [cell field glossary](../cell-field-glossary.md) for plain-English field definitions.

Successful sample:

```json
{
  "simCarriers": [
    {
      "carrierName": "Example Mobile",
      "mcc": "400",
      "mnc": "01",
      "countryIso": "az",
      "isDefaultData": true
    }
  ],
  "rat": "LTE",
  "nrState": "none",
  "dataNetworkType": "LTE",
  "voiceNetworkType": "LTE",
  "roaming": false,
  "serving": {
    "cellId": 12345678,
    "tac": 321,
    "pci": 42,
    "band": 3,
    "arfcn": 1300,
    "rsrpDbm": -86,
    "rsrqDb": -9,
    "sinrDb": 18,
    "dbm": -86,
    "timingAdvance": 4,
    "bandwidthMhz": 20
  },
  "neighbors": []
}
```
