package com.example.crowdmeasure.update

import org.junit.Assert.assertThrows
import org.junit.Test

class ApkIdentityCheckTest {
    private val installed = identity(version = 13, signers = setOf("release-key"))

    @Test
    fun acceptsNewerAnnouncedBuildWithSameKey() {
        checkApkIdentity(installed, 13, 14, identity(version = 14, signers = setOf("release-key")))
    }

    @Test
    fun acceptsRotatedKeyWhoseHistoryContainsTheInstalledKey() {
        val rotated = ApkIdentity(
            packageName = PACKAGE,
            versionCode = 14,
            signerDigests = setOf("release-key", "new-key"),
            currentSignerDigests = setOf("new-key"),
        )
        checkApkIdentity(installed, 13, 14, rotated)
    }

    @Test
    fun rejectsDifferentPackage() {
        assertMismatch(identity(version = 14, signers = setOf("release-key"), packageName = "com.evil.app"), 14)
    }

    @Test
    fun rejectsVersionThatDiffersFromMetadata() {
        // A stale or swapped APK whose real versionCode is not what latest.json announced.
        assertMismatch(identity(version = 12, signers = setOf("release-key")), announced = 14)
    }

    @Test
    fun rejectsDowngradeOrSameVersionEvenWhenAnnounced() {
        assertMismatch(identity(version = 13, signers = setOf("release-key")), announced = 13)
        assertMismatch(identity(version = 10, signers = setOf("release-key")), announced = 10)
    }

    @Test
    fun rejectsDifferentSigningKey() {
        assertMismatch(identity(version = 14, signers = setOf("attacker-key")), announced = 14)
    }

    @Test
    fun rejectsWhenInstalledSignersAreUnknown() {
        assertThrows(ApkIdentityMismatchException::class.java) {
            checkApkIdentity(
                identity(version = 13, signers = emptySet()), 13, 14,
                identity(version = 14, signers = setOf("release-key")),
            )
        }
    }

    private fun assertMismatch(archive: ApkIdentity, announced: Long) {
        assertThrows(ApkIdentityMismatchException::class.java) {
            checkApkIdentity(installed, 13, announced, archive)
        }
    }

    private fun identity(version: Long, signers: Set<String>, packageName: String = PACKAGE) =
        ApkIdentity(packageName, version, signers, signers)

    private companion object {
        const val PACKAGE = "com.example.crowdmeasure"
    }
}
