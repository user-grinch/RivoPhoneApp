package com.grinch.rivo4.view.screen.transitions

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavBackStackEntry
import com.grinch.rivo4.view.theme.RivoEasing
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import kotlin.math.roundToInt

val LocalTransitionTimeScale: ProvidableCompositionLocal<Float> = compositionLocalOf { 1.0f }
val LocalTransitionStyle: ProvidableCompositionLocal<Int> = compositionLocalOf { 0 }

/**
 * Balanced, visible smooth decelerate & accelerate curves.
 * Unlike extreme front-loaded curves that jump 60% in a single frame,
 * these curves start visibly from frame 1 and glide to a soft, fluid stop.
 */
val SmoothDecelerateEasing: Easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
val SmoothAccelerateEasing: Easing = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f)
val SmoothStandardEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

private fun scaledDuration(baseMs: Int, timeScale: Float): Int =
    (baseMs * timeScale.coerceIn(0.1f, 3.0f)).roundToInt().coerceAtLeast(1)

/**
 * Creates a Material 3 Expressive Shared Axis Navigation Transition (Style 0 - Standard)
 * scaled by [timeScale].
 */
fun createExpressiveSharedAxisTransitions(timeScale: Float = 1.0f): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(340, timeScale)
            val fadeDur = scaledDuration(280, timeScale)
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.2f,
                animationSpec = tween(fadeDur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(280, timeScale)
            val fadeDur = scaledDuration(220, timeScale)
            slideOutHorizontally(
                targetOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + fadeOut(
                targetAlpha = 0.3f,
                animationSpec = tween(fadeDur, easing = SmoothAccelerateEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(340, timeScale)
            val fadeDur = scaledDuration(260, timeScale)
            slideInHorizontally(
                initialOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.3f,
                animationSpec = tween(fadeDur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(280, timeScale)
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            )
        }
    }

/**
 * Creates a Smooth Parallax Slide Navigation Transition (Style 1 - Slide)
 * scaled by [timeScale].
 */
fun createParallaxSlideTransitions(timeScale: Float = 1.0f): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(350, timeScale)
            val fadeDur = scaledDuration(240, timeScale)
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.15f,
                animationSpec = tween(fadeDur, easing = RivoEasing.Linear)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(320, timeScale)
            slideOutHorizontally(
                targetOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeOut(
                targetAlpha = 0.3f,
                animationSpec = tween(dur, easing = RivoEasing.Linear)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(350, timeScale)
            slideInHorizontally(
                initialOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.3f,
                animationSpec = tween(dur, easing = RivoEasing.Linear)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(320, timeScale)
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }
    }

/**
 * Creates an Expressive Float & Fade Navigation Transition (Style 2 - Fade)
 * scaled by [timeScale].
 */
fun createExpressiveFloatTransitions(timeScale: Float = 1.0f): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(320, timeScale)
            val fadeDur = scaledDuration(260, timeScale)
            slideInVertically(
                initialOffsetY = { (it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.2f,
                animationSpec = tween(fadeDur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(240, timeScale)
            fadeOut(
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(300, timeScale)
            fadeIn(
                initialAlpha = 0.2f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(260, timeScale)
            slideOutVertically(
                targetOffsetY = { (it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + fadeOut(
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            )
        }
    }

val ExpressiveSharedAxisTransitions: NavHostAnimatedDestinationStyle = createExpressiveSharedAxisTransitions(1.0f)
val ParallaxSlideTransitions: NavHostAnimatedDestinationStyle = createParallaxSlideTransitions(1.0f)
val ExpressiveFloatTransitions: NavHostAnimatedDestinationStyle = createExpressiveFloatTransitions(1.0f)

// Backward compatibility references
val AppTransitions: NavHostAnimatedDestinationStyle = ExpressiveSharedAxisTransitions
val ZoomTransitions: NavHostAnimatedDestinationStyle = ParallaxSlideTransitions
val FadeTransitions: NavHostAnimatedDestinationStyle = ExpressiveFloatTransitions

fun getAppTransition(
    style: Int,
    predictiveBack: Boolean = true,
    timeScale: Float = 1.0f
): NavHostAnimatedDestinationStyle {
    if (style == 3 || timeScale <= 0.05f) return NoTransitionsDestinationStyle
    return when (style) {
        0 -> createExpressiveSharedAxisTransitions(timeScale)
        1 -> createParallaxSlideTransitions(timeScale)
        2 -> createExpressiveFloatTransitions(timeScale)
        else -> createExpressiveSharedAxisTransitions(timeScale)
    }
}

object NoTransitionsDestinationStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
}
