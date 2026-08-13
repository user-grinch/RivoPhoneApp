package com.grinch.rivo4.view.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

object RivoDurations {
    const val Short1: Int = 50
    const val Short2: Int = 100
    const val Short3: Int = 150
    const val Short4: Int = 200
    const val Medium1: Int = 250
    const val Medium2: Int = 300
    const val Medium3: Int = 350
    const val Medium4: Int = 400
    const val Long1: Int = 450
    const val Long2: Int = 500
    const val Long3: Int = 550
    const val Long4: Int = 600
    const val ExtraLong1: Int = 700
    const val ExtraLong2: Int = 800
    const val ExtraLong3: Int = 900
    const val ExtraLong4: Int = 1000
}

object RivoEasing {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val Linear: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)
}

object RivoStaticMotion {
    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 3800f)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 800f)

    fun <T> screenEnter(): FiniteAnimationSpec<T> =
        tween(RivoDurations.Long2, easing = RivoEasing.Emphasized)

    fun <T> screenExit(): FiniteAnimationSpec<T> =
        tween(RivoDurations.Medium4, easing = RivoEasing.EmphasizedAccelerate)

    fun <T> screenEnterEffects(): FiniteAnimationSpec<T> =
        tween(RivoDurations.Medium4, easing = RivoEasing.EmphasizedDecelerate)

    fun <T> screenExitEffects(): FiniteAnimationSpec<T> =
        tween(RivoDurations.Short4, easing = RivoEasing.EmphasizedAccelerate)

    fun <T> ambientLoop(durationMillis: Int): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = RivoEasing.Linear)
}

object RivoMotion {
    @Composable
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

    @Composable
    fun <T> spatialFast(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

    @Composable
    fun <T> spatialSlow(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.slowSpatialSpec()

    @Composable
    fun <T> effectsDefault(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

    @Composable
    fun <T> effectsFast(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastEffectsSpec()

    @Composable
    fun <T> effectsSlow(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.slowEffectsSpec()

    @Composable
    fun <T> listItemEnter(): FiniteAnimationSpec<T> = spatialDefault()

    @Composable
    fun <T> listItemEnterEffects(): FiniteAnimationSpec<T> = effectsDefault()

    @Composable
    fun <T> listItemExit(): FiniteAnimationSpec<T> = spatialFast()

    @Composable
    fun <T> listItemExitEffects(): FiniteAnimationSpec<T> = effectsFast()

    @Composable
    fun <T> dialogEnter(): FiniteAnimationSpec<T> = spatialDefault()

    @Composable
    fun <T> dialogEnterEffects(): FiniteAnimationSpec<T> = effectsDefault()

    @Composable
    fun <T> dialogExit(): FiniteAnimationSpec<T> = spatialFast()

    @Composable
    fun <T> dialogExitEffects(): FiniteAnimationSpec<T> = effectsFast()

    @Composable
    fun <T> pressFeedback(): FiniteAnimationSpec<T> = spatialFast()

    @Composable
    fun <T> pressFeedbackEffects(): FiniteAnimationSpec<T> = effectsFast()

    @Composable
    fun <T> shapeMorph(): FiniteAnimationSpec<T> = spatialFast()

    @Composable
    fun <T> shapeMorphSlow(): FiniteAnimationSpec<T> = spatialSlow()

    @Composable
    fun <T> screenTransition(): FiniteAnimationSpec<T> = RivoStaticMotion.screenEnter()

    @Composable
    fun <T> screenTransitionEffects(): FiniteAnimationSpec<T> = RivoStaticMotion.screenEnterEffects()

    @Composable
    fun <T> colorChange(): FiniteAnimationSpec<T> = effectsDefault()

    @Composable
    fun <T> elevationChange(): FiniteAnimationSpec<T> = effectsFast()
}
