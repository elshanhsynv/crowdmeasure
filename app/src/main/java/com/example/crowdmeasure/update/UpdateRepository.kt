package com.example.crowdmeasure.update

import com.example.crowdmeasure.BuildConfig
import com.example.crowdmeasure.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val metadataClient: UpdateMetadataClient,
    private val downloader: ApkDownloader,
    private val verifier: ApkVerifier,
    private val identityVerifier: ApkIdentityVerifier,
    private val installer: UpdateInstaller,
    private val notifier: UpdateNotifier,
    private val analytics: UpdateAnalytics,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    suspend fun checkForUpdate(notify: Boolean): Result<UpdateAvailability> = withContext(io) {
        runCatching {
            val latest = metadataClient.fetchLatest()
            val available = if (latest.isNewerThan(BuildConfig.VERSION_CODE)) latest else null
            if (available != null) {
                analytics.available(available)
                if (notify) notifier.notifyAvailableIfNeeded(available)
            }
            UpdateAvailability(available)
        }.onFailure { analytics.failed("check", null, it) }
    }

    suspend fun downloadVerifyAndInstall(metadata: UpdateMetadata): Result<Unit> = withContext(io) {
        runCatching {
            // Ask for the install permission before downloading, so the user does not download twice.
            if (!installer.canRequestPackageInstalls()) throw InstallPermissionRequiredException()
            analytics.downloadStarted(metadata)
            val apkFile = downloader.download(metadata)
            if (!verifier.verify(apkFile, metadata.sha256)) {
                apkFile.delete()
                error("APK SHA-256 did not match latest.json")
            }
            try {
                identityVerifier.verify(apkFile, metadata)
            } catch (error: ApkIdentityMismatchException) {
                apkFile.delete()
                throw error
            }
            analytics.downloadVerified(metadata)
            installer.install(apkFile)
            analytics.installOpened(metadata)
        }.onFailure { analytics.failed("install", metadata, it) }
    }

    fun openUnknownAppSourcesSettings() {
        installer.openUnknownAppSourcesSettings()
    }
}
