package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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

    Column(Modifier.padding(contentPadding).padding(16.dp)) {
        OutlinedTextField(
            value = tagText,
            onValueChange = {
                tagText = it
                vm.setTag(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Filter by feedback tag") }
        )
        Spacer(Modifier.height(12.dp))

        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { m ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail(m.header.measurementId) }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(fmt.format(Date(m.header.timestampUtcMs)), style = MaterialTheme.typography.titleMedium)
                        Text("Transport: ${m.context.transport}")
                        Text("RTT avg: ${m.performance.rttAvgMs ?: "-"} ms | TTFB: ${m.performance.ttfbMs ?: "-"} ms")
                        val loc = m.context.coarseLocation
                        Text("Coarse location: ${loc?.let { "${it.lat}, ${it.lon} (~${it.accuracyMeters}m)" } ?: "-"}")
                    }
                }
            }
        }
    }
}