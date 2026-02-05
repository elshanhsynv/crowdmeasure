package com.example.crowdmeasure.presentation.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

object NavigationConfig {

    private val springSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    private val floatSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Enter transition for forward navigation (push).
     * The new screen slides in from the right.
     */
    fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = springSpec,
            initialOffsetX = { it } // Start fully off-screen right
        ) + fadeIn(floatSpringSpec)
    }

    /**
     * Exit transition when navigating forward (current screen exits).
     * The current screen slides slightly left, fades out, and SCALES DOWN
     * to create a "depth" effect (putting it in the background).
     */
    fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = springSpec,
            targetOffsetX = { -it / 4 } // Move slightly left
        ) + fadeOut(floatSpringSpec) + scaleOut(
            animationSpec = floatSpringSpec,
            targetScale = 0.95f // Subtle scale down
        )
    }

    /**
     * Enter transition when popping back stack (returning to previous screen).
     * The previous screen slides back from left, fades in, and SCALES UP
     * returning from the background.
     */
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = springSpec,
            initialOffsetX = { -it / 4 } // Start slightly left
        ) + fadeIn(floatSpringSpec) + scaleIn(
            animationSpec = floatSpringSpec,
            initialScale = 0.95f // Start slightly smaller
        )
    }

    /**
     * Exit transition when popping (current screen exits to the right).
     * The current screen slides out to the right.
     */
    fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = springSpec,
            targetOffsetX = { it } // Move fully off-screen right
        ) + fadeOut(floatSpringSpec)
    }

    /**
     * Lateral navigation (Bottom Bar switches).
     * Uses a Crossfade with a slight scale for a "breathing" effect.
     */
    fun crossfadeTransition(): EnterTransition = fadeIn(
        animationSpec = tween(200)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(300)
    )

    fun crossfadeExit(): ExitTransition = fadeOut(
        animationSpec = tween(200)
    )
}