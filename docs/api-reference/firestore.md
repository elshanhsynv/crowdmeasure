Available

# Firestore API

Create the provider using a host-configured Firebase instance:

```kotlin
val uploader = CrowdMeasureFirestore.create(
    FirebaseFirestore.getInstance(),
)
```

The provider implements `MeasurementUploader`, preserves the existing measurement document contract, and maps Firebase failures into backend-neutral upload errors.

`CrowdMeasureFirestore.createCallUploader(firestore)` implements `CallUploader` and preserves the existing `calls/{sessionId}/samples` contract.
