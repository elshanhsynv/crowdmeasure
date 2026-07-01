param(
    [string]$ReleaseRepoPath,
    [string]$ReleaseNotes = "Bug fixes and stability improvements.",
    [switch]$ForceUpdate,
    [switch]$SkipBuild,
    [switch]$SkipUpload,
    [switch]$NoCommit
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if (-not $ReleaseRepoPath) {
    $ReleaseRepoPath = Join-Path (Split-Path $RepoRoot -Parent) "crowdmeasure-releases"
}
$ReleaseRepoPath = Resolve-Path $ReleaseRepoPath

$GradleFile = Join-Path $RepoRoot "app\build.gradle.kts"
$GradleText = Get-Content $GradleFile -Raw
$VersionCode = [regex]::Match($GradleText, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$VersionName = [regex]::Match($GradleText, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $VersionCode -or -not $VersionName) {
    throw "Could not read versionCode/versionName from $GradleFile"
}

$Tag = "v$VersionName"
$ApkName = "app-v$VersionCode.apk"
$BuiltApk = Join-Path $RepoRoot "app\build\outputs\apk\release\app-release.apk"
$StagedApk = Join-Path $ReleaseRepoPath $ApkName
$AssetUrl = "https://github.com/elshanwork/crowdmeasure-releases/releases/download/$Tag/$ApkName"

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
$Latest | ConvertTo-Json | Set-Content (Join-Path $ReleaseRepoPath "latest.json")

if (-not $SkipUpload) {
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $gh) {
        throw "gh CLI not found. Install GitHub CLI or rerun with -SkipUpload after uploading $StagedApk manually to release $Tag."
    } else {
        Push-Location $ReleaseRepoPath
        try {
            gh release view $ *> $null
            if ($LASTEXITCODE -ne 0) {
                gh release create $Tag $StagedApk --title $Tag --notes $ReleaseNotes
            } else {
                gh release upload $Tag $StagedApk --clobber
            }
            if ($LASTEXITCODE -ne 0) { throw "GitHub release upload failed" }
        } finally {
            Pop-Location
        }
    }
}

if (-not $NoCommit) {
    Push-Location $ReleaseRepoPath
    try {
        git add latest.json
        git commit -m "Release $Tag"
        if ($LASTEXITCODE -ne 0) { throw "Could not commit latest.json" }
        git push
        if ($LASTEXITCODE -ne 0) { throw "Could not push latest.json" }
    } finally {
        Pop-Location
    }
}

Write-Host "Release metadata updated:"
Write-Host "  versionCode: $VersionCode"
Write-Host "  versionName: $VersionName"
Write-Host "  apk: $StagedApk"
Write-Host "  sha256: $Sha256"
Write-Host "  url: $AssetUrl"
