# Collectors

Collectors are the small SDK components that read one part of a measurement. Hosts normally do not call them directly; they run through `sdk.measurements.runAndSave()`.

| Collector | What it adds |
|---|---|
| [Device info](device-info.md) | Device/app metadata |
| [Diagnostics](diagnostics.md) | Battery, charging, screen, and device environment |
| [Environment](environment.md) | Location, IP, diagnostics, and data-usage group |
| [Network](network.md) | Active transport and connection state |
| [Wi-Fi](wifi.md) | Wi-Fi link details when connected |
| [Telephony](telephony.md) | Cellular carrier, RAT, serving cell, neighbors, and signal |
| [Location](location.md) | Best-effort device location when the host grants permission |
| [IP](ip.md) | Public IP and ISP/ASN metadata when enabled |
| [Data usage](data-usage.md) | App-level network byte deltas between samples |
| [Performance](performance.md) | HTTP timing and probe quality |
| [Ping](ping.md) | TCP-connect latency samples used by performance metrics |

Missing permissions or unavailable platform data usually produce `null` fields rather than crashing the measurement.
