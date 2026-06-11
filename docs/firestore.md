Available

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

`CrowdMeasureFirestoreCalls.create(firestore)` preserves the existing `calls/{sessionId}` documents and `samples/{sampleId}` subcollection contract.
