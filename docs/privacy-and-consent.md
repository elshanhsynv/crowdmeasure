Available

# Privacy and Consent

The SDK collects device, network, performance, and optional permission-dependent environment data. Uploads occur only after the host installs an uploader and explicitly enables uploads.

Raw public IP addresses are never exposed, exported, or uploaded. The default policy stores a per-install salted SHA-256 hash under the legacy serialized `publicIp` field name so existing wire consumers remain compatible. ISP and ASN metadata remain available.

Consent UI, consent records, permission explanations, runtime permission requests, privacy-policy presentation, and deciding when uploads may be enabled are host-app responsibilities.

Call sampling collects cellular snapshots during cellular and generic VoIP calls. Hosts must disclose and explicitly enable it. The SDK does not inspect call audio or include WhatsApp notification-listener detection.
