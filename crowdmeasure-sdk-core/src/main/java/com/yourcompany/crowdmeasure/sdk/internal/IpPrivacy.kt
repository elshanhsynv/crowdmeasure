package com.crowdmeasure.sdk.internal

import com.crowdmeasure.sdk.IpHashSaltProvider
import com.crowdmeasure.sdk.model.Measurement
import java.security.MessageDigest

internal class IpPrivacy(private val saltProvider: IpHashSaltProvider) {
    suspend fun sanitize(measurement: Measurement): Measurement {
        val value = measurement.environment.network.ip.publicIpHash ?: return measurement
        if (value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return measurement
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((saltProvider.getSalt() + value).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return measurement.copy(
            environment = measurement.environment.copy(
                network = measurement.environment.network.copy(
                    ip = measurement.environment.network.ip.copy(publicIpHash = digest)
                )
            )
        )
    }
}
