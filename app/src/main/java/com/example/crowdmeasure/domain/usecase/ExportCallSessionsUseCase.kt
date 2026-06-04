package com.example.crowdmeasure.domain.usecase

import android.net.Uri
import com.example.crowdmeasure.data.export.Exporter
import com.example.crowdmeasure.domain.repo.CallSamplingRepository
import javax.inject.Inject

class ExportCallSessionsUseCase @Inject constructor(
    private val repository: CallSamplingRepository,
    private val exporter: Exporter
) {
    suspend operator fun invoke(lastN: Int): Result<Uri> {
        val sessions = repository.getRecentSessionsForExport(lastN)
        return exporter.exportCallSessionsToJson(sessions)
    }
}
