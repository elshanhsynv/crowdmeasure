Available

# Call Sampling

Add `:crowdmeasure-sdk-calls`, provide a notification icon, and install the runtime:

```kotlin
val calls = CrowdMeasureCalls.install(
    context = applicationContext,
    sdk = crowdMeasureSdk,
    config = CallSamplingConfig(notificationIconResId = R.drawable.crowdmeasure),
    uploader = CrowdMeasureFirestore.createCallUploader(FirebaseFirestore.getInstance()),
)
```

Installation starts nothing. Persisted enabled features are restored only after the host calls `activateEnabledFeatures()` during application startup.

Cellular sampling uses phone-state events. Generic VoIP sampling observes Android audio communication mode on a best-effort basis. Both collect cellular snapshots in a foreground location service. Battery-optimization exemption improves reliability but is not a start requirement.

Call uploads have independent enabled, interval, and Wi-Fi-only settings. Default storage is `crowdmeasure_calls.db`; existing apps can provide a `CallStore`.

The SDK declares permissions but never requests them, opens settings, or presents consent UI. WhatsApp notification-listener detection is not included; legacy WhatsApp source enum values remain readable.
