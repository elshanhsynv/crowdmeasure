# Manual Measurements

`sdk.measurements.runAndSave()` collects and persists one measurement. Successful results contain the saved `Measurement`; failures use the typed core error model.

```kotlin
when (val result = sdk.measurements.runAndSave()) {
    is CrowdMeasureResult.Success -> show(result.value)
    is CrowdMeasureResult.Failure -> handle(result.error)
}
```

Observe data with `observeLatest()` and `observeHistory(limit)`, or retrieve one item with `getById(id)`.

When a host configures `requiredDefaultDataMnoId`, `runAndSave()` returns `CrowdMeasureError.DefaultDataMnoNotEligible` before starting any collector when the default data SIM does not match or cannot be resolved.

Each run uses the SDK collectors for device info, active network, Wi-Fi/cellular details, location when permitted, public IP metadata when enabled, data usage deltas, HTTP performance, and TCP-connect ping metrics. See [collectors](collectors/README.md).
