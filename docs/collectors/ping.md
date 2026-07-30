# Ping Collector

Measures TCP-connect latency to the configured endpoint host.

This is not ICMP ping. Android apps usually cannot rely on raw ICMP, so the SDK opens short TCP socket connections to the endpoint address and records average, min, max, jitter, and failed-attempt percentage.

The values are stored in `PerformanceInfo` as:

- `pingAvgMs`
- `pingMinMs`
- `pingMaxMs`
- `pingJitterMs`
- `pingPacketLossPct`

Successful sample:

```json
{
  "pingAvgMs": 38,
  "pingMinMs": 31,
  "pingMaxMs": 52,
  "pingJitterMs": 8,
  "pingPacketLossPct": 0.0
}
```
