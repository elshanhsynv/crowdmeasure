package com.example.crowdmeasure.update

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInstallEvents @Inject constructor() {
    private val _failures = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val failures: SharedFlow<String> = _failures.asSharedFlow()

    fun reportFailure(message: String) {
        _failures.tryEmit(message)
    }
}

class UpdateInstallFailedException(status: Int, statusMessage: String?) :
    IllegalStateException(
        statusMessage
            ?.takeIf(String::isNotBlank)
            ?.let { "Update could not be installed: $it" }
            ?: "Update could not be installed (status $status)."
    )
