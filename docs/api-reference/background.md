# Background API

- `CrowdMeasureBackground.install(context, sdk, config)` registers the runtime and returns `BackgroundCollectionClient`.
- `BackgroundConfig` controls the preferences name and default interval/Wi-Fi policy.
- `enable(intervalMinutes, wifiOnly)` validates, persists, and schedules.
- `disable()` cancels periodic, immediate, and cleanup work.
- `enqueueRunNow()` enqueues one constrained measurement.
- `reschedule()` repairs work from persisted settings.
- `observeSettings()` and `observeStatus()` expose flows.

Public status types are `BackgroundCollectionSettings`, `BackgroundCollectionStatus`, `BackgroundRun`, `BackgroundWorkState`, `BackgroundRunOutcome`, and `BackgroundRunCode`.

Defaults: `crowdmeasure_sdk_background`, 60-minute interval, Wi-Fi-only off, enabled setting true, interval range 20 minutes to 7 days.
