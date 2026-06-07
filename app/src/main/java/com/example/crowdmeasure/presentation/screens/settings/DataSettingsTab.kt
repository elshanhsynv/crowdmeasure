package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.components.cards.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard
import com.example.crowdmeasure.presentation.util.UiState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val DefaultMeasurementExportCount = 50
private const val DefaultCallExportCount = 100
private const val MaxMeasurementExportCount = 10_000
private const val MaxCallExportCount = 1_000
private const val MeasurementExportMaxDigits = 5
private const val CallExportMaxDigits = 4

@Composable
internal fun DataSettingsTab(
    exportState: UiState<Unit>,
    callExportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onExportCalls: (Context, Int) -> Unit,
    onClearCallExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    val context = LocalContext.current

    var exportCount by remember {
        mutableStateOf(DefaultMeasurementExportCount.toString())
    }

    var callExportCount by remember {
        mutableStateOf(DefaultCallExportCount.toString())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        ExportDataCard(
            title = "Export Data",
            description = "Export recent measurements as JSON",
            icon = Icons.Outlined.Code,
            count = exportCount,
            onCountChange = {
                exportCount = it.digitsOnly(maxLength = MeasurementExportMaxDigits)
            },
            label = "Measurement count",
            placeholder = DefaultMeasurementExportCount.toString(),
            supportingText = "Exports the latest 1–10,000 measurements.",
            buttonText = "Export Measurements",
            state = exportState,
            loadingText = "Exporting measurements...",
            successText = "Measurements exported successfully.",
            onAction = {
                val count = exportCount.toBoundedInt(
                    default = DefaultMeasurementExportCount,
                    min = 1,
                    max = MaxMeasurementExportCount
                )
                onExport(context, count)
            },
            onClearState = onClearExportState
        )

        ExportDataCard(
            title = "Export Call Data",
            description = "Export call samples as JSON",
            icon = Icons.Outlined.PhoneAndroid,
            count = callExportCount,
            onCountChange = {
                callExportCount = it.digitsOnly(maxLength = CallExportMaxDigits)
            },
            label = "Session count",
            placeholder = DefaultCallExportCount.toString(),
            supportingText = "Exports the latest 1–1,000 call sessions.",
            buttonText = "Export Call Sessions",
            state = callExportState,
            loadingText = "Exporting call sessions...",
            successText = "Call sessions exported successfully.",
            onAction = {
                val count = callExportCount.toBoundedInt(
                    default = DefaultCallExportCount,
                    min = 1,
                    max = MaxCallExportCount
                )
                onExportCalls(context, count)
            },
            onClearState = onClearCallExportState
        )

        DeleteLocalDataCard(
            deleteState = deleteState,
            onDeleteClick = { showDeleteDialog = true },
            onClearDeleteState = onClearDeleteState
        )

        Spacer(Modifier.safeContentPadding())
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun ExportDataCard(
    title: String,
    description: String,
    icon: ImageVector,
    count: String,
    onCountChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String,
    buttonText: String,
    state: UiState<Unit>,
    loadingText: String,
    successText: String,
    onAction: () -> Unit,
    onClearState: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isLoading = state is UiState.Loading

    SettingsSectionCard(
        title = title,
        description = description,
        icon = icon
    ) {
        ExportCountField(
            value = count,
            onValueChange = onCountChange,
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            enabled = !isLoading,
            onDone = { focusManager.clearFocus() }
        )

        FilledTonalButton(
            onClick = {
                focusManager.clearFocus()
                onAction()
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoading) "Please wait..." else buttonText
            )
        }

        OperationStateMessage(
            state = state,
            loadingText = loadingText,
            successText = successText,
            onClearState = onClearState
        )
    }
}

@Composable
private fun ExportCountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = { Text(supportingText) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
private fun DeleteLocalDataCard(
    deleteState: UiState<Unit>,
    onDeleteClick: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    val isLoading = deleteState is UiState.Loading

    SettingsSectionCard(
        title = "Delete Local Data",
//        description = "Permanently remove all locally stored measurements.",
        icon = Icons.Outlined.Warning
    ) {
        AssistiveHint(
            text = "This action cannot be undone. Export your data before deleting it."
        )

        OutlinedButton(
            onClick = onDeleteClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = if (isLoading) "Deleting..." else "Delete All Local Data"
            )
        }

        OperationStateMessage(
            state = deleteState,
            loadingText = "Deleting local data...",
            successText = "All local data deleted.",
            onClearState = onClearDeleteState
        )
    }
}

@Composable
private fun OperationStateMessage(
    state: UiState<Unit>,
    loadingText: String,
    successText: String,
    onClearState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnClearState by rememberUpdatedState(onClearState)

    AnimatedVisibility(
        visible = state !is UiState.Idle,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 140,
                easing = LinearOutSlowInEasing
            )
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing
            ),
            initialScale = 0.98f
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            ),
            targetScale = 0.98f
        ),
        modifier = modifier
    ) {
        when (state) {
            UiState.Idle -> Unit

            UiState.Loading -> {
                AssistiveHint(loadingText)
            }

            is UiState.Success -> {
                Text(
                    text = successText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is UiState.Error -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    TextButton(
                        onClick = onClearState
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            delay(3_000.milliseconds)
            latestOnClearState()
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete all local data?")
        },
        text = {
            Text(
                text = "This will permanently delete all local measurements stored on this device. This action cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun String.digitsOnly(maxLength: Int): String {
    return filter(Char::isDigit).take(maxLength)
}

private fun String.toBoundedInt(
    default: Int,
    min: Int,
    max: Int
): Int {
    return toIntOrNull()?.coerceIn(min, max) ?: default
}