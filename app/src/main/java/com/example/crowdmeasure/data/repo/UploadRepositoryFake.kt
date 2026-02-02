package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.domain.model.RecordState
import com.example.crowdmeasure.domain.repo.UploadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Backend is not ready:
 * - We keep records in PENDING (or you can mark READY_TO_UPLOAD if you want to simulate).
 * - This fake implementation simply allows toggling a flag/state, without any network.
 */
class UploadRepositoryFake(
    private val dao: MeasurementDao,
    private val io: CoroutineDispatcher
) : UploadRepository {

    override suspend fun markReadyToUpload(measurementId: String, ready: Boolean) = withContext(io) {
        val state = if (ready) RecordState.READY_TO_UPLOAD else RecordState.PENDING
        dao.updateRecordState(measurementId, state.name)
    }
}