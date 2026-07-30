# Location Collector

Adds best-effort location when the host app has granted location permission and location services are enabled.

The collector prefers quick existing/fused location data and falls back to a short active request only when needed. If location is unavailable, the measurement continues with location fields missing.

The SDK declares permissions where needed but never requests them. The host app owns permission UI and consent.

Successful sample:

```json
{
  "lat": 40.4093,
  "lon": 49.8671,
  "accuracyMeters": 18.5
}
```
