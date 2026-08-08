# Firestore

The optional Firestore module provides a `MeasurementUploader`:

```kotlin
val uploader = CrowdMeasureFirestoreMeasurements.create(
    firestore = FirebaseFirestore.getInstance(),
)
```

The host owns Firebase initialization and configuration. The provider preserves the existing `measurements` collection, measurement document ID, and fields:

- `install_id`
- `measurement_id`
- `transport`
- `run_date`
- `uploaded_at`
- `payload`

Firebase network and retryable Firestore failures become transient upload errors. Other Firebase failures become backend rejections.

`CrowdMeasureFirestoreCalls.create(firestore)` writes `calls/{sessionId}` documents and `samples/{sampleId}` subcollection records. Call documents include session-level `sim_carriers`. New call samples include optional `location` and `data_usage` maps.

Firestore is optional. Hosts can replace it with their own `MeasurementUploader` and `CallUploader`. See [custom backend integration](custom-backend.md).
