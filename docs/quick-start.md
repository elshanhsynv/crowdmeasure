Available

# Quick Start

Create and retain one SDK instance:

```kotlin
val sdk = CrowdMeasureSdk.create(applicationContext)
```

Check prerequisites, request any needed permissions in the host UI, then run:

```kotlin
val requirements = sdk.requirements.evaluateManualMeasurement()
val result = sdk.measurements.runAndSave()
```

For optional scheduling, install the background runtime in `Application.onCreate`, then explicitly enable it. See [background collection](background-collection.md).

For optional uploads, install `CrowdMeasureUploads` with a backend provider and explicitly call `enable()`. See [uploads](uploads.md).

For optional call sampling, install `CrowdMeasureCalls` with a required notification icon, then call `activateEnabledFeatures()` during startup. See [call sampling](call-sampling.md).
