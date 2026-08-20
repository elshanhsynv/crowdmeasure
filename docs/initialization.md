# Initialization

```kotlin
val sdk = CrowdMeasureSdk.create(
    context = applicationContext,
    config = CrowdMeasureConfig(
        databaseName = "crowdmeasure_sdk.db",
        preferencesName = "crowdmeasure_sdk_preferences",
        defaultEndpointUrl = "https://www.google.com/",
        defaultRetentionDays = 7,
        collectors = CollectorConfig(),
        publicIpPolicy = PublicIpPolicy.RAW,
        logger = CrowdMeasureLogger.NONE,
        // Optional: only collect when this is the default data SIM's home MCC+MNC.
        requiredDefaultDataMnoId = "40001",
    ),
)
```

Initialization creates clients and stores but does not collect, schedule, upload, request permissions, or show UI. Hosts preserving an existing schema may provide `MeasurementStore` and `CrowdMeasureSettingsStore` adapters.

`requiredDefaultDataMnoId` is optional and defaults to `null`. When set, new manual, background, and call capture runs only when the default data SIM's home MCC+MNC matches. The SDK does not use carrier display names or the currently registered roaming network. Set the value to `null` or omit it to remove the restriction.

Optional background, measurement-upload, calls, and call-upload runtimes must be installed separately. Installing a runtime schedules nothing. Equivalent repeated installation is idempotent; conflicting installation throws `IllegalStateException`.

Installing the optional calls runtime also starts and schedules nothing. Call `activateEnabledFeatures()` during startup to restore only previously enabled call features.

See [default configuration](default-configuration.md) for every default value.
