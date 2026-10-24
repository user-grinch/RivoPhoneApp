package com.grinch.rivo4.view.screen.transitions

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavBackStackEntry
import com.grinch.rivo4.view.theme.RivoEasing
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import kotlin.math.roundToInt

val LocalTransitionTimeScale: ProvidableCompositionLocal<Float> = compositionLocalOf { 2.5f }
val LocalTransitionStyle: ProvidableCompositionLocal<Int> = compositionLocalOf { 0 }

val SmoothDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
val SmoothAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
val SmoothStandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
val PredictiveBackEasing: Easing = LinearEasing

fun scaledDuration(baseMs: Int, timeScale: Float): Int =
    (baseMs * timeScale.coerceIn(0.1f, 5.0f)).roundToInt().coerceAtLeast(1)

fun createExpressiveSharedAxisTransitions(
    timeScale: Float = 2.5f,
    predictiveBack: Boolean = true
): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(320, timeScale)
            val fadeDur = scaledDuration(240, timeScale)
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.15f,
                animationSpec = tween(fadeDur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(280, timeScale)
            val fadeDur = scaledDuration(200, timeScale)
            slideOutHorizontally(
                targetOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + fadeOut(
                targetAlpha = 0.25f,
                animationSpec = tween(fadeDur, easing = SmoothAccelerateEasing)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(300, timeScale)
            val fadeDur = if (predictiveBack) dur else scaledDuration(220, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothDecelerateEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            slideInHorizontally(
                initialOffsetX = { (-it * 0.20f).roundToInt() },
                animationSpec = tween(dur, easing = easing)
            ) + fadeIn(
                initialAlpha = if (predictiveBack) 0.5f else 0.25f,
                animationSpec = tween(fadeDur, easing = fadeEasing)
            ) + if (predictiveBack) {
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(dur, easing = PredictiveBackEasing)
                )
            } else {
                EnterTransition.None
            }
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(280, timeScale)
            val fadeDur = if (predictiveBack) dur else scaledDuration(200, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothAccelerateEasing
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(dur, easing = easing)
            ) + fadeOut(
                targetAlpha = if (predictiveBack) 0.7f else 0.8f,
                animationSpec = tween(fadeDur, easing = fadeEasing)
            ) + if (predictiveBack) {
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(dur, easing = PredictiveBackEasing)
                )
            } else {
                ExitTransition.None
            }
        }
    }

