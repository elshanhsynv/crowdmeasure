# Background Collection

Add `:crowdmeasure-sdk-background`, then install it in `Application.onCreate`:

```kotlin
val sdk = CrowdMeasureSdk.create(applicationContext)
val background = CrowdMeasureBackground.install(
    context = applicationContext,
    sdk = sdk,
    config = BackgroundConfig(
        preferencesName = "crowdmeasure_sdk_background",
        defaultIntervalMinutes = 60,
        defaultWifiOnly = false,
    ),
)
```

Installation only registers the SDK instance for workers. It schedules nothing. The default
preferences name preserves compatibility with existing SDK installations. Repeated equivalent
installation is idempotent; conflicting installation fails with `IllegalStateException`.

```kotlin
background.enable(intervalMinutes = 60, wifiOnly = false)
background.enqueueRunNow()
background.reschedule()
background.disable()
```

Intervals must be between 20 minutes and 7 days. Enabling schedules periodic local measurements and daily retention cleanup. Disabling cancels all SDK-owned background work. Immediate runs respect the configured network constraint.

Workers require the process to install the runtime before execution. Missing installation is recorded as `NOT_INSTALLED`. Work names are namespaced under `com.crowdmeasure.sdk.background`.

When core MNO restriction is enabled and the default data SIM is not eligible, a measurement worker records `SKIPPED_TARGET_MNO_NOT_ELIGIBLE` and completes without retrying. Retention cleanup still runs normally.

Defaults: preferences file `crowdmeasure_sdk_background`, enabled setting `true`, interval `60` minutes, Wi-Fi-only `false`, allowed interval `20` minutes to 7 days. Installing still schedules nothing until `enable()` is called.
