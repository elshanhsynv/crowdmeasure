Available

# Manual Measurements

`sdk.measurements.runAndSave()` collects and persists one measurement. Successful results contain the saved `Measurement`; failures use the typed core error model.

```kotlin
when (val result = sdk.measurements.runAndSave()) {
    is CrowdMeasureResult.Success -> show(result.value)
    is CrowdMeasureResult.Failure -> handle(result.error)
}
```

Observe data with `observeLatest()` and `observeHistory(limit)`, or retrieve one item with `getById(id)`.
