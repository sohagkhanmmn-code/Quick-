package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisObsidianDark
import com.example.ui.theme.JarvisRed
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

@Composable
fun JarvisOrb(
    state: AssistantState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    isPowerSavingMode: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JarvisOrbTransition")

    // Rotation duration is relaxed when in power-saving mode
    val outerDuration = when {
        isPowerSavingMode -> 18000
        state == AssistantState.PROCESSING -> 3000
        else -> 12000
    }

    val innerDuration = when {
        isPowerSavingMode -> 14000
        state == AssistantState.PROCESSING -> 2000
        else -> 8000
    }

    // Continuous rotation of outer ring
    val rawOuterRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = outerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    // Counter rotation of middle ring
    val rawInnerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = innerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Quantize angle updates in power-saving mode (simulates stepped 24-30 FPS rendering)
    val outerRotation = if (isPowerSavingMode) {
        (rawOuterRotation / 12f).toInt() * 12f
    } else {
        rawOuterRotation
    }

    val innerRotation = if (isPowerSavingMode) {
        (rawInnerRotation / 12f).toInt() * 12f
    } else {
        rawInnerRotation
    }

    // Breathing pulse for idle
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPowerSavingMode) 3500 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Speaking wave frequency
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPowerSavingMode) 2200 else 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Determine primary & secondary colors based on state
    val (primaryGlow, secondaryGlow, coreGlow) = when {
        isPowerSavingMode && state == AssistantState.IDLE -> Triple(JarvisGold, Color(0xFFFFA000), JarvisGold.copy(alpha = 0.75f))
        state == AssistantState.IDLE -> Triple(JarvisCyan, JarvisElectricBlue, JarvisCyan.copy(alpha = 0.85f))
        state == AssistantState.LISTENING -> Triple(JarvisElectricBlue, JarvisCyan, Color(0xFF64D2FF))
        state == AssistantState.PROCESSING -> Triple(JarvisGold, Color(0xFFFF9100), JarvisGold)
        state == AssistantState.SPEAKING -> Triple(JarvisGreen, JarvisCyan, Color(0xFF00FFB2))
        state == AssistantState.ERROR -> Triple(JarvisRed, Color(0xFFFF5252), JarvisRed)
        else -> Triple(JarvisCyan, JarvisElectricBlue, JarvisCyan)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("jarvis_3d_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.88f

            val scale = when (state) {
                AssistantState.LISTENING, AssistantState.SPEAKING -> 0.92f + (audioAmplitude * 0.28f)
                AssistantState.PROCESSING -> 1.02f
                AssistantState.IDLE, AssistantState.ERROR -> idlePulse
            }

            val currentRadius = baseRadius * scale

            // 1. Ambient Glow (Lean radius in power saving mode to reduce GPU overdraw)
            val glowMultiplier = if (isPowerSavingMode) 1.08f else 1.3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlow.copy(alpha = if (isPowerSavingMode) 0.18f else if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) 0.45f else 0.25f),
                        primaryGlow.copy(alpha = if (isPowerSavingMode) 0.06f else 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * glowMultiplier
                ),
                radius = currentRadius * glowMultiplier,
                center = center
            )

            // 2. Outer Ring with Cyber Dashes (Rotates clockwise)
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryGlow.copy(alpha = 0.55f),
                    radius = currentRadius * 0.96f,
                    center = center,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f, 6f, 10f), 0f)
                    )
                )

                // Skip outer nodes in power-saving mode to reduce canvas operations
                if (!isPowerSavingMode) {
                    for (i in 0 until 4) {
                        val angle = (i * 90) * (Math.PI / 180.0)
                        val nodeX = center.x + (currentRadius * 0.96f * cos(angle)).toFloat()
                        val nodeY = center.y + (currentRadius * 0.96f * sin(angle)).toFloat()
                        drawCircle(
                            color = primaryGlow,
                            radius = 4.dp.toPx(),
                            center = Offset(nodeX, nodeY)
                        )
                    }
                }
            }

            // 3. Middle Segmented Arc Ring (Rotates counter-clockwise)
            rotate(innerRotation, pivot = center) {
                val midRadius = currentRadius * 0.78f
                drawCircle(
                    color = secondaryGlow.copy(alpha = 0.35f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 15f), 0f)
                    )
                )

                // Segmented bright accent arcs (2 arcs if power-saving, 3 otherwise)
                val arcCount = if (isPowerSavingMode) 2 else 3
                val step = 360f / arcCount
                for (i in 0 until arcCount) {
                    val startAngle = i * step + 15f
                    drawArc(
                        color = secondaryGlow,
                        startAngle = startAngle,
                        sweepAngle = 45f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2)
                    )
                }
            }

            // 4. Arc Reactor Star Spoke Geometry (Inner Ring)
            val spokeRadius = currentRadius * 0.58f
            val numPoints = if (isPowerSavingMode) 6 else 8
            val spokePath = Path()
            for (i in 0 until numPoints) {
                val angleOuter = (i * (360f / numPoints) + (outerRotation * 0.5f)) * (Math.PI / 180.0)
                val angleInner = ((i * (360f / numPoints) + (360f / numPoints / 2f)) + (outerRotation * 0.5f)) * (Math.PI / 180.0)

                val xOuter = center.x + (spokeRadius * cos(angleOuter)).toFloat()
                val yOuter = center.y + (spokeRadius * sin(angleOuter)).toFloat()

                val xInner = center.x + ((spokeRadius * 0.65f) * cos(angleInner)).toFloat()
                val yInner = center.y + ((spokeRadius * 0.65f) * sin(angleInner)).toFloat()

                if (i == 0) {
                    spokePath.moveTo(xOuter, yOuter)
                } else {
                    spokePath.lineTo(xOuter, yOuter)
                }
                spokePath.lineTo(xInner, yInner)
            }
            spokePath.close()

            drawPath(
                path = spokePath,
                color = primaryGlow.copy(alpha = 0.45f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Active Audio Acoustic Ripples
            if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
                val waveCount = if (isPowerSavingMode) 6 else 12
                for (i in 0 until waveCount) {
                    val angle = (i * (360.0 / waveCount) + wavePhase) * (Math.PI / 180.0)
                    val waveAmp = (audioAmplitude * 24.dp.toPx()).coerceAtLeast(6.dp.toPx())
                    val r1 = currentRadius * 0.60f
                    val r2 = r1 + waveAmp
                    val start = Offset(center.x + (r1 * cos(angle)).toFloat(), center.y + (r1 * sin(angle)).toFloat())
                    val end = Offset(center.x + (r2 * cos(angle)).toFloat(), center.y + (r2 * sin(angle)).toFloat())
                    drawLine(
                        color = coreGlow.copy(alpha = 0.8f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 6. Central Arc Reactor Core Sphere
            val coreRadius = currentRadius * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreGlow,
                        primaryGlow.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 7. Core Inner Ring
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.45f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Central Node
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = center
            )
        }

        // Power Saving Mode Badge Overlay
        if (isPowerSavingMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisObsidianDark.copy(alpha = 0.9f))
                    .border(1.dp, JarvisGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatterySaver,
                    contentDescription = "Power Saver Active",
                    tint = JarvisGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PWR-SAVE • 30 FPS",
                    color = JarvisGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
