package com.example.crowdmeasure.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.crowdmeasure.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ApkIdentityVerifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val verifier = ApkIdentityVerifier(context)
    private val ownApk = File(context.applicationInfo.sourceDir)

    @Test
    fun readsTheInstalledApkLikeTheInstalledApp() {
        val installed = verifier.readInstalled()
        val archive = verifier.readArchive(ownApk)!!

        assertEquals(context.packageName, archive.packageName)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), archive.versionCode)
        assertTrue(installed.currentSignerDigests.isNotEmpty())
        assertTrue(archive.signerDigests.containsAll(installed.currentSignerDigests))
    }

    @Test
    fun rejectsTheSameVersionAsAnUpdate() {
        val metadata = UpdateMetadata(
            versionCode = BuildConfig.VERSION_CODE,
            apkUrl = "https://example.com/app.apk",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        )
        assertThrows(ApkIdentityMismatchException::class.java) { verifier.verify(ownApk, metadata) }
    }

    @Test
    fun rejectsAFileThatIsNotAnApk() {
        val notAnApk = File.createTempFile("not-an-apk", ".apk", context.cacheDir)
        try {
            notAnApk.writeText("not an apk")
            assertNull(verifier.readArchive(notAnApk))
        } finally {
            notAnApk.delete()
        }
    }
}
