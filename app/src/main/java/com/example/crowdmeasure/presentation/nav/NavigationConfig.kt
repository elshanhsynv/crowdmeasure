package com.example.crowdmeasure.presentation.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

object NavigationConfig {

    private const val ForwardSlideFraction = 3
    private const val BackSlideFraction = 4
    private const val BackgroundScale = 0.98f
    private const val IncomingScale = 1.01f

    private val offsetSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    private val fadeSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Forward navigation.
     *
     * New destination enters with subtle horizontal motion
     * and light fade for continuity.
     */
    val enterTransition: EnterTransition =
        slideInHorizontally(
            animationSpec = offsetSpring,
            initialOffsetX = { it / ForwardSlideFraction }
        ) + fadeIn(animationSpec = fadeSpring)

    /**
     * Current screen moves slightly left and fades.
     *
     * Small scale reduction creates hierarchy
     * without obvious zooming.
     */
    val exitTransition: ExitTransition =
        slideOutHorizontally(
            animationSpec = offsetSpring,
            targetOffsetX = { -it / BackSlideFraction }
        ) + fadeOut(animationSpec = fadeSpring) +
                scaleOut(
                    targetScale = BackgroundScale,
                    animationSpec = fadeSpring
                )

    /**
     * Back navigation restores previous screen naturally.
     */
    val popEnterTransition: EnterTransition =
        slideInHorizontally(
            animationSpec = offsetSpring,
            initialOffsetX = { -it / BackSlideFraction }
        ) + fadeIn(animationSpec = fadeSpring) +
                scaleIn(
                    initialScale = BackgroundScale,
                    animationSpec = fadeSpring
                )

    /**
     * Current screen exits quickly to the right.
     */
    val popExitTransition: ExitTransition =
        slideOutHorizontally(
            animationSpec = offsetSpring,
            targetOffsetX = { it / ForwardSlideFraction }
        ) + fadeOut(animationSpec = fadeSpring)

    /**
     * Bottom navigation / sibling destinations.
     *
     * Crossfade only.
     * Avoid directional movement between equal hierarchy screens.
     */
    val crossfadeEnterTransition: EnterTransition =
        fadeIn(animationSpec = fadeSpring) +
                scaleIn(
                    initialScale = IncomingScale,
                    animationSpec = fadeSpring
                )

    val crossfadeExitTransition: ExitTransition =
        fadeOut(animationSpec = fadeSpring)
}