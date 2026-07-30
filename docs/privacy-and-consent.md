# Privacy and Consent

The SDK collects device, network, performance, and optional permission-dependent environment data. Uploads occur only after the host installs an uploader and explicitly enables uploads.

Public IP collection stores, exports, and uploads the raw public IP under the legacy serialized `publicIp` field name when `PublicIpPolicy.RAW` is enabled. Hosts that do not want public IP collection should set `publicIpPolicy = PublicIpPolicy.DISABLED` or `collectors.publicIpEnabled = false`. ISP and ASN metadata remain available when the lookup succeeds.

Consent UI, consent records, permission explanations, runtime permission requests, privacy-policy presentation, and deciding when uploads may be enabled are host-app responsibilities.

Call sampling collects cellular and best-effort location snapshots during cellular and generic VoIP calls. Hosts must disclose and explicitly enable it. The SDK does not inspect call audio or include WhatsApp notification-listener detection.
