package com.grinch.rivo4.view.screen.transitions

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object AppTransitions : NavHostAnimatedDestinationStyle() {
    private const val DURATION = 500

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(DURATION))
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(DURATION)) + 
        fadeOut(animationSpec = tween(DURATION), targetAlpha = 0.4f)
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(initialOffsetX = { -it / 10 }, animationSpec = tween(DURATION)) + 
        fadeIn(animationSpec = tween(DURATION), initialAlpha = 0.4f)
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(DURATION))
    }
}

object FadeTransitions : NavHostAnimatedDestinationStyle() {
    private const val DURATION = 400
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(DURATION)) + scaleIn(initialScale = 0.95f, animationSpec = tween(DURATION))
    }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(DURATION))
    }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(DURATION))
    }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(DURATION)) + scaleOut(targetScale = 0.95f, animationSpec = tween(DURATION))
    }
}

object ZoomTransitions : NavHostAnimatedDestinationStyle() {
    private const val DURATION = 500
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(initialScale = 0.8f, animationSpec = tween(DURATION)) + fadeIn(animationSpec = tween(DURATION))
    }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(targetScale = 1.1f, animationSpec = tween(DURATION)) + fadeOut(animationSpec = tween(DURATION))
    }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(initialScale = 1.1f, animationSpec = tween(DURATION)) + fadeIn(animationSpec = tween(DURATION))
    }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(targetScale = 0.8f, animationSpec = tween(DURATION)) + fadeOut(animationSpec = tween(DURATION))
    }
}

fun getAppTransition(style: Int): NavHostAnimatedDestinationStyle {
    return when (style) {
        0 -> AppTransitions
        1 -> ZoomTransitions
        2 -> FadeTransitions
        3 -> NoTransitionsDestinationStyle
        else -> AppTransitions
    }
}

object NoTransitionsDestinationStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }
    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }
}
