# Wi-Fi Collector

Adds Wi-Fi details when the active connection is Wi-Fi.

It can include SSID/BSSID availability, link speed, RSSI, frequency, and other Android Wi-Fi fields depending on OS version, permissions, and device behavior.

Android may hide or redact Wi-Fi fields without location permission or location services.

Successful sample:

```json
{
  "bssidHash": "b1f9c8a2f5a4",
  "ssid": "Office WiFi",
  "standard": "WIFI_6",
  "frequencyMhz": 5180,
  "channelWidthMhz": 80,
  "rssiDbm": -54,
  "linkSpeedMbps": 866,
  "txLinkSpeedMbps": 780,
  "rxLinkSpeedMbps": 866
}
```
