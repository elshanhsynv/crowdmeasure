# Environment Collector

Builds the environment section of a measurement.

It coordinates location, public IP metadata, diagnostics, and data-usage collection so one measurement has a single environment snapshot. Hosts usually care about the result fields, not this collector directly.

If one optional source fails or is unavailable, the measurement can still continue with partial environment data.

Successful sample:

```json
{
  "location": {
    "lat": 40.4093,
    "lon": 49.8671,
    "accuracyMeters": 18.5
  },
  "network": {
    "transport": "WIFI",
    "validatedInternet": true,
    "captivePortal": false,
    "vpn": false,
    "metered": false
  },
  "device": {
    "batteryPct": 82,
    "charging": false,
    "screenOn": true
  }
}
```
