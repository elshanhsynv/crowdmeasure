package com.example.crowdmeasure.workers

import com.example.crowdmeasure.domain.repo.CallUploadRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CallUploadWorkRepository @Inject constructor(
    private val sessionRepository: UserSessionRepository,
    private val callUploadRepository: CallUploadRepository
) {
    suspend fun execute(runAttemptCount: Int): UploadExecution {
        if (!sessionRepository.settings.first().firestoreUploadsEnabled) {
            return UploadExecution(
                outcome = UploadExecution.Outcome.SUCCESS,
                code = CODE_GATE_BLOCKED
            )
        }

        return try {
            UploadExecution(
                outcome = UploadExecution.Outcome.SUCCESS,
                code = CODE_OK,
                uploadedCount = callUploadRepository.uploadPending(UPLOAD_LIMIT).getOrThrow()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            UploadExecution(
                outcome = if (WorkRetryClassifier.shouldRetryUpload(error, runAttemptCount)) {
                    UploadExecution.Outcome.RETRY
                } else {
                    UploadExecution.Outcome.FAILURE
                },
                code = CODE_UPLOAD_FAILED,
                cause = error
            )
        }
    }

    companion object {
        const val CODE_OK = "call_upload_ok"
        const val CODE_GATE_BLOCKED = "call_upload_gate_blocked"
        const val CODE_UPLOAD_FAILED = "call_upload_failed"
        private const val UPLOAD_LIMIT = 10
    }
}
