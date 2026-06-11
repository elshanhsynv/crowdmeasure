Available

# Firestore API

Create the provider using a host-configured Firebase instance:

```kotlin
val uploader = CrowdMeasureFirestoreMeasurements.create(
    FirebaseFirestore.getInstance(),
)
```

The provider implements `MeasurementUploader`, preserves the existing measurement document contract, and maps Firebase failures into backend-neutral upload errors.

`CrowdMeasureFirestoreCalls.create(firestore)` implements `CallUploader` and preserves the existing `calls/{sessionId}/samples` contract. Measurement-only consumers do not receive calls dependencies.
