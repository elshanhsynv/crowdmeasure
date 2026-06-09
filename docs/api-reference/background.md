Available

# Background API

- `CrowdMeasureBackground.install(context, sdk)` registers the runtime and returns `BackgroundCollectionClient`.
- `enable(intervalMinutes, wifiOnly)` validates, persists, and schedules.
- `disable()` cancels periodic, immediate, and cleanup work.
- `enqueueRunNow()` enqueues one constrained measurement.
- `reschedule()` repairs work from persisted settings.
- `observeSettings()` and `observeStatus()` expose flows.

Public status types are `BackgroundCollectionSettings`, `BackgroundCollectionStatus`, `BackgroundRun`, `BackgroundWorkState`, `BackgroundRunOutcome`, and `BackgroundRunCode`.
