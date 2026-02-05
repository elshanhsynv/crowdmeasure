package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing

@Immutable
data class SectionHeaderAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun DetailSectionCard(
    title: String,
    description: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    headerAction: SectionHeaderAction? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = LocalSpacing.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = s.xxs
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(s.md)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!description.isNullOrBlank()) {
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (headerAction != null) {
                    TextButton(onClick = headerAction.onClick) { Text(headerAction.label) }
                } else {
                    TextButton(onClick = onToggle) { Text(if (expanded) "Collapse" else "Expand") }
                }
            }
            if (expanded) {
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(s.sm))
                content()
            }
        }
    }
}