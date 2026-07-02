package com.example.crowdmeasure.presentation.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.components.cards.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard

@Composable
fun BackgroundReliabilityCard(
    onFixScheduling: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Background Reliability",
        description = "Android may delay background work to save battery.",
        icon = Icons.Outlined.SyncProblem,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistiveHint(
                text = "If auto-run appears inactive, repair the schedule or review battery optimization settings."
            )

            ReliabilityActions(
                onFixScheduling = onFixScheduling,
                onOpenBatterySettings = onOpenBatterySettings
            )
        }
    }
}

@Composable
private fun ReliabilityActions(
    onFixScheduling: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onFixScheduling,
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionIcon(Icons.Outlined.Build)
            Text(
                text = "Fix Scheduling",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionIcon(Icons.Outlined.BatterySaver)
            Text(
                text = "Battery Settings",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
@NonRestartableComposable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Icon(
        imageVector = icon,
        contentDescription = null
    )

    Spacer(Modifier.width(8.dp))
}

@Preview(showBackground = true)
@Composable
private fun BackgroundReliabilityCardPreview() {
    MaterialTheme {
        BackgroundReliabilityCard(
            onFixScheduling = {},
            onOpenBatterySettings = {}
        )
    }
}