# App Update Distribution

This app uses a self-update flow for users who install APKs outside Google Play. The update system belongs only to the `:app` module and must not be moved into any `crowdmeasure-sdk-*` module.

## Public Release Repository

APK release metadata is hosted in the public GitHub repository:

```text
https://github.com/elshanhsynv/crowdmeasure-releases
```

GitHub Pages serves the metadata file:

```text
https://elshanhsynv.github.io/crowdmeasure-releases/latest.json
```

APK files are not committed to that repository. They are uploaded as GitHub Release assets, for example:

```text
https://github.com/elshanhsynv/crowdmeasure-releases/releases/download/v1.3/app-v3.apk
```

## Private App Repository Configuration

The private app repository configures the metadata URL through Gradle:

```properties
crowdmeasure.updateMetadataUrl=https://elshanhsynv.github.io/crowdmeasure-releases/latest.json
```

The app reads this value into `BuildConfig.UPDATE_METADATA_URL`. If the Gradle property is missing, `app/build.gradle.kts` uses the same GitHub Pages URL as the default.

## latest.json Format

The public `latest.json` file must use this shape:

```json
{
  "versionCode": 3,
  "versionName": "1.3",
  "apkUrl": "https://github.com/elshanhsynv/crowdmeasure-releases/releases/download/v1.3/app-v3.apk",
  "sha256": "PASTE_SHA256_HASH_HERE",
  "forceUpdate": false,
  "releaseNotes": "Bug fixes and stability improvements."
}
```

Rules:

- `versionCode` must be greater than the installed app version to show an update, and must equal the APK's real versionCode.
- `apkUrl` must be HTTPS and must not contain credentials.
- `sha256` must be the SHA-256 hash of the APK file.
- `versionName` is limited to 64 characters and `releaseNotes` to 4,000.
- `forceUpdate: true` makes the update dialog non-dismissable, but Android still requires user confirmation to install.

## Release Steps

Use the helper script from the private repo root:

```powershell
.\scripts\release-update.ps1 -ReleaseNotes "Bug fixes and background scheduling fix."
```

The script fails closed and changes nothing remote until every check passes. In order, it:

1. Builds the signed release APK (skip with `-SkipBuild`).
2. Checks with `aapt2` that the APK's package, versionCode and versionName match `app/build.gradle.kts`, so a stale APK is never published under a new version.
3. Checks with `apksigner` that the signature is valid and is not the debug key. Pass `-ExpectedCertSha256 <digest>` to also pin the release certificate; the script prints the digest it found.
4. Stages the APK as `app-v<versionCode>.apk` under `app/build/outputs/release-staging/`, outside the public repo.
5. Resolves the canonical `owner/name` of the release repo through `gh`, following transfers. Override it with `-ReleaseRepo owner/name`.
6. Fast-forwards the metadata repos.
7. Uploads the APK with GitHub CLI (skip with `-SkipUpload`). It refuses to replace an asset that is already published under the same version unless you pass `-ReplaceAsset`.
8. Downloads the published asset and checks that its SHA-256 matches. Only then does it write `latest.json`, so clients never see metadata for an asset that is missing or different.
9. Commits and pushes `latest.json`, skipping repos where it did not change (skip entirely with `-NoCommit`).

Use `-DryRun` to run the build and APK checks and print the `latest.json` it would publish, without uploading, writing or pushing anything:

```powershell
.\scripts\release-update.ps1 -SkipBuild -DryRun
```

Builds up to 1.12 have `https://elshanwork.github.io/crowdmeasure-releases/latest.json` compiled in. Until those installs have updated, pass a clone of the old Pages repository so its `latest.json` is kept current too:

```powershell
.\scripts\release-update.ps1 -LegacyReleaseRepoPath ..\crowdmeasure-releases-legacy
```

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

The app checks for updates only after the user has accepted consent.

During installation, the app does the following:

1. Checks that installs from CrowdMeasure are allowed. If Android blocks them, the user is sent to the system setting first.
2. Downloads the APK into its cache, capped at 200 MB.
3. Verifies the APK's SHA-256 against `latest.json`.
4. Reads the APK and checks that:
   - the package is this app;
   - its versionCode equals the one `latest.json` announced and is newer than the installed build;
   - it is signed with the installed app's key (a rotated key is accepted when its history contains that key).
5. Installs it with Android `PackageInstaller`.

`latest.json` and the APK come from the same host, so the hash alone proves only that the file matches the metadata. The package, version and signer checks are what stop a compromised host from serving a different, older, or downgraded build.

## What Not To Use

Do not use Firebase Hosting for APK distribution on the Spark plan. Firebase Analytics, Crashlytics, Firestore, and other Firebase runtime services can still remain in the app; only APK hosting moved to GitHub Releases and GitHub Pages.
