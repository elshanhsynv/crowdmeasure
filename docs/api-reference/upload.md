# Upload API

- `CrowdMeasureUploads.install(context, sdk, uploader, installationIdProvider, config)` registers the upload runtime without scheduling work.
- `MeasurementUploadConfig` controls the preferences name, default batch size, interval, and Wi-Fi policy.
- `MeasurementUploadClient.enable(intervalMinutes, wifiOnly)` persists settings and schedules periodic uploads.
- `disable()`, `uploadNow()`, `enqueueUploadNow()`, and `reschedule()` control uploads.
- `observeQueue()` and `observeStatus()` expose queue and operational status flows.
- `MeasurementUploader` and partial `UploadBatchResult` outcomes are provided by dependency-minimal `crowdmeasure-measurements-upload-api`.
- Measurement upload scheduling/runtime APIs are published from `crowdmeasure-measurements-upload`.

Call uploads use the independent `crowdmeasure-calls-upload` artifact and `CallUploadClient`.

Upload operations return typed `MeasurementUploadResult` values. Supported intervals are 20 minutes through 7 days.

Defaults: `crowdmeasure_sdk_upload`, batch size 50, 60-minute interval, Wi-Fi-only off, upload setting true.
