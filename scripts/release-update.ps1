param(
    [string]$ReleaseRepoPath,
    # GitHub "owner/name" that hosts the APK. Default: the canonical repo behind the release
    # checkout's origin, as resolved by gh (follows transfers and renames).
    [string]$ReleaseRepo,
    [string]$LegacyReleaseRepoPath,
    [string]$ReleaseNotes = "Bug fixes and stability improvements.",
    # Optional pin: SHA-256 of the release signing certificate (hex, colons/spaces allowed).
    [string]$ExpectedCertSha256,
    [switch]$ForceUpdate,
    [switch]$SkipBuild,
    [switch]$SkipUpload,
    [switch]$NoCommit,
    # Replace an asset already published under this version. Clients that already read the old
    # latest.json will fail their hash check until they fetch the new one, so this must be explicit.
    [switch]$ReplaceAsset,
    # Validate and print what would be published, without uploading, writing or pushing anything.
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param([string]$What, [scriptblock]$Command)
    # Native tools (git, gh) write progress to stderr; judge them by exit code, not by stderr.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Command 2>&1
    } finally {
        $ErrorActionPreference = $previous
    }
    if ($LASTEXITCODE -ne 0) { throw "$What failed:`n$($output | Out-String)" }
    return $output | ForEach-Object { "$_" }
}

function Get-BuildTool {
    param([string]$Name)
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) {
        $localProperties = Join-Path $RepoRoot "local.properties"
        if (Test-Path $localProperties) {
            $line = Select-String -Path $localProperties -Pattern '^sdk\.dir=(.+)$' | Select-Object -First 1
            if ($line) { $sdk = $line.Matches[0].Groups[1].Value -replace '\\:', ':' -replace '\\\\', '\' }
        }
    }
    if (-not $sdk) { throw "Android SDK not found; set ANDROID_HOME" }
    $tool = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]($_.Name -replace '[^\d.].*$', '') } -Descending |
        ForEach-Object { Get-ChildItem $_.FullName -Filter "$Name*" -File } |
        Where-Object { $_.BaseName -eq $Name } |
        Select-Object -First 1
    if (-not $tool) { throw "$Name not found in $sdk\build-tools" }
    return $tool.FullName
}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if (-not $ReleaseRepoPath) {
    $ReleaseRepoPath = Join-Path (Split-Path $RepoRoot -Parent) "crowdmeasure-releases"
}
$ReleaseRepoPath = (Resolve-Path $ReleaseRepoPath).Path

