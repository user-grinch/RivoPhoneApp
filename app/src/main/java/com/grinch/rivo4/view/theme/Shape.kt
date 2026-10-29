package com.grinch.rivo4.view.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

object RivoShapeDefaults {
    const val DefaultRoundness: Int = 28
    val RoundnessOptions: List<Int> = listOf(32, 28, 20, 12, 5)

    const val BaseExtraSmall: Int = 4
    const val BaseSmall: Int = 8
    const val BaseMedium: Int = 12
    const val BaseLarge: Int = 16
    const val BaseLargeIncreased: Int = 20
    const val BaseExtraLarge: Int = 28
    const val BaseExtraLargeIncreased: Int = 32
    const val BaseExtraExtraLarge: Int = 48

    val None: Shape = RectangleShape
    val Full: Shape = CircleShape
}

fun rivoRoundnessScale(roundness: Int): Float =
    roundness.coerceIn(5, RivoShapeDefaults.BaseExtraExtraLarge) /
        RivoShapeDefaults.DefaultRoundness.toFloat()

fun rivoCornerDp(baseDp: Int, roundness: Int): Dp =
    (baseDp * rivoRoundnessScale(roundness)).coerceAtLeast(1f).dp

fun rivoCornerShape(baseDp: Int, roundness: Int): CornerBasedShape =
    RoundedCornerShape(rivoCornerDp(baseDp, roundness))

fun rivoShapes(roundness: Int = RivoShapeDefaults.DefaultRoundness): Shapes {
    val scale = rivoRoundnessScale(roundness)
    fun corner(baseDp: Int): CornerBasedShape {
        val cornerDp = (baseDp * scale).coerceAtLeast(1f).dp
        return RoundedCornerShape(cornerDp)
    }
    return Shapes(
        extraSmall = corner(RivoShapeDefaults.BaseExtraSmall),
        small = corner(RivoShapeDefaults.BaseSmall),
        medium = corner(RivoShapeDefaults.BaseMedium),
        large = corner(RivoShapeDefaults.BaseLarge),
        extraLarge = corner(RivoShapeDefaults.BaseExtraLarge)
    ).copy(
        largeIncreased = corner(RivoShapeDefaults.BaseLargeIncreased),
        extraLargeIncreased = corner(RivoShapeDefaults.BaseExtraLargeIncreased),
        extraExtraLarge = corner(RivoShapeDefaults.BaseExtraExtraLarge)
    )
}

val LocalCardRoundness: ProvidableCompositionLocal<Int> =
    staticCompositionLocalOf { RivoShapeDefaults.DefaultRoundness }

object RivoMaterialShapes {
    val Circle: RoundedPolygon get() = MaterialShapes.Circle
    val Square: RoundedPolygon get() = MaterialShapes.Square
    val Slanted: RoundedPolygon get() = MaterialShapes.Slanted
    val Arch: RoundedPolygon get() = MaterialShapes.Arch
    val Fan: RoundedPolygon get() = MaterialShapes.Fan
    val Arrow: RoundedPolygon get() = MaterialShapes.Arrow
    val SemiCircle: RoundedPolygon get() = MaterialShapes.SemiCircle
    val Oval: RoundedPolygon get() = MaterialShapes.Oval
    val Pill: RoundedPolygon get() = MaterialShapes.Pill
    val Triangle: RoundedPolygon get() = MaterialShapes.Triangle
    val Diamond: RoundedPolygon get() = MaterialShapes.Diamond
    val ClamShell: RoundedPolygon get() = MaterialShapes.ClamShell
    val Pentagon: RoundedPolygon get() = MaterialShapes.Pentagon
    val Gem: RoundedPolygon get() = MaterialShapes.Gem
    val Sunny: RoundedPolygon get() = MaterialShapes.Sunny
    val VerySunny: RoundedPolygon get() = MaterialShapes.VerySunny
    val Cookie4Sided: RoundedPolygon get() = MaterialShapes.Cookie4Sided
    val Cookie6Sided: RoundedPolygon get() = MaterialShapes.Cookie6Sided
    val Cookie7Sided: RoundedPolygon get() = MaterialShapes.Cookie7Sided
    val Cookie9Sided: RoundedPolygon get() = MaterialShapes.Cookie9Sided
    val Cookie12Sided: RoundedPolygon get() = MaterialShapes.Cookie12Sided
    val Clover4Leaf: RoundedPolygon get() = MaterialShapes.Clover4Leaf
    val Clover8Leaf: RoundedPolygon get() = MaterialShapes.Clover8Leaf
    val Burst: RoundedPolygon get() = MaterialShapes.Burst
    val SoftBurst: RoundedPolygon get() = MaterialShapes.SoftBurst
    val Boom: RoundedPolygon get() = MaterialShapes.Boom
    val SoftBoom: RoundedPolygon get() = MaterialShapes.SoftBoom
    val Flower: RoundedPolygon get() = MaterialShapes.Flower
    val Puffy: RoundedPolygon get() = MaterialShapes.Puffy
    val PuffyDiamond: RoundedPolygon get() = MaterialShapes.PuffyDiamond
    val PixelCircle: RoundedPolygon get() = MaterialShapes.PixelCircle
    val PixelTriangle: RoundedPolygon get() = MaterialShapes.PixelTriangle
    val Ghostish: RoundedPolygon get() = MaterialShapes.Ghostish
    val Heart: RoundedPolygon get() = MaterialShapes.Heart
    val Bun: RoundedPolygon get() = MaterialShapes.Bun

