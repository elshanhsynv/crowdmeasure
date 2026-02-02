package com.example.crowdmeasure.domain.usecase

import android.net.Uri
import com.example.crowdmeasure.data.export.Exporter
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import javax.inject.Inject

class ExportMeasurementsUseCase @Inject constructor(
    private val repo: MeasurementRepository,
    private val exporter: Exporter
) {
    suspend operator fun invoke(lastN: Int): Result<Uri> {
        val items = repo.getLastN(lastN)
        return exporter.exportMeasurementsToJson(items)
    }
}