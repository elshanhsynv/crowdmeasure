package com.example.crowdmeasure.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import com.example.crowdmeasure.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** What an APK says about itself, or what the installed app is. */
data class ApkIdentity(
    val packageName: String,
    val versionCode: Long,
    /** SHA-256 digests of the current signers plus, for a single signer, its rotation history. */
    val signerDigests: Set<String>,
    /** SHA-256 digests of the current signers only. */
    val currentSignerDigests: Set<String>,
)

class ApkIdentityMismatchException(message: String) : IllegalStateException(message)

/**
 * Checks that a downloaded APK is a genuine, newer build of this app before it is installed.
 *
 * The SHA-256 in latest.json only proves the file matches the metadata; both come from the
 * same host. This adds checks the metadata cannot fake: the package, the exact version the
 * metadata announced (newer than the installed one), and a signer matching the installed app.
 */
@Singleton
class ApkIdentityVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun verify(apk: File, metadata: UpdateMetadata) {
        val archive = readArchive(apk)
            ?: throw ApkIdentityMismatchException("Downloaded file is not a readable APK")
        checkApkIdentity(
            installed = readInstalled(),
            installedVersionCode = BuildConfig.VERSION_CODE.toLong(),
            announcedVersionCode = metadata.versionCode.toLong(),
            archive = archive,
        )
    }

    fun readInstalled(): ApkIdentity {
        val info = context.packageManager.packageInfo(context.packageName)
        return info.toIdentity()
            ?: throw ApkIdentityMismatchException("Installed app signing information is unavailable")
    }

    fun readArchive(apk: File): ApkIdentity? =
        context.packageManager.archiveInfo(apk.absolutePath)?.toIdentity()

    @Suppress("DEPRECATION")
    private fun PackageManager.packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }

    @Suppress("DEPRECATION")
    private fun PackageManager.archiveInfo(path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        }

    private fun PackageInfo.toIdentity(): ApkIdentity? {
        val signing: SigningInfo = signingInfo ?: return null
        val current = signing.apkContentsSigners.orEmpty().digests()
        val all = if (signing.hasMultipleSigners()) {
            current
        } else {
            current + signing.signingCertificateHistory.orEmpty().digests()
        }
        return ApkIdentity(packageName, longVersionCode, all, current)
    }

    private fun Array<out Signature>.digests(): Set<String> = mapTo(mutableSetOf()) { signature ->
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

/**
 * The update rules, kept free of Android APIs so they can be unit-tested.
 * Throws [ApkIdentityMismatchException] when the archive must not be installed.
 */
internal fun checkApkIdentity(
    installed: ApkIdentity,
    installedVersionCode: Long,
    announcedVersionCode: Long,
    archive: ApkIdentity,
) {
    if (archive.packageName != installed.packageName) {
        throw ApkIdentityMismatchException("APK is for ${archive.packageName}, not ${installed.packageName}")
    }
    if (archive.versionCode != announcedVersionCode) {
        throw ApkIdentityMismatchException(
            "APK version ${archive.versionCode} does not match the announced version $announcedVersionCode"
        )
    }
    if (archive.versionCode <= installedVersionCode) {
        throw ApkIdentityMismatchException(
            "APK version ${archive.versionCode} is not newer than the installed $installedVersionCode"
        )
    }
    // Every current signer of the installed app must appear in the update's signers or, for a
    // rotated key, its signing history. A different key never matches.
    if (installed.currentSignerDigests.isEmpty() ||
        !archive.signerDigests.containsAll(installed.currentSignerDigests)
    ) {
        throw ApkIdentityMismatchException("APK is not signed with this app's signing key")
    }
}