    val AvatarPolygons: List<RoundedPolygon>
        get() = listOf(Circle, Cookie9Sided, Clover4Leaf, Arch, Pill, Gem, Sunny, PixelCircle)

    val IncomingCallPulse: List<RoundedPolygon>
        get() = listOf(Circle, Cookie9Sided, Clover4Leaf, Circle)

    val AvatarMorphStart: RoundedPolygon get() = Circle
    val AvatarMorphEnd: RoundedPolygon get() = Cookie9Sided
}

enum class RivoExpressiveAvatarShape(val id: Int, val label: String) {
    SQUIRCLE(0, "Squircle"),
    CIRCLE(1, "Circle"),
    SQUARE(2, "Square"),
    SLANTED(3, "Slanted"),
    ARCH(4, "Arch"),
    SEMICIRCLE(5, "Semicircle"),
    OVAL(6, "Oval"),
    PILL(7, "Pill"),
    TRIANGLE(8, "Triangle"),
    ARROW(9, "Arrow"),
    FAN(10, "Fan"),
    DIAMOND(11, "Diamond"),
    CLAMSHELL(12, "Clamshell"),
    PENTAGON(13, "Pentagon"),
    GEM(14, "Gem"),
    SUNNY(15, "Sunny"),
    VERY_SUNNY(16, "Very sunny"),
    COOKIE_4(17, "4-sided cookie"),
    COOKIE_6(18, "6-sided cookie"),
    COOKIE_7(19, "7-sided cookie"),
    COOKIE_9(20, "9-sided cookie"),
    COOKIE_12(21, "12-sided cookie"),
    CLOVER_4(22, "4-leaf clover"),
    CLOVER_8(23, "8-leaf clover"),
    BURST(24, "Burst"),
    SOFT_BURST(25, "Soft burst"),
    BOOM(26, "Boom"),
    SOFT_BOOM(27, "Soft boom"),
    FLOWER(28, "Flower"),
    PUFFY(29, "Puffy"),
    PUFFY_DIAMOND(30, "Puffy diamond"),
    GHOSTISH(31, "Ghost-ish"),
    PIXEL_CIRCLE(32, "Pixel circle"),
    PIXEL_TRIANGLE(33, "Pixel triangle"),
    BUN(34, "Bun"),
    HEART(35, "Heart");

    companion object {
        fun fromId(id: Int): RivoExpressiveAvatarShape =
            entries.firstOrNull { it.id == id } ?: SQUIRCLE
    }
}

const val RIVO_AVATAR_SHAPE_SQUIRCLE: Int = 0
const val RIVO_AVATAR_SHAPE_CIRCLE: Int = 1
const val RIVO_AVATAR_SHAPE_COOKIE_9: Int = 20

