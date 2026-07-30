# Performance Collector

Runs lightweight HTTP probes against the configured endpoint.

It records DNS timing, connection timing, TLS timing, time to first byte, HTTP latency, jitter, probe failures, stalls, HTTP status, protocol, and optional server-region headers.

It also calls the [ping collector](ping.md) and stores TCP-connect latency metrics in `PerformanceInfo`.

The endpoint should be HTTPS and should be stable enough for repeat measurements.

Successful sample:

```json
{
  "endpointId": "https://www.google.com/",
  "dnsMs": 18,
  "connectMs": 42,
  "tlsMs": 65,
  "ttfbAvgMs": 96,
  "ttfbP95Ms": 140,
  "httpLatencyAvgMs": 118,
  "httpLatencyP95Ms": 165,
  "jitterMs": 12,
  "pingAvgMs": 38,
  "pingMinMs": 31,
  "pingMaxMs": 52,
  "pingJitterMs": 8,
  "pingPacketLossPct": 0.0,
  "probeFailurePct": 0.0,
  "probesAttempted": 8,
  "probesSucceeded": 8,
  "probesFailed": 0,
  "httpStatus": 200,
  "protocol": "HTTP2"
}
```
