Available

# Installation

The SDK currently uses local Gradle modules and supports Android API 29+.

```kotlin
dependencies {
    implementation(project(":crowdmeasure-sdk-core"))
    implementation(project(":crowdmeasure-sdk-background")) // optional
    implementation(project(":crowdmeasure-sdk-upload")) // optional
    implementation(project(":crowdmeasure-sdk-firestore")) // optional
    implementation(project(":crowdmeasure-sdk-calls")) // optional
}
```

Core remains free of Hilt, Compose, Firebase, Crashlytics, WorkManager, and call sampling. Upload and calls add WorkManager but no Firebase. Only the Firestore module depends on Firebase.
