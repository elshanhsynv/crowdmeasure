# Permissions

The SDK manifest declares network state, Wi-Fi state, coarse location, and phone-state permissions used by measurement collectors. The SDK never requests permissions or launches permission UI.

Use `sdk.requirements.evaluateManualMeasurement()` before collection. The host decides how and when to request permissions and how to handle disabled location services.

When `requiredDefaultDataMnoId` is configured, `READ_PHONE_STATE` is also required to verify the default data SIM. If it cannot be read, the SDK fails closed and does not collect new data.

The calls module declares phone-state, fine/background location, notifications, and foreground-location-service permissions. Battery-optimization exemption is a reliability warning, not a start blocker.
