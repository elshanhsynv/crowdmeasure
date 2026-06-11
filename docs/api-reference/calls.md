Available

# Calls API

`CrowdMeasureCalls.install(...)` registers the optional calls runtime and schedules or starts nothing.

`CallSamplingClient` exposes cellular and generic VoIP toggles, explicit activation, requirements/status flows, session and sample history, export, deletion, and the advanced upload queue integration contract.

`crowdmeasure-calls-upload` exposes independent call upload settings, scheduling, status, and immediate upload execution. It is not required by local-only call sampling consumers.

`CallStore`, `CallUploader`, and `CallInstallationIdProvider` are advanced integration contracts for existing storage and backend identity.

All mutating operations return `CallSamplingResult`.
