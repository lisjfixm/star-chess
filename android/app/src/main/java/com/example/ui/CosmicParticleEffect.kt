package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class CosmicParticle(
    var normX: Float,
    var normY: Float,
    var vx: Float,
    var vy: Float,
    val radiusDp: Float,
    val maxAlpha: Float,
    val colorSlot: Int, // 0 = primary, 1 = secondary, 2 = gold, 3 = starlight white
    val isSparkle: Boolean,
    val twinklePhase: Float,
    val twinkleFreq: Float,
    val driftPhase: Float,
    var ageSec: Float,
    val lifeSec: Float,
)

private data class StarlightWaveRing(
    val originNormX: Float,
    val originNormY: Float,
    var ageSec: Float,
    val lifeSec: Float,
    val colorSlot: Int,
)

/**
 * Subtle, pointer-transparent cosmic particle background overlay triggered when a star icon is clicked.
 * Emits soft starlight ripples and gently drifting stardust motes across the viewport, then idles at
 * zero frame cost once all particles naturally fade out.
 */
@Composable
fun CosmicParticleOverlay(
    burstTrigger: Int,
    originNormX: Float = 0.08f,
    originNormY: Float = 0.05f,
    primary: Color,
    secondary: Color,
    gold: Color,
    modifier: Modifier = Modifier,
) {
    val particles = remember { mutableListOf<CosmicParticle>() }
    val rings = remember { mutableListOf<StarlightWaveRing>() }
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(burstTrigger) {
        if (burstTrigger <= 0) return@LaunchedEffect
        val rng = Random(System.nanoTime() + burstTrigger * 7919L)

        // Soft expanding starlight rings from the clicked star icon
        rings.add(
            StarlightWaveRing(
                originNormX = originNormX,
                originNormY = originNormY,
                ageSec = 0f,
                lifeSec = 1.6f,
                colorSlot = burstTrigger % 3,
            )
        )
        rings.add(
            StarlightWaveRing(
                originNormX = originNormX,
                originNormY = originNormY,
                ageSec = -0.18f,
                lifeSec = 1.9f,
                colorSlot = (burstTrigger + 2) % 3,
            )
        )

        // Emit subtle radial stardust motes from the star origin + ambient shimmering dust across menu
        val burstCount = 26
        repeat(burstCount) { idx ->
            val fromOrigin = idx < 18
            val angle = if (fromOrigin) {
                rng.nextFloat() * (2f * PI.toFloat())
            } else {
                rng.nextFloat() * (2f * PI.toFloat())
            }
            val speed = if (fromOrigin) {
                0.055f + rng.nextFloat() * 0.145f
            } else {
                0.012f + rng.nextFloat() * 0.032f
            }
            val startX = if (fromOrigin) {
                (originNormX + (rng.nextFloat() - 0.5f) * 0.04f).coerceIn(0.02f, 0.98f)
            } else {
                rng.nextFloat()
            }
            val startY = if (fromOrigin) {
                (originNormY + (rng.nextFloat() - 0.5f) * 0.03f).coerceIn(0.01f, 0.96f)
            } else {
                0.08f + rng.nextFloat() * 0.84f
            }
            particles.add(
                CosmicParticle(
                    normX = startX,
                    normY = startY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed + if (fromOrigin) 0.025f else -0.010f,
                    radiusDp = 1.5f + rng.nextFloat() * 2.8f,
                    maxAlpha = 0.28f + rng.nextFloat() * 0.34f,
                    colorSlot = rng.nextInt(4),
                    isSparkle = idx % 3 == 0,
                    twinklePhase = rng.nextFloat() * 6.28f,
                    twinkleFreq = 2.2f + rng.nextFloat() * 3.4f,
                    driftPhase = rng.nextFloat() * 6.28f,
                    ageSec = 0f,
                    lifeSec = 3.2f + rng.nextFloat() * 2.8f,
                )
            )
        }

        // Keep particle count subtle and lightweight even on rapid taps
        while (particles.size > 90) {
            particles.removeAt(0)
        }
        while (rings.size > 6) {
            rings.removeAt(0)
        }

        // Drive frame loop only while particles or rings remain alive
        var lastNanos = 0L
        while (particles.isNotEmpty() || rings.isNotEmpty()) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos

                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.ageSec += dt
                    if (p.ageSec >= p.lifeSec) {
                        pIter.remove()
                        continue
                    }
                    // Gentle cosmic drag + subtle harmonic horizontal drift
                    val drag = 1f - (0.38f * dt)
                    p.vx *= drag
                    p.vy *= drag
                    val sway = sin(p.ageSec * 1.4f + p.driftPhase) * 0.008f
                    p.normX += (p.vx + sway) * dt
                    p.normY += p.vy * dt
                }

                val rIter = rings.iterator()
                while (rIter.hasNext()) {
                    val r = rIter.next()
                    r.ageSec += dt
                    if (r.ageSec >= r.lifeSec) {
                        rIter.remove()
                    }
                }

                frameTick = frameNanos
            }
        }
        frameTick = 0L
    }

    if (frameTick == 0L && particles.isEmpty() && rings.isEmpty()) return

    Canvas(modifier = modifier) {
        // Read frameTick inside draw lambda to trigger invalidation per frame
        val tick = frameTick
        if (tick == 0L && particles.isEmpty() && rings.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val maxDim = size.maxDimension

        // Draw subtle expanding starlight rings
        for (ring in rings) {
            if (ring.ageSec < 0f) continue
            val progress = (ring.ageSec / ring.lifeSec).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress) * (1f - progress)
            val ringAlpha = ((1f - progress) * 0.24f).coerceIn(0f, 0.24f)
            val ringColor = when (ring.colorSlot) {
                0 -> primary
                1 -> secondary
                else -> gold
            }
            val center = Offset(w * ring.originNormX, h * ring.originNormY)
            val radius = (18.dp.toPx()) + eased * maxDim * 0.42f
            drawCircle(
                color = ringColor.copy(alpha = ringAlpha),
                radius = radius,
                center = center,
                style = Stroke(width = (1.6f * (1f - progress * 0.6f)).dp.toPx()),
            )
        }

        // Draw subtle stardust motes and delicate 4-point sparkles
        for (p in particles) {
            val progress = (p.ageSec / p.lifeSec).coerceIn(0f, 1f)
            val fadeIn = (p.ageSec / 0.35f).coerceIn(0f, 1f)
            val fadeOut = (1f - progress).let { it * it }
            val twinkle = 0.62f + 0.38f * sin(p.ageSec * p.twinkleFreq + p.twinklePhase)
            val alpha = (p.maxAlpha * fadeIn * fadeOut * twinkle).coerceIn(0f, 0.68f)
            if (alpha <= 0.01f) continue

            val cx = w * p.normX
            val cy = h * p.normY
            val baseColor = when (p.colorSlot) {
                0 -> primary
                1 -> secondary
                2 -> gold
                else -> Color.White
            }
            val radiusPx = p.radiusDp.dp.toPx()

            // Soft halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = alpha * 0.55f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radiusPx * 3.2f,
                ),
                radius = radiusPx * 3.2f,
                center = Offset(cx, cy),
            )

            if (p.isSparkle) {
                val starPath = Path().apply {
                    val outer = radiusPx * 1.9f
                    val inner = radiusPx * 0.45f
                    moveTo(cx, cy - outer)
                    lineTo(cx + inner, cy - inner)
                    lineTo(cx + outer, cy)
                    lineTo(cx + inner, cy + inner)
                    lineTo(cx, cy + outer)
                    lineTo(cx - inner, cy + inner)
                    lineTo(cx - outer, cy)
                    lineTo(cx - inner, cy - inner)
                    close()
                }
                drawPath(path = starPath, color = baseColor.copy(alpha = alpha))
            } else {
                drawCircle(
                    color = baseColor.copy(alpha = alpha),
                    radius = radiusPx,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}
