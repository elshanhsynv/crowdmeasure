Available

# Background Collection

Add `:crowdmeasure-sdk-background`, then install it in `Application.onCreate`:

```kotlin
val sdk = CrowdMeasureSdk.create(applicationContext)
val background = CrowdMeasureBackground.install(applicationContext, sdk)
```

Installation only registers the SDK instance for workers. It schedules nothing.

```kotlin
background.enable(intervalMinutes = 60, wifiOnly = false)
background.enqueueRunNow()
background.reschedule()
background.disable()
```

Intervals must be between 20 minutes and 7 days. Enabling schedules periodic local measurements and daily retention cleanup. Disabling cancels all SDK-owned background work. Immediate runs respect the configured network constraint.

Workers require the process to install the runtime before execution. Missing installation is recorded as `NOT_INSTALLED`. Work names are namespaced under `com.yourcompany.crowdmeasure.sdk.background`.
