# CrowdMeasure SDK Core

Manual, local-first network measurement SDK for Android 10 (API 29) and newer.

## Integration

```kotlin
dependencies {
    implementation(project(":crowdmeasure-sdk-core"))
}
```

Create one SDK instance in the host application:

```kotlin
val crowdMeasure = CrowdMeasureSdk.create(
    context = applicationContext,
    config = CrowdMeasureConfig(
        databaseName = "crowdmeasure_sdk.db",
        defaultEndpointUrl = "https://www.google.com/",
        defaultRetentionDays = 7,
    ),
)
```

Run and persist a measurement:

```kotlin
when (val result = crowdMeasure.measurements.runAndSave()) {
    is CrowdMeasureResult.Success -> use(result.value)
    is CrowdMeasureResult.Failure -> handle(result.error)
}
```

The host app owns permission requests. Inspect requirements with:

```kotlin
val requirements = crowdMeasure.requirements.evaluateManualMeasurement()
```

Core initialization does not schedule background work, upload data, start services, or require Hilt.

Full documentation: [docs/README.md](../docs/README.md)
