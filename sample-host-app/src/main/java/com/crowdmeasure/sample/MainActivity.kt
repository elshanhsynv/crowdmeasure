package com.crowdmeasure.sample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.background.BackgroundResult
import com.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.crowdmeasure.sdk.calls.CallSamplingResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val sampleApp get() = application as SampleApplication
    private val sdk get() = sampleApp.sdk
    private val background get() = sampleApp.background
    private val uploads get() = sampleApp.uploads
    private val calls get() = sampleApp.calls
    private val callUploads get() = sampleApp.callUploads
    private lateinit var status: TextView

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { showRequirements() }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(status)
            addAction("Request optional permissions") {
                permissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                    )
                )
            }
            addAction("Run and save measurement") {
                lifecycleScope.launch {
                    status.text = "Measuring..."
                    status.text = sdk.measurements.runAndSave().describe()
                }
            }
            addAction("Use default endpoint") {
                lifecycleScope.launch {
                    status.text = sdk.settings
                        .setEndpointUrl("https://www.google.com/")
                        .describe()
                }
            }
            addAction("Set retention to 14 days") {
                lifecycleScope.launch {
                    status.text = sdk.settings.setRetentionDays(14).describe()
                }
            }
            addAction("Export latest 100") {
                lifecycleScope.launch {
                    status.text = sdk.data.exportMeasurements(100).describe()
                }
            }
            addAction("Delete all measurements") {
                lifecycleScope.launch {
                    status.text = sdk.data.deleteAllMeasurements().describe()
                }
            }
            addAction("Enable hourly background collection") {
                lifecycleScope.launch { status.text = this@MainActivity.background.enable(60, false).describe() }
            }
            addAction("Run background collection now") {
                lifecycleScope.launch { status.text = this@MainActivity.background.enqueueRunNow().describe() }
            }
            addAction("Disable background collection") {
                lifecycleScope.launch { status.text = this@MainActivity.background.disable().describe() }
            }
            addAction("Enable hourly Wi-Fi uploads") {
                lifecycleScope.launch { status.text = this@MainActivity.uploads.enable(60, true).describe() }
            }
            addAction("Upload pending measurements now") {
                lifecycleScope.launch { status.text = this@MainActivity.uploads.uploadNow().describe() }
            }
            addAction("Disable uploads") {
                lifecycleScope.launch { status.text = this@MainActivity.uploads.disable().describe() }
            }
            addAction("Enable cellular call sampling") {
                lifecycleScope.launch { status.text = calls.setCellularSamplingEnabled(true).describe() }
            }
            addAction("Enable generic VoIP sampling") {
                lifecycleScope.launch {
                    status.text = calls.setVoipSamplingEnabled(true).describe()
                    calls.activateEnabledFeatures()
                }
            }
            addAction("Enable call uploads") {
                lifecycleScope.launch { status.text = callUploads.enable(60, true).describe() }
            }
            addAction("Upload pending calls") {
                lifecycleScope.launch { status.text = callUploads.uploadPending().describe() }
            }
            addAction("Export call sessions") {
                lifecycleScope.launch { status.text = calls.exportSessions().describe() }
            }
            addAction("Delete call sessions") {
                lifecycleScope.launch { status.text = calls.deleteAll().describe() }
            }
        }
        setContentView(ScrollView(this).apply { addView(content) })

        showRequirements()
        lifecycleScope.launch {
            sdk.measurements.observeHistory().collectLatest { items ->
                status.text = "${status.text}\nStored measurements: ${items.size}"
            }
        }
        lifecycleScope.launch {
            background.observeStatus().collectLatest {
                status.text = "${status.text}\nBackground: ${it.workState}, last=${it.lastRun?.code}"
            }
        }
        lifecycleScope.launch {
            uploads.observeStatus().collectLatest {
                status.text = "${status.text}\nUploads: ${it.workState}, queue=${it.queue.pendingCount + it.queue.failedCount}"
            }
        }
        lifecycleScope.launch {
            calls.observeStatus().collectLatest {
                status.text = "${status.text}\nCalls: cellular=${it.settings.cellularEnabled}, voip=${it.settings.voipEnabled}, ready=${it.requirements.canStart}"
            }
        }
    }

    private fun LinearLayout.addAction(label: String, action: () -> Unit) {
        addView(Button(context).apply {
            text = label
            setOnClickListener { action() }
        })
    }

    private fun showRequirements() {
        val requirements = sdk.requirements.evaluateManualMeasurement()
        status.text = buildString {
            appendLine("Supported: ${requirements.supportedAndroidVersion}")
            appendLine("Location services: ${requirements.locationServicesEnabled}")
            append("Missing optional permissions: ${requirements.missingPermissions.joinToString()}")
        }
    }

    private fun CrowdMeasureResult<*>.describe(): String = when (this) {
        is CrowdMeasureResult.Success -> "Success: $value"
        is CrowdMeasureResult.Failure -> "Failure: $error"
    }

    private fun BackgroundResult<*>.describe(): String = when (this) {
        is BackgroundResult.Success -> "Success: $value"
        is BackgroundResult.Failure -> "Failure: $error"
    }

    private fun MeasurementUploadResult<*>.describe(): String = when (this) {
        is MeasurementUploadResult.Success -> "Success: $value"
        is MeasurementUploadResult.Failure -> "Failure: $error"
    }

    private fun CallSamplingResult<*>.describe(): String = when (this) {
        is CallSamplingResult.Success -> "Success: $value"
        is CallSamplingResult.Failure -> "Failure: $error"
    }
}
