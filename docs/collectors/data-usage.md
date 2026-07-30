# Data Usage Collector

Adds app-level network byte deltas.

The collector reads Android traffic counters and records how many bytes changed since the previous sample in the same scope. This is useful for call samples and repeated measurements because it shows network activity around the sample window.

Counters can reset after process restart or OS counter reset, so consumers should treat missing or reset values as normal.

Successful sample:

```json
{
  "dlMB": 1.42,
  "ulMB": 0.18,
  "dlKbps": 580.6,
  "ulKbps": 73.5
}
```
