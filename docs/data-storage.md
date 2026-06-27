Available

# Data Storage

By default, core stores measurements in the configured Room database and endpoint/retention settings in the configured DataStore preferences file. The background module uses its own `crowdmeasure_sdk_background` DataStore for scheduling settings and last-run status.

Existing apps can preserve their database and settings by supplying `MeasurementStore` and `CrowdMeasureSettingsStore`. All installed background workers use the same SDK instance and therefore the same adapters.

Measurements are saved as pending upload records. The optional upload module marks successful records uploaded and permanent failures failed. Both pending and failed records remain observable through the queue API.

Calls use `crowdmeasure_calls.db` and a calls-specific DataStore by default. Call sessions store stable carrier/SIM metadata; each call sample stores cellular data plus optional location and data-usage snapshots. Supply `CallStore` to preserve an existing schema, as the CrowdMeasure app does for its migrated shared database.
