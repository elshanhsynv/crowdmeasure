package com.example.crowdmeasure.presentation.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme

/** Lets the user pause automatic measurements and uploads at any time. */
@Composable
fun DataCollectionCard(
    autoRunEnabled: Boolean,
    autoRunIntervalMinutes: Int,
    uploadsEnabled: Boolean,
    onAutoRunChanged: (Boolean) -> Unit,
    onUploadsChanged: (Boolean) -> Unit,
) {
    val interval = autoRunIntervalMinutes.toLong()
        .coerceIn(CrowdMeasureBackground.MIN_INTERVAL_MINUTES, CrowdMeasureBackground.MAX_INTERVAL_MINUTES)

    SettingsSectionCard(
        title = "Data collection",
        icon = Icons.Outlined.Tune,
        description = "Pause automatic measurements or uploads at any time."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SamplingToggleRow(
                title = "Automatic measurements",
                subtitle = if (autoRunEnabled) {
                    "Measures the network in the background about every $interval min"
                } else {
                    "Paused. Measurements run only when you start them"
                },
                checked = autoRunEnabled,
                enabled = true,
                onCheckedChange = onAutoRunChanged
            )
            SamplingToggleRow(
                title = "Upload data",
                subtitle = if (uploadsEnabled) {
                    "Sends measurements and call sessions to the CrowdMeasure server"
                } else {
                    "Paused. Data stays on this device"
                },
                checked = uploadsEnabled,
                enabled = true,
                onCheckedChange = onUploadsChanged
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataCollectionCardPreview() {
    CrowdMeasureTheme {
        DataCollectionCard(
            autoRunEnabled = true,
            autoRunIntervalMinutes = 20,
            uploadsEnabled = false,
            onAutoRunChanged = {},
            onUploadsChanged = {}
        )
    }
}
