package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing

@Composable
fun BackgroundReliabilityCard(
    onFixScheduling: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    SettingsSectionCard(
        title = "Background Reliability",
        description = "Android may delay background work to save battery. If auto-run appears inactive, try these actions.",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Button(
                onClick = onFixScheduling,
                modifier = Modifier.weight(1f)
            ) {
                Text("Fix Scheduling")
            }

            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.weight(1f)
            ) {
                Text("Battery Settings")
            }
        }
    }
}

@Preview
@Composable
private fun BackgroundReliabilityCardPreview() {
    BackgroundReliabilityCard(
        onFixScheduling = {},
        onOpenBatterySettings = {}
    )
}