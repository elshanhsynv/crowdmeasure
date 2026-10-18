# Privacy and Consent

The SDK collects device, network, performance, and optional permission-dependent environment data.

Several features are **enabled by default** in their persisted settings: background collection, measurement uploads, cellular call sampling, and VoIP call sampling. Installing a module schedules nothing on its own. However, any of the following acts on those defaults without a prior `enable()`:

- `reschedule()` and `enqueueRunNow()` on background collection.
- `reschedule()`, `uploadNow()`, and `enqueueUploadNow()` on measurement uploads.
- The cellular phone-state receiver, as soon as `READ_PHONE_STATE` and location are granted.

Hosts that need consent before collection must therefore call `disable()` (or `setCellularSamplingEnabled(false)` / `setVoipSamplingEnabled(false)`) until consent is given, and must not call the methods above before then.

Public IP collection stores, exports, and uploads the raw public IP under the legacy serialized `publicIp` field name when `PublicIpPolicy.RAW` is enabled. Hosts that do not want public IP collection should set `publicIpPolicy = PublicIpPolicy.DISABLED` or `collectors.publicIpEnabled = false`. ISP and ASN metadata remain available when the lookup succeeds.

Consent UI, consent records, permission explanations, runtime permission requests, privacy-policy presentation, and deciding when uploads may be enabled are host-app responsibilities.

Call sampling collects cellular and best-effort location snapshots during cellular and generic VoIP calls. Hosts must disclose it. Both call features default to enabled; disable them until the user has consented. The SDK does not inspect call audio or include WhatsApp notification-listener detection.
