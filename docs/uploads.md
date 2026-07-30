# Uploads

Add the optional backend-neutral upload module and install it with a `MeasurementUploader`:

```kotlin
val uploads = CrowdMeasureUploads.install(
    context = applicationContext,
    sdk = sdk,
    uploader = uploader,
    config = MeasurementUploadConfig(
        preferencesName = "crowdmeasure_sdk_upload",
        defaultBatchSize = 50,
        defaultIntervalMinutes = 60,
        defaultWifiOnly = false,
    ),
)
```

Installation registers the runtime but schedules nothing. The default preferences name preserves
compatibility with existing SDK installations. Repeated equivalent installation is idempotent;
conflicting installation fails with `IllegalStateException`. Enable uploads explicitly:

```kotlin
uploads.enable(intervalMinutes = 60, wifiOnly = false)
uploads.enqueueUploadNow()
uploads.uploadNow()
uploads.disable()
```

Successful measurements enter the local pending queue. Permanent backend failures mark candidates failed; failed records remain eligible for later retries. Transient failures retain queue state and cause WorkManager retries. Observe queue counts and last-run status through the public client.

Defaults: preferences file `crowdmeasure_sdk_upload`, batch size `50`, interval `60` minutes, Wi-Fi-only `false`, persisted upload setting `true`, allowed interval `20` minutes to 7 days. Installing still schedules nothing until `enable()` is called.
