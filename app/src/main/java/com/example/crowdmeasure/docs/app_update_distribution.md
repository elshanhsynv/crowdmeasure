# App Update Distribution

This app uses a self-update flow for users who install APKs outside Google Play. The update system belongs only to the `:app` module and must not be moved into any `crowdmeasure-sdk-*` module.

## Public Release Repository

APK release metadata is hosted in the public GitHub repository:

```text
https://github.com/elshanwork/crowdmeasure-releases
```

GitHub Pages serves the metadata file:

```text
https://elshanwork.github.io/crowdmeasure-releases/latest.json
```

APK files are not committed to that repository. They are uploaded as GitHub Release assets, for example:

```text
https://github.com/elshanwork/crowdmeasure-releases/releases/download/v1.3/app-v3.apk
```

## Private App Repository Configuration

The private app repository configures the metadata URL through Gradle:

```properties
crowdmeasure.updateMetadataUrl=https://elshanwork.github.io/crowdmeasure-releases/latest.json
```

The app reads this value into `BuildConfig.UPDATE_METADATA_URL`. If the Gradle property is missing, `app/build.gradle.kts` uses the same GitHub Pages URL as the default.

## latest.json Format

The public `latest.json` file must use this shape:

```json
{
  "versionCode": 3,
  "versionName": "1.3",
  "apkUrl": "https://github.com/elshanwork/crowdmeasure-releases/releases/download/v1.3/app-v3.apk",
  "sha256": "PASTE_SHA256_HASH_HERE",
  "forceUpdate": false,
  "releaseNotes": "Bug fixes and stability improvements."
}
```

Rules:

- `versionCode` must be greater than the installed app version to show an update.
- `apkUrl` must be HTTPS.
- `sha256` must be the SHA-256 hash of the APK file.
- `forceUpdate: true` makes the update dialog non-dismissable, but Android still requires user confirmation to install.

## Release Steps

Use the helper script from the private repo root:

```powershell
.\scripts\release-update.ps1 -ReleaseNotes "Bug fixes and background scheduling fix."
```

The script builds the signed release APK, stages it as `app-v<versionCode>.apk`, computes SHA-256, updates the public repo `latest.json`, optionally uploads the APK with GitHub CLI, then commits and pushes `latest.json`.

Manual flow:

1. In the private app repo, increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build a signed release APK.
3. Rename the APK with the version code, for example `app-v4.apk`.
4. Compute its SHA-256 hash:

   ```powershell
   Get-FileHash .\app-v4.apk -Algorithm SHA256
   ```

5. In the public `crowdmeasure-releases` repo, create a GitHub Release with a tag such as `v1.4`.
6. Upload `app-v4.apk` as a release asset.
7. Update public `latest.json` with the new `versionCode`, `versionName`, `apkUrl`, `sha256`, `forceUpdate`, and `releaseNotes`.
8. Commit and push `latest.json` to the public repo.
9. Install an older app version on a test phone and confirm the app detects the new version, downloads the APK, verifies the hash, and opens Android's installer.

## Runtime Behavior

The app checks `latest.json` once after startup and also schedules a daily WorkManager check with network connectivity required. When a newer version exists, it can show an in-app dialog and post a local notification if notification permission is granted.

During installation, the app downloads the APK into its cache, verifies SHA-256, and uses Android `PackageInstaller`. If Android blocks installs from this app, the user is sent to the system setting for allowing installs from CrowdMeasure.

## What Not To Use

Do not use Firebase Hosting for APK distribution on the Spark plan. Firebase Analytics, Crashlytics, Firestore, and other Firebase runtime services can still remain in the app; only APK hosting moved to GitHub Releases and GitHub Pages.
