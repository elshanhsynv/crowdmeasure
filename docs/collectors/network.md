# Network Collector

Identifies the active network at measurement time.

It reports the active transport, such as Wi-Fi, cellular, VPN, ethernet, or unknown, plus Android connectivity information where available. This is the top-level answer to "what network was the device using?"

Used in manual and background measurements.

Successful sample:

```json
{
  "transport": "WIFI",
  "validatedInternet": true,
  "captivePortal": false,
  "vpn": false,
  "metered": false,
  "wifi": {
    "ssid": "Office WiFi",
    "rssiDbm": -54
  },
  "cell": null
}
```
