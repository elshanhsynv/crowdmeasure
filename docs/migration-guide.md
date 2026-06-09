Available

# Migration Guide

## 0.1.0 Unreleased

1. Add `:crowdmeasure-sdk-core` and optionally `:crowdmeasure-sdk-background`.
2. Create one `CrowdMeasureSdk` instance, providing compatibility stores when existing data must remain available.
3. Replace app-owned measurement orchestration with `MeasurementClient` and `DataClient`.
4. Install `CrowdMeasureBackground` in `Application.onCreate`.
5. Explicitly migrate existing enabled/interval/Wi-Fi-only values, then call `enable()` or `disable()`.
6. Remove app-owned measurement and retention workers while retaining upload/call workers until their later SDK phases.
7. Install `CrowdMeasureUploads` with a backend provider and migrate the existing measurement-upload enabled state.
8. Remove app-owned measurement upload workers while retaining call-session uploads until the calls phase.
9. Install `CrowdMeasureCalls` with a required notification icon and a compatibility `CallStore` when preserving existing call history.
10. Migrate cellular, VoIP, and call-upload enabled settings once, then call `activateEnabledFeatures()` during startup.
11. Remove app-owned call services, receivers, monitors, upload workers, and Firestore repository.

Installing background support alone never enables collection.
