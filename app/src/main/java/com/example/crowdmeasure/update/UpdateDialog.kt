package com.example.crowdmeasure.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onRetryCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metadata = state.update ?: return
    val forceUpdate = metadata.forceUpdate
    val versionLabel = metadata.versionName?.takeIf { it.isNotBlank() }
        ?: metadata.versionCode.toString()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!forceUpdate && !state.installing) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null
            )
        },
        title = {
            Text(if (forceUpdate) "Update required" else "Update available")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CrowdMeasure $versionLabel is ready to install.",
                    style = MaterialTheme.typography.bodyMedium
                )

                metadata.releaseNotes
                    ?.takeIf { it.isNotBlank() }
                    ?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                enabled = !state.installing
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.installing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (state.installing) "Preparing" else "Update")
                }
            }
        },
        dismissButton = {
            if (forceUpdate) {
                TextButton(
                    onClick = onRetryCheck,
                    enabled = !state.installing && !state.checking
                ) {
                    Text(if (state.checking) "Checking" else "Retry")
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.installing
                ) {
                    Text("Later")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun UpdateDialogPreview() {
    CrowdMeasureTheme {
        UpdateDialog(
            state = UpdateUiState(
                update = UpdateMetadata(
                    versionCode = 42,
                    versionName = "2.0.0",
                    releaseNotes = "New features and improvements.",
                    apkUrl = "https://example.com/crowdmeasure.apk",
                    forceUpdate = false,
                    sha256 = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890abc123"
                ),
                checking = false,
                installing = false,
                message = "Download complete",
                error = null
            ),
            onInstall = {},
            onDismiss = {},
            onRetryCheck = {}
        )
    }
}