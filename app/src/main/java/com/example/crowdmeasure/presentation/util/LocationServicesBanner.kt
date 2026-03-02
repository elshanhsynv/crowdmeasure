package com.example.crowdmeasure.presentation.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.AppPermissions.locationServicesEnabledFlow
import kotlinx.coroutines.flow.flowOf


@Composable
fun LocationServicesBanner(
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val isPreview = LocalInspectionMode.current
    val locationServicesOn by remember(context, isPreview) {
        if (isPreview) {
            flowOf(true)
        } else {
            locationServicesEnabledFlow(context)
        }
    }.collectAsState(initial = if (isPreview) true else AppPermissions.isLocationServicesEnabled(context))

    if (!locationServicesOn) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Text(
                    "Location services OFF — cell metrics may be empty. Enable in Notification Panel.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(spacing.sm))
    }
}