$GradleFile = Join-Path $RepoRoot "app\build.gradle.kts"
$GradleText = Get-Content $GradleFile -Raw
$ApplicationId = [regex]::Match($GradleText, 'applicationId\s*=\s*"([^"]+)"').Groups[1].Value
$VersionCode = [regex]::Match($GradleText, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$VersionName = [regex]::Match($GradleText, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $ApplicationId -or -not $VersionCode -or -not $VersionName) {
    throw "Could not read applicationId/versionCode/versionName from $GradleFile"
}

# Resolve the canonical owner/name instead of hardcoding it. A transferred repo keeps redirecting
# from its old name, but the old owner could later be re-registered, so the URL clients download
# from must use the canonical name.
$HasGh = [bool](Get-Command gh -ErrorAction SilentlyContinue)
if (-not $ReleaseRepo) {
    if (-not $HasGh) { throw "Pass -ReleaseRepo owner/name, or install the GitHub CLI so it can be resolved." }
    $RemoteUrl = (Invoke-Checked "Reading release repo remote" { git -C $ReleaseRepoPath remote get-url origin } | Select-Object -First 1).Trim()
    $ReleaseRepo = (Invoke-Checked "Resolving $RemoteUrl" { gh repo view $RemoteUrl --json nameWithOwner --jq .nameWithOwner } | Select-Object -First 1).Trim()
}
if ($ReleaseRepo -notmatch '^[\w.-]+/[\w.-]+$') { throw "Invalid -ReleaseRepo '$ReleaseRepo'; expected owner/name" }

$Tag = "v$VersionName"
$ApkName = "app-v$VersionCode.apk"
$BuiltApk = Join-Path $RepoRoot "app\build\outputs\apk\release\app-release.apk"
# Stage under build/ so the APK can never be committed to the public release repo.
$StagingDir = Join-Path $RepoRoot "app\build\outputs\release-staging"
$StagedApk = Join-Path $StagingDir $ApkName
$AssetUrl = "https://github.com/$ReleaseRepo/releases/download/$Tag/$ApkName"

$MetadataRepoPaths = @($ReleaseRepoPath)
if ($LegacyReleaseRepoPath) {
    # Builds up to 1.12 poll the old GitHub Pages URL; keep its latest.json current so they can still update.
    $MetadataRepoPaths += (Resolve-Path $LegacyReleaseRepoPath).Path
}

if (-not $SkipBuild) {
    Push-Location $RepoRoot
    try {
        & .\gradlew.bat :app:assembleRelease
        if ($LASTEXITCODE -ne 0) { throw "Release build failed" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $BuiltApk)) {
    throw "Release APK not found: $BuiltApk"
}

# --- Verify the APK is what the metadata will claim it is --------------------------------------
$Badging = (Invoke-Checked "Reading APK badging" { & (Get-BuildTool "aapt2") dump badging $BuiltApk }) -join "`n"
$Package = [regex]::Match($Badging, "package: name='(?<name>[^']+)' versionCode='(?<code>\d+)' versionName='(?<vname>[^']*)'")
if (-not $Package.Success) { throw "Could not read package information from $BuiltApk" }
if ($Package.Groups['name'].Value -ne $ApplicationId) {
    throw "APK package '$($Package.Groups['name'].Value)' is not '$ApplicationId'"
}
if ($Package.Groups['code'].Value -ne $VersionCode -or $Package.Groups['vname'].Value -ne $VersionName) {
    throw "APK is $($Package.Groups['vname'].Value) ($($Package.Groups['code'].Value)) but build.gradle.kts says $VersionName ($VersionCode). Rebuild without -SkipBuild."
}

$Certs = (Invoke-Checked "Verifying APK signature" { & (Get-BuildTool "apksigner") verify --print-certs $BuiltApk }) -join "`n"
if ($Certs -match 'CN=Android Debug') { throw "APK is signed with the debug key" }
$CertDigest = [regex]::Match($Certs, 'certificate SHA-256 digest: (?<d>[0-9a-fA-F]+)').Groups['d'].Value.ToLowerInvariant()
if (-not $CertDigest) { throw "Could not read the signing certificate digest" }
if ($ExpectedCertSha256) {
    $Expected = ($ExpectedCertSha256 -replace '[^0-9a-fA-F]', '').ToLowerInvariant()
    if ($CertDigest -ne $Expected) { throw "APK signing certificate $CertDigest does not match the expected $Expected" }
}

New-Item -ItemType Directory -Force $StagingDir | Out-Null
Copy-Item $BuiltApk $StagedApk -Force
$Sha256 = (Get-FileHash $StagedApk -Algorithm SHA256).Hash.ToLowerInvariant()

$Latest = [ordered]@{
    versionCode = [int]$VersionCode
    versionName = $VersionName
    apkUrl = $AssetUrl
    sha256 = $Sha256
    forceUpdate = [bool]$ForceUpdate
    releaseNotes = $ReleaseNotes
}
$LatestJson = $Latest | ConvertTo-Json

if ($DryRun) {
    Write-Host "Dry run: APK verified, nothing uploaded, written or pushed."
    Write-Host "  release repo: $ReleaseRepo"
    Write-Host "  package: $ApplicationId $VersionName ($VersionCode)"
    Write-Host "  signer sha256: $CertDigest"
    Write-Host "  apk: $StagedApk"
    Write-Host "latest.json would be:"
    Write-Host $LatestJson
    return
}

# --- Make sure the metadata repos can be pushed before anything is published -------------------
if (-not $NoCommit) {
    foreach ($RepoPath in $MetadataRepoPaths) {
        Invoke-Checked "Updating $RepoPath" { git -C $RepoPath pull --ff-only } | Out-Null
    }
}

# --- Publish the APK ------------------------------------------------------------------------------
if (-not $SkipUpload) {
    if (-not $HasGh) {
        throw "gh CLI not found. Install GitHub CLI, or upload $StagedApk to release $Tag manually and rerun with -SkipUpload -SkipBuild."
    }
    gh release view $Tag --repo $ReleaseRepo *> $null
    if ($LASTEXITCODE -ne 0) {
        Invoke-Checked "Creating release $Tag" { gh release create $Tag $StagedApk --repo $ReleaseRepo --title $Tag --notes $ReleaseNotes } | Out-Null
    } else {
        $Existing = Invoke-Checked "Listing release assets" { gh release view $Tag --repo $ReleaseRepo --json assets --jq '.assets[].name' }
        if ($Existing -contains $ApkName) {
            if (-not $ReplaceAsset) {
                throw "$ApkName is already published in $Tag. Bump versionCode, or rerun with -ReplaceAsset to overwrite it."
            }
            Invoke-Checked "Replacing $ApkName" { gh release upload $Tag $StagedApk --repo $ReleaseRepo --clobber } | Out-Null
        } else {
            Invoke-Checked "Uploading $ApkName" { gh release upload $Tag $StagedApk --repo $ReleaseRepo } | Out-Null
        }
    }
}

# --- Only point clients at the asset once it is really served with this exact content ----------
$Downloaded = Join-Path $StagingDir "$ApkName.published"
try {
    Invoke-WebRequest -Uri $AssetUrl -OutFile $Downloaded -UseBasicParsing
} catch {
    throw "Published asset is not reachable at $AssetUrl. latest.json was not changed. $_"
}
$PublishedSha256 = (Get-FileHash $Downloaded -Algorithm SHA256).Hash.ToLowerInvariant()
Remove-Item $Downloaded -Force
if ($PublishedSha256 -ne $Sha256) {
    throw "Published asset hash $PublishedSha256 does not match the local APK $Sha256. latest.json was not changed."
}

# --- Write and push the metadata --------------------------------------------------------------
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false
foreach ($RepoPath in $MetadataRepoPaths) {
    [System.IO.File]::WriteAllText((Join-Path $RepoPath "latest.json"), $LatestJson, $Utf8NoBom)
}

if (-not $NoCommit) {
    foreach ($RepoPath in $MetadataRepoPaths) {
        Invoke-Checked "Staging latest.json in $RepoPath" { git -C $RepoPath add latest.json } | Out-Null
        git -C $RepoPath diff --cached --quiet
        if ($LASTEXITCODE -eq 0) {
            Write-Host "latest.json unchanged in $RepoPath; nothing to commit."
            continue
        }
        Invoke-Checked "Committing latest.json in $RepoPath" { git -C $RepoPath commit -m "Release $Tag" } | Out-Null
        Invoke-Checked "Pushing latest.json in $RepoPath" { git -C $RepoPath push } | Out-Null
    }
}

Write-Host "Release metadata updated:"
Write-Host "  versionCode: $VersionCode"
Write-Host "  versionName: $VersionName"
Write-Host "  signer sha256: $CertDigest"
Write-Host "  apk: $StagedApk"
Write-Host "  sha256: $Sha256"
Write-Host "  url: $AssetUrl"
