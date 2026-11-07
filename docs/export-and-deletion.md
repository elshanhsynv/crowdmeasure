# Export and Deletion

```kotlin
sdk.data.exportMeasurements(lastN = 100)
sdk.data.deleteAllMeasurements()
sdk.data.pruneExpiredMeasurements()
```

Export returns a content `Uri` through the SDK file provider. Deletion removes all stored measurements. Pruning removes measurements older than the configured retention period and is used by background cleanup.

`CallSamplingClient.exportSessions(lastN)` and `deleteAll()` independently export and delete call sessions and samples.

Export files are temporary. They are written to the app cache and deleted one hour after they are created: on the next export, during `pruneExpiredMeasurements()` (measurement exports), or when the next call session starts (call exports). Share the returned `Uri` right away; a share target that tries to read it more than an hour later will find it gone. `deleteAllMeasurements()` and `CallSamplingClient.deleteAll()` remove all export files immediately.
