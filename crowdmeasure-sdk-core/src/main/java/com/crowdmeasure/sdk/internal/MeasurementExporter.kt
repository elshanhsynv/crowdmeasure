package com.crowdmeasure.sdk.internal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.crowdmeasure.sdk.CrowdMeasureError
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class MeasurementExporter(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    suspend fun export(measurements: List<Measurement>): CrowdMeasureResult<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val directory = exportDirectory().apply { mkdirs() }
                deleteExpiredFiles(directory, System.currentTimeMillis())
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(directory, "crowdmeasure_export_$timestamp.json")
                file.writeText(json.encodeToString(measurements))
                CrowdMeasureResult.Success(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.crowdmeasure-sdk.fileprovider",
                        file,
                    )
                )
            } catch (error: Exception) {
                CrowdMeasureResult.Failure(CrowdMeasureError.ExportFailed(error))
            }
        }

    /** Removes previously exported files; they contain the same data as the measurement store. */
    suspend fun deleteExports() {
        withContext(Dispatchers.IO) {
            val directory = exportDirectory()
            if (directory.exists() && !directory.deleteRecursively()) {
                throw IOException("Could not delete $directory")
            }
        }
    }

    /** Removes exports older than [EXPORT_TTL_MS]; called by export and by retention pruning. */
    suspend fun deleteExpiredExports(nowMs: Long = System.currentTimeMillis()) {
        withContext(Dispatchers.IO) { deleteExpiredFiles(exportDirectory(), nowMs) }
    }

    private fun exportDirectory() = File(context.cacheDir, "crowdmeasure-sdk-exports")
}

/**
 * How long an export file stays readable through its content URI. Share targets read the file
 * when the user picks them; keeping it longer only leaves a copy of the location history behind.
 */
internal const val EXPORT_TTL_MS = 60L * 60L * 1_000L

/** Deletes the files in [directory] last modified more than [ttlMs] before [nowMs]. */
internal fun deleteExpiredFiles(directory: File, nowMs: Long, ttlMs: Long = EXPORT_TTL_MS) {
    directory.listFiles()?.forEach { file ->
        if (file.isFile && nowMs - file.lastModified() > ttlMs) file.delete()
    }
}