fun createParallaxSlideTransitions(
    timeScale: Float = 1.0f,
    predictiveBack: Boolean = true
): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(320, timeScale)
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.1f,
                animationSpec = tween(dur / 2, easing = SmoothStandardEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(300, timeScale)
            slideOutHorizontally(
                targetOffsetX = { (-it * 0.30f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + fadeOut(
                targetAlpha = 0.35f,
                animationSpec = tween(dur / 2, easing = SmoothAccelerateEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(320, timeScale)
            val fadeDur = if (predictiveBack) dur else dur / 2
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothDecelerateEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            slideInHorizontally(
                initialOffsetX = { (-it * 0.25f).roundToInt() },
                animationSpec = tween(dur, easing = easing)
            ) + fadeIn(
                initialAlpha = if (predictiveBack) 0.5f else 0.35f,
                animationSpec = tween(fadeDur, easing = fadeEasing)
            ) + if (predictiveBack) {
                scaleIn(initialScale = 0.94f, animationSpec = tween(dur, easing = PredictiveBackEasing))
            } else {
                EnterTransition.None
            }
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(300, timeScale)
            val fadeDur = if (predictiveBack) dur else dur / 2
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothAccelerateEasing
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(dur, easing = easing)
            ) + if (predictiveBack) {
                scaleOut(targetScale = 0.92f, animationSpec = tween(dur, easing = PredictiveBackEasing)) +
                        fadeOut(targetAlpha = 0.7f, animationSpec = tween(fadeDur, easing = fadeEasing))
            } else {
                ExitTransition.None
            }
        }
    }

fun createExpressiveFloatTransitions(
    timeScale: Float = 1.0f,
    predictiveBack: Boolean = true
): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(300, timeScale)
            val fadeDur = scaledDuration(240, timeScale)
            slideInVertically(
                initialOffsetY = { (it * 0.16f).roundToInt() },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.15f,
                animationSpec = tween(fadeDur, easing = SmoothStandardEasing)
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(240, timeScale)
            fadeOut(
                targetAlpha = 0.3f,
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(280, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothDecelerateEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            fadeIn(
                initialAlpha = if (predictiveBack) 0.5f else 0.3f,
                animationSpec = tween(dur, easing = fadeEasing)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(dur, easing = easing)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(260, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothAccelerateEasing
            slideOutVertically(
                targetOffsetY = { (it * 0.16f).roundToInt() },
                animationSpec = tween(dur, easing = easing)
            ) + fadeOut(
                targetAlpha = if (predictiveBack) 0.5f else 0f,
                animationSpec = tween(dur, easing = fadeEasing)
            ) + scaleOut(
                targetScale = if (predictiveBack) 0.92f else 0.94f,
                animationSpec = tween(dur, easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing)
            )
        }
    }

fun createExpressiveZoomTransitions(
    timeScale: Float = 1.0f,
    predictiveBack: Boolean = true
): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(300, timeScale)
            scaleIn(
                initialScale = 0.86f,
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.15f,
                animationSpec = tween(dur / 2, easing = SmoothStandardEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(250, timeScale)
            scaleOut(
                targetScale = 1.08f,
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            ) + fadeOut(
                targetAlpha = 0.2f,
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(280, timeScale)
            val fadeDur = if (predictiveBack) dur else dur / 2
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothDecelerateEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            scaleIn(
                initialScale = if (predictiveBack) 0.94f else 1.08f,
                animationSpec = tween(dur, easing = easing)
            ) + fadeIn(
                initialAlpha = if (predictiveBack) 0.5f else 0.2f,
                animationSpec = tween(fadeDur, easing = fadeEasing)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(250, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothAccelerateEasing
            scaleOut(
                targetScale = if (predictiveBack) 0.92f else 0.88f,
                animationSpec = tween(dur, easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing)
            ) + fadeOut(
                targetAlpha = if (predictiveBack) 0.5f else 0f,
                animationSpec = tween(dur, easing = fadeEasing)
            )
        }
    }

fun createDepthStackTransitions(
    timeScale: Float = 1.0f,
    predictiveBack: Boolean = true
): NavHostAnimatedDestinationStyle =
    object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(320, timeScale)
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(dur, easing = SmoothDecelerateEasing)
            ) + fadeIn(
                initialAlpha = 0.1f,
                animationSpec = tween(dur / 2, easing = SmoothStandardEasing)
            )
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(270, timeScale)
            scaleOut(
                targetScale = 0.88f,
                animationSpec = tween(dur, easing = SmoothStandardEasing)
            ) + fadeOut(
                targetAlpha = 0.35f,
                animationSpec = tween(dur, easing = SmoothAccelerateEasing)
            )
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            val dur = scaledDuration(300, timeScale)
            val fadeDur = if (predictiveBack) dur else dur / 2
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothDecelerateEasing
            val fadeEasing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(dur, easing = easing)
            ) + fadeIn(
                initialAlpha = if (predictiveBack) 0.5f else 0.35f,
                animationSpec = tween(fadeDur, easing = fadeEasing)
            )
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            val dur = scaledDuration(270, timeScale)
            val easing = if (predictiveBack) PredictiveBackEasing else SmoothStandardEasing
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(dur, easing = easing)
            ) + if (predictiveBack) {
                scaleOut(targetScale = 0.92f, animationSpec = tween(dur, easing = PredictiveBackEasing))
            } else {
                ExitTransition.None
            }
        }
    }

val ExpressiveSharedAxisTransitions: NavHostAnimatedDestinationStyle = createExpressiveSharedAxisTransitions(2.5f, true)
val ParallaxSlideTransitions: NavHostAnimatedDestinationStyle = createParallaxSlideTransitions(2.5f, true)
val ExpressiveFloatTransitions: NavHostAnimatedDestinationStyle = createExpressiveFloatTransitions(2.5f, true)
val ExpressiveZoomTransitions: NavHostAnimatedDestinationStyle = createExpressiveZoomTransitions(2.5f, true)
val DepthStackTransitions: NavHostAnimatedDestinationStyle = createDepthStackTransitions(2.5f, true)

val AppTransitions: NavHostAnimatedDestinationStyle = ExpressiveSharedAxisTransitions
val ZoomTransitions: NavHostAnimatedDestinationStyle = ParallaxSlideTransitions
val FadeTransitions: NavHostAnimatedDestinationStyle = ExpressiveFloatTransitions

fun getAppTransition(
    style: Int,
    predictiveBack: Boolean = true,
    timeScale: Float = 2.5f
): NavHostAnimatedDestinationStyle {
    if (style == 5 || timeScale <= 0.05f) return NoTransitionsDestinationStyle
    return when (style) {
        0 -> createExpressiveSharedAxisTransitions(timeScale, predictiveBack)
        1 -> createParallaxSlideTransitions(timeScale, predictiveBack)
        2 -> createExpressiveFloatTransitions(timeScale, predictiveBack)
        3 -> createExpressiveZoomTransitions(timeScale, predictiveBack)
        4 -> createDepthStackTransitions(timeScale, predictiveBack)
        else -> createExpressiveSharedAxisTransitions(timeScale, predictiveBack)
    }
}

object NoTransitionsDestinationStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
}
