# Diagnostics Collector

Adds device-environment details that can explain measurement quality.

Typical values include battery state, charging state, screen state, power-save state, and similar Android environment signals. These fields help interpret slow or failed probes without assuming the network is always the cause.

Used in manual and background measurements.

Successful sample:

```json
{
  "batteryPct": 82,
  "charging": false,
  "batterySaver": false,
  "screenOn": true,
  "dozeMode": false,
  "dataSaver": false,
  "thermalState": "NORMAL",
  "cpuUsagePct": null,
  "memoryUsagePct": 61.2
}
```
