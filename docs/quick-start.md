# Quick Start

Create and retain one SDK instance, usually from `Application.onCreate`:

```kotlin
val sdk = CrowdMeasureSdk.create(applicationContext)
```

Check prerequisites. The SDK reports what is missing; the host app owns permission UI and consent.

```kotlin
val requirements = sdk.requirements.evaluateManualMeasurement()
```

Run and save one measurement:

```kotlin
val result = sdk.measurements.runAndSave()
```

Observe saved data:

```kotlin
sdk.measurements.observeLatest()
sdk.measurements.observeHistory(limit = 100)
```

For optional scheduling, install the background runtime in `Application.onCreate`, then explicitly enable it. See [background collection](background-collection.md).

For optional uploads, install `CrowdMeasureUploads` with a backend provider and explicitly call `enable()`. See [uploads](uploads.md).

For optional call sampling, install `CrowdMeasureCalls` with a required notification icon, then call `activateEnabledFeatures()` during startup. See [call sampling](call-sampling.md).
