package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.MyLocation
import com.example.crowdmeasure.presentation.ui.theme.ExtendedColors

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    enabled: Boolean,
    onRequest: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    buttonText: String = "Grant"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
//            Text(
//                text = if (granted) "Status: Granted" else "Status: Not granted",
//                style = MaterialTheme.typography.bodySmall,
//                fontWeight = FontWeight.Medium,
//                color = if (granted) MaterialTheme.colorScheme.tertiary
//                else MaterialTheme.colorScheme.onSurfaceVariant
//            )
        }

        if (granted) {
            GrantedPill()
        } else {
            OutlinedButton(
                onClick = onRequest,
                enabled = enabled
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun GrantedPill() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = ExtendedColors.successDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Granted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRowGrantedPreview() {
    PermissionRow(
        title = "Coarse Location",
        subtitle = "Adds approximate coordinates to measurements",
        granted = true,
        enabled = true,
        onRequest = {},
        icon = Icons.Outlined.MyLocation
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionRowNotGrantedPreview() {
    PermissionRow(
        title = "Fine Location",
        subtitle = "Required for detailed cell signal on some devices",
        granted = false,
        enabled = true,
        onRequest = {},
        icon = Icons.Outlined.MyLocation
    )
}