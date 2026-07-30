# Installation

The SDK supports Android API 29+.

For this repo, the easiest integration path is to depend on the local Gradle modules. Start with core only, then add optional modules only when the host app needs them.

```kotlin
dependencies {
    // Required for manual measurements, local storage, settings, export, and deletion.
    implementation(project(":crowdmeasure-sdk-core"))

    // Optional: scheduled local measurements and retention cleanup.
    implementation(project(":crowdmeasure-sdk-background"))

    // Optional: measurement upload queue and backend contract.
    implementation(project(":crowdmeasure-sdk-measurements-upload"))
    implementation(project(":crowdmeasure-sdk-measurements-upload-api"))

    // Optional: Firestore measurement uploader.
    implementation(project(":crowdmeasure-sdk-firestore-measurements"))

    // Optional: cellular/generic VoIP call sampling and call uploads.
    implementation(project(":crowdmeasure-sdk-calls"))
    implementation(project(":crowdmeasure-sdk-calls-upload"))
    implementation(project(":crowdmeasure-sdk-firestore-calls"))
}
```

Maven Local preview coordinates use group `com.crowdmeasure`, artifact names without `sdk`, and version `0.1.0`. Example: `com.crowdmeasure:crowdmeasure-core:0.1.0`.

Core and calls remain free of Hilt, Compose, Firebase, Crashlytics, and WorkManager. Only upload scheduling artifacts use WorkManager. Firestore artifacts contain provider implementations only.
