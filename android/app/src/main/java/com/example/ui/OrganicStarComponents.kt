package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Computes a continuous, non-uniform organic rotation state driven by frame time.
 * Uses a superposition of incommensurate frequencies so angular velocity continuously
 * varies — accelerating, drifting, briefly lingering or subtly oscillating back, and
 * surging forward again — never falling into a mechanical constant-speed loop.
 */
data class OrganicMotionState(
    val primaryAngleDeg: Float,
    val secondaryAngleDeg: Float,
    val pulseScale: Float,
    val glowAlpha: Float,
    val elapsedSec: Float,
)

@Composable
fun rememberOrganicStarMotion(
    seedOffset: Float = 0f,
    speedMultiplier: Float = 1f,
): OrganicMotionState {
    var primaryAngle by remember { mutableFloatStateOf(seedOffset * 37f) }
    var secondaryAngle by remember { mutableFloatStateOf(seedOffset * -23f) }
    var elapsedSec by remember { mutableFloatStateOf(seedOffset) }

    LaunchedEffect(seedOffset, speedMultiplier) {
        var lastNanos = 0L
        var t = seedOffset.toDouble()
        var angle = (seedOffset * 37f).toDouble()
        var ringAngle = (seedOffset * -23f).toDouble()

        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                lastNanos = frameNanos
                t += dt

                // Irregular angular velocity (deg/sec) composed of incommensurate waves:
                // drifts slow, surges smoothly, occasionally dips slightly negative for a cosmic flutter.
                val baseDrift = 26.0
                val waveA = 34.0 * sin(t * 0.71)
                val waveB = 22.0 * cos(t * 1.37 + 0.8)
                val waveC = 14.0 * sin(t * 2.29 + 2.1)
                val burst = 18.0 * sin(t * 0.31) * cos(t * 0.89)
                val angularVelocity = (baseDrift + waveA + waveB + waveC + burst) * speedMultiplier

                // Outer orbit arc drifts independently with a counter-weighted irregular velocity
                val ringVelocity = (-18.0 + 24.0 * cos(t * 0.59 + 1.4) + 15.0 * sin(t * 1.73)) * speedMultiplier

                angle += angularVelocity * dt
                ringAngle += ringVelocity * dt

                primaryAngle = angle.toFloat()
                secondaryAngle = ringAngle.toFloat()
                elapsedSec = t.toFloat()
            }
        }
    }

    val pulseScale = 1f + 0.055f * sin(elapsedSec * 1.43f) + 0.025f * cos(elapsedSec * 2.61f)
    val glowAlpha = (0.30f + 0.16f * sin(elapsedSec * 1.17f + 0.5f) + 0.08f * cos(elapsedSec * 2.11f))
        .coerceIn(0.14f, 0.58f)

    return OrganicMotionState(
        primaryAngleDeg = primaryAngle,
        secondaryAngleDeg = secondaryAngle,
        pulseScale = pulseScale,
        glowAlpha = glowAlpha,
        elapsedSec = elapsedSec,
    )
}

@Composable
fun OrganicAuthStarEmblem(
    primary: Color,
    secondary: Color,
    gold: Color,
    modifier: Modifier = Modifier,
) {
    val motion = rememberOrganicStarMotion(seedOffset = 0.4f, speedMultiplier = 1.0f)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.49f

        // Soft pulsing cosmic aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = motion.glowAlpha),
                    secondary.copy(alpha = motion.glowAlpha * 0.35f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 1.08f,
            ),
            radius = radius * 1.08f,
            center = Offset(cx, cy),
        )

        // Independent irregular orbital ring with luminous arcs
        rotate(degrees = motion.secondaryAngleDeg, pivot = Offset(cx, cy)) {
            val arcRadius = radius * 0.84f
            val arcTopLeft = Offset(cx - arcRadius, cy - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            drawCircle(
                color = primary.copy(alpha = 0.22f),
                radius = arcRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, primary, gold, Color.Transparent),
                    center = Offset(cx, cy),
                ),
                startAngle = 0f,
                sweepAngle = 128f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = secondary.copy(alpha = 0.72f),
                startAngle = 195f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            // Tiny orbiting satellite spark
            val satX = cx + arcRadius
            val satY = cy
            drawCircle(
                color = gold,
                radius = 3.2.dp.toPx(),
                center = Offset(satX, satY),
            )
        }

        // Main organically rotating five-pointed star with subtle scale breath
        rotate(degrees = motion.primaryAngleDeg, pivot = Offset(cx, cy)) {
            scale(scale = motion.pulseScale, pivot = Offset(cx, cy)) {
                val starPath = buildFivePointStarPath(
                    cx = cx,
                    cy = cy,
                    outerR = size.minDimension * 0.33f,
                    innerR = size.minDimension * 0.145f,
                )
                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(
                        colors = listOf(primary, Color.White.copy(alpha = 0.92f), secondary),
                        start = Offset(cx - radius * 0.5f, cy - radius * 0.5f),
                        end = Offset(cx + radius * 0.5f, cy + radius * 0.5f),
                    ),
                )
                drawPath(
                    path = starPath,
                    color = gold.copy(alpha = 0.88f),
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Inner crisp core facet
                val innerStar = buildFivePointStarPath(
                    cx = cx,
                    cy = cy,
                    outerR = size.minDimension * 0.13f,
                    innerR = size.minDimension * 0.055f,
                )
                drawPath(
                    path = innerStar,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
fun OrganicTopBarStarIcon(
    primary: Color,
    secondary: Color,
    gold: Color,
    modifier: Modifier = Modifier,
) {
    val motion = rememberOrganicStarMotion(seedOffset = 2.7f, speedMultiplier = 1.15f)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.5f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = motion.glowAlpha * 0.9f), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.15f,
            ),
            radius = radius * 1.15f,
            center = Offset(cx, cy),
        )

        rotate(degrees = motion.primaryAngleDeg, pivot = Offset(cx, cy)) {
            scale(scale = motion.pulseScale, pivot = Offset(cx, cy)) {
                val starPath = buildFivePointStarPath(
                    cx = cx,
                    cy = cy,
                    outerR = size.minDimension * 0.44f,
                    innerR = size.minDimension * 0.20f,
                )
                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(listOf(primary, secondary)),
                )
                drawPath(
                    path = starPath,
                    color = gold.copy(alpha = 0.85f),
                    style = Stroke(width = 1.3.dp.toPx()),
                )
            }
        }
    }
}

private fun buildFivePointStarPath(
    cx: Float,
    cy: Float,
    outerR: Float,
    innerR: Float,
): Path {
    val path = Path()
    for (i in 0 until 10) {
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
