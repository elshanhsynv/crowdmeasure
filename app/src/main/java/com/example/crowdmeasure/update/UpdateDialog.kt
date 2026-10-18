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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
    val update = state.update ?: return

    val dialogState = remember(update, state.installing, state.checking) {
        UpdateDialogState(
            forceUpdate = update.forceUpdate,
            versionLabel = update.displayVersion,
            canDismiss = !update.forceUpdate && !state.installing,
            canInstall = !state.installing,
            canRetry = !state.installing && !state.checking,
            installLabel = if (state.installing) "Preparing" else "Update",
            secondaryLabel = when {
                update.forceUpdate && state.checking -> "Checking"
                update.forceUpdate -> "Retry"
                else -> "Later"
            }
        )
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (dialogState.canDismiss) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (dialogState.forceUpdate) {
                    "Update required"
                } else {
                    "Update available"
                }
            )
        },
        text = {
            UpdateDialogContent(
                versionLabel = dialogState.versionLabel,
                releaseNotes = update.releaseNotes,
                message = state.message,
                error = state.error
            )
        },
        confirmButton = {
            InstallButton(
                installing = state.installing,
                enabled = dialogState.canInstall,
                label = dialogState.installLabel,
                onClick = onInstall
            )
        },
        dismissButton = {
            SecondaryActionButton(
                forceUpdate = dialogState.forceUpdate,
                enabled = if (dialogState.forceUpdate) {
                    dialogState.canRetry
                } else {
                    dialogState.canDismiss
                },
                label = dialogState.secondaryLabel,
                onRetryCheck = onRetryCheck,
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
private fun UpdateDialogContent(
    versionLabel: String,
    releaseNotes: String?,
    message: String?,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CrowdMeasure $versionLabel is ready to install.",
            style = MaterialTheme.typography.bodyMedium
        )

        releaseNotes
            ?.takeIf(String::isNotBlank)
            ?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        message
            ?.takeIf(String::isNotBlank)
            ?.let { value ->
                StatusText(
                    text = value,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        error
            ?.takeIf(String::isNotBlank)
            ?.let { value ->
                StatusText(
                    text = value,
                    color = MaterialTheme.colorScheme.error
                )
            }
    }
}

@Composable
private fun StatusText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun InstallButton(
    installing: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (installing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }

            Text(label)
        }
    }
}

@Composable
private fun SecondaryActionButton(
    forceUpdate: Boolean,
    enabled: Boolean,
    label: String,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        modifier = modifier,
        onClick = if (forceUpdate) onRetryCheck else onDismiss,
        enabled = enabled
    ) {
        Text(label)
    }
}

private val UpdateMetadata.displayVersion: String
    get() = versionName
        ?.takeIf(String::isNotBlank)
        ?: versionCode.toString()

@Immutable
private data class UpdateDialogState(
    val forceUpdate: Boolean,
    val versionLabel: String,
    val canDismiss: Boolean,
    val canInstall: Boolean,
    val canRetry: Boolean,
    val installLabel: String,
    val secondaryLabel: String
)

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

@Preview(showBackground = true)
@Composable
private fun ForceUpdateDialogPreview() {
    CrowdMeasureTheme {
        UpdateDialog(
            state = UpdateUiState(
                update = UpdateMetadata(
                    versionCode = 43,
                    versionName = "2.1.0",
                    releaseNotes = "This version includes critical stability fixes.",
                    apkUrl = "https://example.com/crowdmeasure.apk",
                    forceUpdate = true,
                    sha256 = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890abc123"
                ),
                checking = false,
                installing = false,
                message = null,
                error = "Unable to prepare the update. Please try again."
            ),
            onInstall = {},
            onDismiss = {},
            onRetryCheck = {}
        )
    }
}