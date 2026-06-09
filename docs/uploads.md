Available

# Uploads

Add the optional backend-neutral upload module and install it with a `MeasurementUploader`:

```kotlin
val uploads = CrowdMeasureUploads.install(
    context = applicationContext,
    sdk = sdk,
    uploader = uploader,
)
```

Installation registers the runtime but schedules nothing. Enable uploads explicitly:

```kotlin
uploads.enable(intervalMinutes = 60, wifiOnly = true)
uploads.enqueueUploadNow()
uploads.uploadNow()
uploads.disable()
```

Successful measurements enter the local pending queue. Permanent backend failures mark candidates failed; failed records remain eligible for later retries. Transient failures retain queue state and cause WorkManager retries. Observe queue counts and last-run status through the public client.
