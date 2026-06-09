Available

# Upload API

- `CrowdMeasureUploads.install(context, sdk, uploader, installationIdProvider)` registers the upload runtime without scheduling work.
- `MeasurementUploadClient.enable(intervalMinutes, wifiOnly)` persists settings and schedules periodic uploads.
- `disable()`, `uploadNow()`, `enqueueUploadNow()`, and `reschedule()` control uploads.
- `observeQueue()` and `observeStatus()` expose queue and operational status flows.
- `MeasurementUploader` is the backend-neutral provider contract.

Upload operations return typed `MeasurementUploadResult` values. Supported intervals are 20 minutes through 7 days.
