package com.example.crowdmeasure.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkVerifierTest {
    private val verifier = ApkVerifier()

    @Test
    fun sha256MatchesFileContents() {
        val file = File.createTempFile("crowdmeasure-update", ".apk")
        file.writeText("test apk")

        assertEquals(
            "2183b8d680809f991816eb45ba8661c9f686111223fdecc35e1c0cbf56569d96",
            verifier.sha256(file)
        )
        assertTrue(
            verifier.verify(
                file,
                "2183B8D680809F991816EB45BA8661C9F686111223FDECC35E1C0CBF56569D96"
            )
        )
        assertFalse(
            verifier.verify(
                file,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
        )

        file.delete()
    }
}
