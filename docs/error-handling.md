# Error Handling

Core operations return `CrowdMeasureResult.Success` or `CrowdMeasureResult.Failure` with `CrowdMeasureError`, including unsupported Android, collection, persistence, export, invalid configuration, and `DefaultDataMnoNotEligible` failures. The latter includes `MATCHED`, `MISMATCHED`, or `UNAVAILABLE` eligibility details.

Background operations return `BackgroundResult` with invalid-interval, not-enabled, or scheduling failures. Observe `BackgroundCollectionStatus.lastRun` for worker outcomes and codes such as `OK`, `SKIPPED_RECENT_RUN`, `SKIPPED_TARGET_MNO_NOT_ELIGIBLE`, `NOT_INSTALLED`, and `COLLECTION_FAILED`.

Upload operations return `MeasurementUploadResult`. Typed errors distinguish disabled or missing runtime state, transient backend failures, backend rejection, serialization failure, persistence failure, and scheduling failure.

Call operations return `CallSamplingResult`. Typed errors cover disabled or missing runtime state, requirements, default-data-MNO eligibility, backend failures, persistence, export, configuration, and scheduling. Status codes distinguish `TARGET_MNO_NOT_DEFAULT` from `TARGET_MNO_UNAVAILABLE`.
