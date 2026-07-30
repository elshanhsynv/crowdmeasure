# Device Info Collector

Adds basic device and app metadata to a measurement.

It records Android/device identifiers that are useful for grouping measurements by OS, manufacturer, model, and app version. It does not read personal accounts, contacts, or user files.

Used in manual and background measurements.

Successful sample:

```json
{
  "appVersion": "1.0.0",
  "androidRelease": "15",
  "androidSdk": 35,
  "deviceModel": "Google Pixel 8",
  "brand": "google",
  "deviceManufacturer": "Google",
  "deviceOS": "Android",
  "buildID": "AP3A.240905.015",
  "hardware": "raven",
  "chipset": "Tensor",
  "chipsetManufacturer": "Google"
}
```
