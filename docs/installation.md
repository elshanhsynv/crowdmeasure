Available

# Installation

The SDK supports Android API 29+. Use local modules during development or the `0.1.0` Maven Local preview artifacts.

```kotlin
dependencies {
    implementation(project(":crowdmeasure-sdk-core"))
    implementation(project(":crowdmeasure-sdk-background")) // optional
    implementation(project(":crowdmeasure-sdk-measurements-upload")) // optional measurement uploads
    implementation(project(":crowdmeasure-sdk-measurements-upload-api")) // measurement uploader providers only
    implementation(project(":crowdmeasure-sdk-firestore-measurements")) // optional
    implementation(project(":crowdmeasure-sdk-calls")) // optional
    implementation(project(":crowdmeasure-sdk-calls-upload")) // optional
    implementation(project(":crowdmeasure-sdk-firestore-calls")) // optional
}
```

Published coordinates use group `com.crowdmeasure`, artifact names without `sdk`, and version `0.1.0`.

Core and calls remain free of Hilt, Compose, Firebase, Crashlytics, and WorkManager. Only upload scheduling artifacts use WorkManager. Firestore artifacts contain provider implementations only.
