package com.example.crowdmeasure.presentation.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LocationServicesBanner(
    locationServicesOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !locationServicesOn,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 140,
                easing = LinearOutSlowInEasing
            )
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = 220,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        AppBanner(
            title = "Location services are off",
            body = "Enable location to improve cell and signal metrics.",
            tone = BannerTone.Critical,
            onClick = onClick,
            contentDescription = "Location services are off. Enable location to improve cell and signal metrics.",
            modifier = modifier,
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.76f)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationServicesBannerPreview() {
    MaterialTheme {
        LocationServicesBanner(
            locationServicesOn = false,
            onClick = {}
        )
    }
}