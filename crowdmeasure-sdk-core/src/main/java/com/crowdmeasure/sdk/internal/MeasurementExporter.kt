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

    private fun exportDirectory() = File(context.cacheDir, "crowdmeasure-sdk-exports")
}
