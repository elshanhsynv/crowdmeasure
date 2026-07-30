# IP Collector

Adds public network metadata when enabled.

It can resolve the public IP plus ISP/ASN-style metadata using the configured public-IP policy. If the host disables public IP collection, these fields are omitted.

The current SDK can store raw public IP when configured for that behavior. Hosts should disclose this clearly if uploads or exports are enabled.

Successful sample:

```json
{
  "publicIp": "203.0.113.24",
  "ispName": "AS64500 Example ISP",
  "asn": 64500
}
```
