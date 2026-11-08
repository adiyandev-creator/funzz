package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    val color: Color,
    var life: Float = 1.0f,
    val maxLife: Float = 1.0f,
    val isRing: Boolean = false
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()

    fun emitSparks(x: Float, y: Float, color: Color, count: Int = 12, speed: Float = 220f) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val spd = (Random.nextFloat() * 0.7f + 0.3f) * speed
            val life = Random.nextFloat() * 0.4f + 0.3f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    radius = Random.nextFloat() * 4f + 2f,
                    color = color,
                    life = life,
                    maxLife = life
                )
            )
        }
    }

    fun emitShockwave(x: Float, y: Float, color: Color, maxRadius: Float = 50f) {
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                radius = maxRadius,
                color = color,
                life = 0.35f,
                maxLife = 0.35f,
                isRing = true
            )
        )
    }

    fun emitConfetti(
        x: Float,
        y: Float,
        colors: List<Color> = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFFBBF24), Color.White),
        count: Int = 20
    ) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val spd = (Random.nextFloat() * 0.8f + 0.2f) * 260f
            val life = Random.nextFloat() * 0.6f + 0.4f
            val c = colors[Random.nextInt(colors.size)]
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd - 60f,
                    radius = Random.nextFloat() * 5f + 3f,
                    color = c,
                    life = life,
                    maxLife = life
                )
            )
        }
    }

    fun update(deltaSeconds: Float, gravity: Float = 120f) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= deltaSeconds
            if (p.life <= 0f) {
                iterator.remove()
            } else {
                p.vy += gravity * deltaSeconds
                p.x += p.vx * deltaSeconds
                p.y += p.vy * deltaSeconds
            }
        }
    }

    fun draw(drawScope: DrawScope) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.isRing) {
                val progress = 1f - alpha
                val currentRadius = p.radius * progress
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    center = Offset(p.x, p.y),
                    radius = currentRadius,
                    style = Stroke(width = 4f * alpha + 1f)
                )
            } else {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    center = Offset(p.x, p.y),
                    radius = p.radius * alpha
                )
            }
        }
    }

    fun clear() {
        particles.clear()
    }
}
