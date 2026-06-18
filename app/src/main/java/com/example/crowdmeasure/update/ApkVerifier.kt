package com.example.crowdmeasure.update

import java.io.File
import java.security.MessageDigest

class ApkVerifier {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun verify(file: File, expectedSha256: String): Boolean =
        sha256(file).equals(expectedSha256, ignoreCase = true)
}
