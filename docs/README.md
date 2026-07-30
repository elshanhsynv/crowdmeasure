# CrowdMeasure SDK Documentation

CrowdMeasure is a local-first Android SDK for Android network measurements. This repo includes the SDK modules, a minimal sample host app, and the full CrowdMeasure app as a real integration reference.

Version: **0.1.0 Unreleased**.

## Feature Matrix

| Feature | Status | Module |
|---|---|---|
| Manual measurements, local storage, settings, export, deletion | Available | `:crowdmeasure-sdk-core` |
| Scheduled measurements and retention cleanup | Available | `:crowdmeasure-sdk-background` |
| Measurement uploader contracts | Available | `crowdmeasure-measurements-upload-api` |
| Measurement upload queue and scheduling | Available | `crowdmeasure-measurements-upload` |
| Firestore measurement uploads | Available | `crowdmeasure-firestore-measurements` |
| Cellular and generic VoIP call sampling and local data | Available | `crowdmeasure-calls` |
| Call upload scheduling | Available | `crowdmeasure-calls-upload` |
| Firestore call uploads | Available | `crowdmeasure-firestore-calls` |

## Guides

- [Repository overview](app-overview.md)
- [Installation](installation.md)
- [Quick start](quick-start.md)
- [Initialization](initialization.md)
- [Permissions](permissions.md)
- [Default configuration](default-configuration.md)
- [Collectors](collectors/README.md)
- [Cell field glossary](cell-field-glossary.md)
- [Manual measurements](manual-measurements.md)
- [Background collection](background-collection.md)
- [Data storage](data-storage.md)
- [Privacy and consent](privacy-and-consent.md)
- [Export and deletion](export-and-deletion.md)
- [Error handling](error-handling.md)
- [ProGuard and R8](proguard-r8.md)
- [Migration guide](migration-guide.md)
- [Changelog](changelog.md)
- [Core API](api-reference/core.md)
- [Background API](api-reference/background.md)
- [Upload API](api-reference/upload.md)
- [Firestore API](api-reference/firestore.md)
- [Calls API](api-reference/calls.md)
- [Uploads](uploads.md), [Firestore](firestore.md), [call sampling](call-sampling.md)
