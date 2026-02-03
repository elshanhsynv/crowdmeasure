package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onOpenDetail: (String) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val items = vm.items.collectAsState().value
    var tagText by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Filter by feedback tag (optional).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = tagText,
                    onValueChange = {
                        tagText = it
                        vm.setTag(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Feedback tag") },
                    supportingText = {
                        Text("Showing ${items.size} record(s)")
                    }
                )
            }
        }

        // List
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            if (items.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No measurements found.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try clearing the filter or run a new measurement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = items,
                        key = { it.header.measurementId }
                    ) { m ->
                        val timeText = fmt.format(Date(m.header.timestampUtcMs))
                        val transportText = m.context.transport?.toString() ?: "-" // safest
                        val rttText = m.performance.rttAvgMs?.let { "$it ms" } ?: "-"
                        val ttfbText = m.performance.ttfbMs?.let { "$it ms" } ?: "-"

                        val loc = m.context.coarseLocation
                        val locText = loc?.let {
                            "${it.lat}, ${it.lon} (~${it.accuracyMeters}m)"
                        } ?: "-"

                        ListItem(
                            headlineContent = { Text(timeText) },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Transport: $transportText",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "RTT avg: $rttText   •   TTFB: $ttfbText",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Coarse location: $locText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetail(m.header.measurementId) }
                                .padding(horizontal = 4.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}