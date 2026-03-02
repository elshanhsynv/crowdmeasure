package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    enabled: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Grant"
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (granted) "Status: Granted" else "Status: Not granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        trailingContent = {
            OutlinedButton(
                onClick = onRequest,
                enabled = enabled && !granted
            ) {
                Text(if (granted) "Granted" else buttonText)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionRowPreview() {
    PermissionRow(
        title = "Title",
        subtitle = "Subtitle",
        granted = true,
        enabled = true,
        onRequest = {}
    )
}