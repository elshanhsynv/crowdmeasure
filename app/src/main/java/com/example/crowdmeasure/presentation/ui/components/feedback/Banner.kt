package com.example.crowdmeasure.presentation.ui.components.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "$title. $body",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = bannerColors(tone)
    val icon = bannerIcon(tone)

    val semanticsModifier = Modifier.clearAndSetSemantics {
        this.contentDescription = contentDescription
        if (onClick != null) {
            role = Role.Button
        }
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 72.dp)
        .then(semanticsModifier)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.container,
            contentColor = colors.content,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = colors.border
            )
        ) {
            BannerContent(
                title = title,
                body = body,
                icon = icon,
                colors = colors,
                trailing = trailing
            )
        }
    } else {
        Surface(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.container,
            contentColor = colors.content,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = colors.border
            )
        ) {
            BannerContent(
                title = title,
                body = body,
                icon = icon,
                colors = colors,
                trailing = trailing
            )
        }
    }
}

@Composable
private fun BannerContent(
    title: String,
    body: String,
    icon: ImageVector,
    colors: AppBannerColors,
    trailing: (@Composable () -> Unit)?
) {
    Row(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BannerIconBox(
            icon = icon,
            contentColor = colors.content
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.content.copy(alpha = 0.82f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        trailing?.invoke()
    }
}

@Composable
@NonRestartableComposable
private fun BannerIconBox(
    icon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = contentColor.copy(alpha = 0.11f),
        contentColor = contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Stable
private data class AppBannerColors(
    val container: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun bannerColors(
    tone: BannerTone
): AppBannerColors {
    return when (tone) {
        BannerTone.Info -> AppBannerColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )

        BannerTone.Warning -> AppBannerColors(
            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        )

        BannerTone.Critical -> AppBannerColors(
            container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
            content = MaterialTheme.colorScheme.onErrorContainer,
            border = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        )
    }
}

private fun bannerIcon(
    tone: BannerTone
): ImageVector {
    return when (tone) {
        BannerTone.Info -> Icons.Outlined.Info
        BannerTone.Warning -> Icons.Outlined.WarningAmber
        BannerTone.Critical -> Icons.Outlined.ErrorOutline
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBannerInfoPreview() {
    MaterialTheme {
        AppBanner(
            title = "Information",
            body = "This is a neutral informational message.",
            tone = BannerTone.Info
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBannerWarningPreview() {
    MaterialTheme {
        AppBanner(
            title = "Location services are off",
            body = "Enable location to improve cell and signal metrics.",
            tone = BannerTone.Warning
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBannerCriticalPreview() {
    MaterialTheme {
        AppBanner(
            title = "Delete warning",
            body = "This action cannot be undone.",
            tone = BannerTone.Critical
        )
    }
}