@Composable
fun rivoAvatarShape(shapeIndex: Int): Shape = when (shapeIndex) {
    0 -> MaterialTheme.shapes.large
    1 -> CircleShape
    2 -> RivoMaterialShapes.Square.toShape()
    3 -> RivoMaterialShapes.Slanted.toShape()
    4 -> RivoMaterialShapes.Arch.toShape()
    5 -> RivoMaterialShapes.SemiCircle.toShape()
    6 -> RivoMaterialShapes.Oval.toShape()
    7 -> RivoMaterialShapes.Pill.toShape()
    8 -> RivoMaterialShapes.Triangle.toShape()
    9 -> RivoMaterialShapes.Arrow.toShape()
    10 -> RivoMaterialShapes.Fan.toShape()
    11 -> RivoMaterialShapes.Diamond.toShape()
    12 -> RivoMaterialShapes.ClamShell.toShape()
    13 -> RivoMaterialShapes.Pentagon.toShape()
    14 -> RivoMaterialShapes.Gem.toShape()
    15 -> RivoMaterialShapes.Sunny.toShape()
    16 -> RivoMaterialShapes.VerySunny.toShape()
    17 -> RivoMaterialShapes.Cookie4Sided.toShape()
    18 -> RivoMaterialShapes.Cookie6Sided.toShape()
    19 -> RivoMaterialShapes.Cookie7Sided.toShape()
    20 -> RivoMaterialShapes.Cookie9Sided.toShape()
    21 -> RivoMaterialShapes.Cookie12Sided.toShape()
    22 -> RivoMaterialShapes.Clover4Leaf.toShape()
    23 -> RivoMaterialShapes.Clover8Leaf.toShape()
    24 -> RivoMaterialShapes.Burst.toShape()
    25 -> RivoMaterialShapes.SoftBurst.toShape()
    26 -> RivoMaterialShapes.Boom.toShape()
    27 -> RivoMaterialShapes.SoftBoom.toShape()
    28 -> RivoMaterialShapes.Flower.toShape()
    29 -> RivoMaterialShapes.Puffy.toShape()
    30 -> RivoMaterialShapes.PuffyDiamond.toShape()
    31 -> RivoMaterialShapes.Ghostish.toShape()
    32 -> RivoMaterialShapes.PixelCircle.toShape()
    33 -> RivoMaterialShapes.PixelTriangle.toShape()
    34 -> RivoMaterialShapes.Bun.toShape()
    35 -> RivoMaterialShapes.Heart.toShape()
    else -> MaterialTheme.shapes.large
}

@Composable
fun rivoPolygonShape(polygon: RoundedPolygon): Shape = polygon.toShape()

class RivoMorphShape(
    private val morph: Morph,
    private val progress: () -> Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        val p = progress()
        if (p.isNaN()) return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        val path = morph.toPath(p.coerceIn(0f, 1f))
        val bounds = path.getBounds()
        if (bounds.width <= 0f || bounds.height <= 0f || bounds.width.isNaN() || bounds.height.isNaN()) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        }
        val scaleX = size.width / bounds.width
        val scaleY = size.height / bounds.height
        if (scaleX.isNaN() || scaleY.isNaN() || scaleX.isInfinite() || scaleY.isInfinite()) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        }
        val matrix = Matrix()
        matrix.scale(scaleX, scaleY)
        matrix.translate(-bounds.left, -bounds.top)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

fun rivoMorph(start: RoundedPolygon, end: RoundedPolygon): Morph = Morph(start, end)

@Composable
fun rememberRivoMorph(start: RoundedPolygon, end: RoundedPolygon): Morph =
    remember(start, end) { Morph(start, end) }

@Composable
fun rememberRivoMorphShape(
    start: RoundedPolygon,
    end: RoundedPolygon,
    progress: () -> Float
): Shape {
    val morph = rememberRivoMorph(start, end)
    val currentProgress = rememberUpdatedState(progress)
    return remember(morph) { RivoMorphShape(morph) { currentProgress.value.invoke() } }
}
