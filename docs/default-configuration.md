# Default Configuration

These are the SDK defaults used when a host does not pass a custom config.

## Core

| Setting | Default |
|---|---|
| `databaseName` | `crowdmeasure_sdk.db` |
| `preferencesName` | `crowdmeasure_sdk_preferences` |
| `defaultEndpointUrl` | `https://www.google.com/` |
| `defaultRetentionDays` | `7` |
| `publicIpPolicy` | `RAW` |
| `logger` | `CrowdMeasureLogger.NONE` |
| `performanceProbe.attempts` | `8` |
| `performanceProbe.timeoutMs` | `10000` |

Collector toggles default to enabled: location, Wi-Fi, cellular, public IP, and performance.

## Background

| Setting | Default |
|---|---|
| `preferencesName` | `crowdmeasure_sdk_background` |
| `defaultEnabled` | `true` |
| `defaultIntervalMinutes` | `60` |
| `defaultWifiOnly` | `false` |
| Minimum interval | `20` minutes |
| Maximum interval | `10080` minutes, or 7 days |

Installing background support schedules nothing. Hosts still call `enable()`.

## Measurement Uploads

| Setting | Default |
|---|---|
| `preferencesName` | `crowdmeasure_sdk_upload` |
| `defaultBatchSize` | `50` |
| `defaultIntervalMinutes` | `60` |
| `defaultWifiOnly` | `false` |
| `defaultMeasurementUploadEnabled` | `true` |
| Minimum interval | `20` minutes |
| Maximum interval | `10080` minutes, or 7 days |

Installing upload support schedules nothing. Hosts still call `enable()`.

## Calls

| Setting | Default |
|---|---|
| `databaseName` | `crowdmeasure_calls.db` |
| `preferencesName` | `crowdmeasure_sdk_calls` |
| `notificationIconResId` | Required from host |
| `notificationChannelName` | `Call cell sampling` |
| `notificationTitle` | `Measuring signal quality` |
| `notificationText` | `Collecting network stats during this call.` |
| `sampleIntervalSeconds` | `5` |
| `retentionDays` | `7` |
| `cellularEnabled` | `true` |
| `voipEnabled` | `true` |

Installing calls support starts nothing. Hosts call `activateEnabledFeatures()` to restore enabled features.

## Call Uploads

| Setting | Default |
|---|---|
| `preferencesName` | `crowdmeasure_sdk_calls_upload` |
| `defaultBatchSize` | `50` |
| `defaultIntervalMinutes` | `60` |
| `defaultWifiOnly` | `true` |
| `enabled` | `false` |

Call uploads are independent from measurement uploads.

## Firestore

Firestore modules do not create Firebase. The host passes an existing `FirebaseFirestore` instance. Measurement uploads use the `measurements` collection; call uploads use `calls/{sessionId}/samples`.
