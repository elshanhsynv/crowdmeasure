Available

# Changelog

## 0.1.0 Unreleased

- Added core manual measurement, storage, settings, requirements, export, deletion, and retention APIs.
- Added optional WorkManager-based background collection and retention cleanup.
- Added typed background settings/status/results and namespaced unique work.
- Migrated the CrowdMeasure app and sample host to the shared SDK engine.
- Added root SDK documentation.
- Added backend-neutral measurement upload queue, status, and WorkManager scheduling.
- Added optional Firestore measurement uploader while preserving the existing document contract.
- Migrated the CrowdMeasure app's measurement uploads to the SDK.
- Added optional cellular and generic VoIP call sampling, call storage, history, export, deletion, and independent call-upload scheduling.
- Added Firestore call uploads while preserving the existing call/session document contract.
- Migrated the CrowdMeasure app's call runtime to the SDK while preserving its version-4 shared database.
