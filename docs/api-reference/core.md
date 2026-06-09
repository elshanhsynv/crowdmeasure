Available

# Core API

- `CrowdMeasureSdk.create(context, config, measurementStore, settingsStore)` creates an instance.
- `MeasurementClient`: `runAndSave`, `observeLatest`, `observeHistory`, `getById`.
- `DataClient`: `exportMeasurements`, `deleteAllMeasurements`, `pruneExpiredMeasurements`.
- `SettingsClient`: `observeSettings`, `setEndpointUrl`, `setRetentionDays`.
- `RequirementsClient`: `evaluateManualMeasurement`.
- `CellularSnapshotClient`: narrow cellular snapshot collection used by optional SDK modules.
- `MeasurementStore` and `CrowdMeasureSettingsStore` support compatibility adapters.
- `MeasurementQueueClient` exposes backend-neutral pending/failed queue operations for optional upload modules.

All mutating public operations return typed `CrowdMeasureResult` values.
