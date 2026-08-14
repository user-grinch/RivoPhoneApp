package com.grinch.rivo4.view.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoMorphShape
import com.grinch.rivo4.view.theme.RivoMotion
import com.grinch.rivo4.view.theme.rememberRivoMorph
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoCornerDp

data class MorphingPage(
    val icon: ImageVector,
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val descriptionRes: Int,
    val morphProgress: Float,
    val rotation: Float,
    val scale: Float
)

private val pages = listOf(
    MorphingPage(
        icon = Icons.Default.Palette,
        titleRes = R.string.onboarding_page1_title,
        descriptionRes = R.string.onboarding_page1_description,
        morphProgress = 0f,
        rotation = 0f,
        scale = 1f
    ),
    MorphingPage(
        icon = Icons.Default.Dialpad,
        titleRes = R.string.onboarding_page2_title,
        descriptionRes = R.string.onboarding_page2_description,
        morphProgress = 0.5f,
        rotation = 30f,
        scale = 1.15f
    ),
    MorphingPage(
        icon = Icons.Default.Security,
        titleRes = R.string.onboarding_page3_title,
        descriptionRes = R.string.onboarding_page3_description,
        morphProgress = 1f,
        rotation = 0f,
        scale = 1f
    )
)

@Composable
fun MorphingOnboardingScreen(onFinished: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val roundness = LocalCardRoundness.current

    val morphProgress by animateFloatAsState(
        targetValue = pages[currentPage].morphProgress,
        animationSpec = RivoMotion.shapeMorph(),
        label = "onboardingMorph"
    )
    val rotation by animateFloatAsState(
        targetValue = pages[currentPage].rotation,
        animationSpec = RivoMotion.spatialDefault(),
        label = "onboardingRotation"
    )
    val scale by animateFloatAsState(
        targetValue = pages[currentPage].scale,
        animationSpec = RivoMotion.spatialDefault(),
        label = "onboardingScale"
    )
    val shapeSize by animateDpAsState(
        targetValue = (144 * pages[currentPage].scale).dp,
        animationSpec = RivoMotion.spatialDefault(),
        label = "onboardingSize"
    )

    val heroMorph = rememberRivoMorph(RivoMaterialShapes.Circle, RivoMaterialShapes.Cookie12Sided)
    val heroShape = RivoMorphShape(heroMorph) { morphProgress }

    val bgMorph = rememberRivoMorph(RivoMaterialShapes.Cookie9Sided, RivoMaterialShapes.Circle)
    val bgShape = RivoMorphShape(bgMorph) { morphProgress }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background expressive morphing shapes
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = (-60).dp, y = 80.dp)
                    .rotate(rotation * 1.5f)
                    .clip(bgShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            )

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = 180.dp)
                    .rotate(-rotation)
                    .clip(heroShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
            )

            // Skip button
            TextButton(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Main morphing shape with icon
                Surface(
                    modifier = Modifier
                        .size(shapeSize)
                        .rotate(rotation),
                    shape = heroShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = pages[currentPage].icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .rotate(-rotation),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Title & Description with MD3 Expressive animated text
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (fadeIn() togetherWith fadeOut())
                    },
                    label = "onboardingTextTransition"
                ) { pageIdx ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(pages[pageIdx].titleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(pages[pageIdx].descriptionRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Expressive fluid page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isCurrent = index == currentPage
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isCurrent) 28.dp else 10.dp,
                            animationSpec = RivoMotion.shapeMorph(),
                            label = "indicatorWidth"
                        )
                        val indicatorColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(indicatorWidth, 10.dp)
                                .clip(RoundedCornerShape(rivoCornerDp(12, roundness)))
                                .background(indicatorColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Navigation controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage > 0) {
                        TextButton(onClick = { currentPage-- }) {
                            Text(
                                text = stringResource(R.string.action_back),
                                style = MaterialTheme.typography.labelLargeEmphasized
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onFinished()
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = if (currentPage == pages.size - 1) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
