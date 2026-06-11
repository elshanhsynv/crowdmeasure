Available

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
        publicIpPolicy = PublicIpPolicy.HASHED,
        logger = CrowdMeasureLogger.NONE,
    ),
)
```

Initialization creates clients and stores but does not collect, schedule, upload, request permissions, or show UI. Hosts preserving an existing schema may provide `MeasurementStore` and `CrowdMeasureSettingsStore` adapters.

Optional background, measurement-upload, calls, and call-upload runtimes must be installed separately. Installing a runtime schedules nothing. Equivalent repeated installation is idempotent; conflicting installation throws `IllegalStateException`.

Installing the optional calls runtime also starts and schedules nothing. Call `activateEnabledFeatures()` during startup to restore only previously enabled call features.
