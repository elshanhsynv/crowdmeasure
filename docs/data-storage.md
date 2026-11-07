# Data Storage

By default, core stores measurements in the configured Room database and endpoint/retention settings in the configured DataStore preferences file. The background module uses its own `crowdmeasure_sdk_background` DataStore for scheduling settings and last-run status.

Existing apps can preserve their database and settings by supplying `MeasurementStore` and `CrowdMeasureSettingsStore`. All installed background workers use the same SDK instance and therefore the same adapters.

Measurements are saved as pending upload records. The optional upload module marks successful records uploaded and permanent failures failed. Both pending and failed records remain observable through the queue API.

Calls use `crowdmeasure_calls.db` and a calls-specific DataStore by default. Call sessions store stable carrier/SIM metadata; each call sample stores cellular data plus optional location and data-usage snapshots. Supply `CallStore` to preserve an existing schema, as the CrowdMeasure app does for its migrated shared database.

## Backup and device transfer

The SDK databases and preferences hold location history, call history and per-device settings. A library cannot change the host's backup rules, so on a host with `android:allowBackup="true"` (the Android default) these files go into cloud backup and device-to-device transfer unless the host excludes them. `CrowdMeasureSdk.create` logs a `WARN` through the configured `CrowdMeasureLogger` whenever backup is enabled; hosts that already exclude the files below can ignore it.

Installation IDs (measurement uploads and call uploads) are stored in `noBackupFilesDir/crowdmeasure/` and are never backed up, so a restored or transferred device gets a new ID. IDs written by earlier SDK versions are moved there on first use and keep their value. Export files live in the cache directory, which Android never backs up, and are deleted an hour after they are created (see [Export and Deletion](export-and-deletion.md)).

With the default names, exclude these files. If you changed `databaseName` or `preferencesName`, use your names instead; omit modules you do not install.

```xml
<!-- res/xml/data_extraction_rules.xml (Android 12+), referenced by android:dataExtractionRules -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="crowdmeasure_sdk.db" />
        <exclude domain="database" path="crowdmeasure_sdk.db-wal" />
        <exclude domain="database" path="crowdmeasure_sdk.db-shm" />
        <exclude domain="database" path="crowdmeasure_calls.db" />
        <exclude domain="database" path="crowdmeasure_calls.db-wal" />
        <exclude domain="database" path="crowdmeasure_calls.db-shm" />
        <exclude domain="file" path="datastore/crowdmeasure_sdk_preferences.preferences_pb" />
        <exclude domain="file" path="datastore/crowdmeasure_sdk_background.preferences_pb" />
        <exclude domain="file" path="datastore/crowdmeasure_sdk_upload.preferences_pb" />
        <exclude domain="file" path="datastore/crowdmeasure_sdk_calls.preferences_pb" />
        <exclude domain="file" path="datastore/crowdmeasure_sdk_calls_upload.preferences_pb" />
    </cloud-backup>
    <device-transfer>
        <!-- the same <exclude> entries as cloud-backup -->
    </device-transfer>
</data-extraction-rules>
```

For Android 11 and lower, put the same `<exclude>` entries in a `<full-backup-content>` file referenced by `android:fullBackupContent`. The simplest option, if the host has nothing else worth backing up, is `android:allowBackup="false"` together with a `data_extraction_rules.xml` that excludes everything, since `allowBackup="false"` alone does not stop device-to-device transfer on Android 12+.
