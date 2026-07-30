# Calls API

`CrowdMeasureCalls.install(...)` registers the optional calls runtime and schedules or starts nothing.

`CallSamplingClient` exposes cellular and generic VoIP toggles, explicit activation, requirements/status flows, session and sample history, export, deletion, and the advanced upload queue integration contract.

`CallSession.simCarriers` contains carrier/SIM metadata captured for the call. `CallCellSample` contains the per-sample cellular snapshot, optional best-effort location, and optional data-usage delta/rate snapshot.

`crowdmeasure-calls-upload` exposes independent call upload settings, scheduling, status, and immediate upload execution. It is not required by local-only call sampling consumers.

`CallStore`, `CallUploader`, and `CallInstallationIdProvider` are advanced integration contracts for existing storage and backend identity.

All mutating operations return `CallSamplingResult`.

Defaults: calls database `crowdmeasure_calls.db`, calls preferences `crowdmeasure_sdk_calls`, 5-second sample interval, 7-day retention, cellular/VoIP settings true. Call-upload defaults are `crowdmeasure_sdk_calls_upload`, batch size 50, 60-minute interval, Wi-Fi-only on, disabled.
