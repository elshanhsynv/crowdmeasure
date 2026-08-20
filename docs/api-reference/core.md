# Core API

- `CrowdMeasureSdk.create(context, config, measurementStore, settingsStore)` creates an instance.
- `MeasurementClient`: `runAndSave`, `observeLatest`, `observeHistory`, `getById`.
- `DataClient`: `exportMeasurements`, `deleteAllMeasurements`, `pruneExpiredMeasurements`.
- `SettingsClient`: `observeSettings`, `setEndpointUrl`, `setRetentionDays`.
- `RequirementsClient`: `evaluateManualMeasurement`, `evaluateDefaultDataMno`.
- `CellularSnapshotClient`: narrow cellular snapshot collection used by optional SDK modules.
- `MeasurementStore` and `CrowdMeasureSettingsStore` support compatibility adapters.
- `MeasurementQueueClient` exposes backend-neutral pending/failed queue operations for optional upload modules.

All mutating public operations return typed `CrowdMeasureResult` values.

`PerformanceInfo` includes HTTP timing metrics and TCP-connect ping fields: average, min, max, jitter, and failed-attempt percentage.

`CrowdMeasureConfig.requiredDefaultDataMnoId` is an optional default-data-SIM home MCC+MNC gate. `DefaultDataMnoEligibility` reports `UNRESTRICTED`, `MATCHED`, `MISMATCHED`, or `UNAVAILABLE`; `MeasurementRequirements` includes it.

Core defaults: database `crowdmeasure_sdk.db`, preferences `crowdmeasure_sdk_preferences`, endpoint `https://www.google.com/`, retention 7 days, raw public IP policy, no-op logger, unrestricted MNO policy, 8 HTTP probes, 10-second probe timeout, all collectors enabled.
