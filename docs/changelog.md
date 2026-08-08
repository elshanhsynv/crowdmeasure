# Changelog

## 0.1.0 Unreleased

- Added per-sample data-usage snapshots to call sampling and moved stable carrier/SIM metadata to call sessions.
- Added optional location snapshots to cellular and generic VoIP call samples without changing existing database schemas.
- Adopted permanent namespace `com.crowdmeasure.sdk` and Maven group `com.crowdmeasure`.
- Split provider contracts, WorkManager runtimes, and Firestore measurement/call implementations into dependency-minimal artifacts.
- Removed WorkManager and the battery-optimization permission from local call sampling.
- Added independent call-upload controls and partial measurement/call upload outcomes.
- Replaced SDK-global Timber installation with a no-op-by-default host logger.
- Added configurable collector and probe policies and removed unused throughput configuration.
- Public IP collection now stores the raw public IP again while preserving the serialized `publicIp` field name.
- Added Maven Local publication, sample-host Maven switching, dependency checks, and aggregate `sdkCheck`.
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
- Migrated the CrowdMeasure app's call runtime to the SDK while preserving and migrating its shared database.
- Moved overview/glossary docs under `docs/`, removed status-prefix headers, and added collector documentation including TCP-connect ping metrics.
- Documented default SDK configuration values and added collector success JSON examples.
- Added custom backend integration guidance for measurement and call uploaders.
