Available

# Export and Deletion

```kotlin
sdk.data.exportMeasurements(lastN = 100)
sdk.data.deleteAllMeasurements()
sdk.data.pruneExpiredMeasurements()
```

Export returns a content `Uri` through the SDK file provider. Deletion removes all stored measurements. Pruning removes measurements older than the configured retention period and is used by background cleanup.

`CallSamplingClient.exportSessions(lastN)` and `deleteAll()` independently export and delete call sessions and samples.
