package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SpinnerBattleGame : MiniGame {
    override val id: String = "spinner_battle"
    override val title: String = "Spinner Battle"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null
    private val particleSystem = com.example.engine.ParticleSystem()

    class Spinner(
        val player: Player,
        var x: Float,
        var y: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var rotation: Float = 0f,
        var isAlive: Boolean = true,
        val radius: Float = 28f,
        var targetPos: Offset? = null
    )

    private val spinners = mutableListOf<Spinner>()
    private var arenaCenter: Offset = Offset.Zero
    private var arenaRadius: Float = 0f
    private var gameOver: Boolean = false
    private val eliminationOrder = mutableListOf<Player>()

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        arenaCenter = Offset(bounds.width / 2f, bounds.height / 2f)
        arenaRadius = minOf(bounds.width, bounds.height) * 0.43f
        reset()
    }

    override fun reset() {
        spinners.clear()
        eliminationOrder.clear()
        particleSystem.clear()
        gameOver = false

        val angleStep = (2 * Math.PI / players.size).toFloat()
        val spawnDist = arenaRadius * 0.60f

        players.forEachIndexed { index, player ->
            val angle = index * angleStep
            val spawnX = arenaCenter.x + cos(angle) * spawnDist
            val spawnY = arenaCenter.y + sin(angle) * spawnDist
            spinners.add(
                Spinner(
                    player = player,
                    x = spawnX,
                    y = spawnY
                )
            )
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        particleSystem.update(deltaSeconds, gravity = 180f)

        val aliveSpinners = spinners.filter { it.isAlive }
        if (aliveSpinners.size <= 1 && players.size > 1) {
            aliveSpinners.firstOrNull()?.let { winner ->
                eliminationOrder.add(winner.player)
            }
            gameOver = true
            soundAndHaptics?.playVictory()
            return
        }

        spinners.forEach { spinner ->
            if (!spinner.isAlive) return@forEach

            // Fast continuous spin
            spinner.rotation += 840f * deltaSeconds

            // AI bot behavior
            if (spinner.player.isAi) {
                val distFromCenter = sqrt(
                    (spinner.x - arenaCenter.x) * (spinner.x - arenaCenter.x) +
                    (spinner.y - arenaCenter.y) * (spinner.y - arenaCenter.y)
                )

                if (distFromCenter > arenaRadius * 0.70f) {
                    spinner.targetPos = arenaCenter
                } else {
                    val target = spinners.filter { it.isAlive && it != spinner }
                        .minByOrNull {
                            val dX = it.x - spinner.x
                            val dY = it.y - spinner.y
                            dX * dX + dY * dY
                        }
                    target?.let { spinner.targetPos = Offset(it.x, it.y) }
                }
            }

            // Move towards target / touch steer
            spinner.targetPos?.let { target ->
                val dx = target.x - spinner.x
                val dy = target.y - spinner.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 8f) {
                    val steerAngle = atan2(dy, dx)
                    val accel = 480f * deltaSeconds
                    spinner.vx += cos(steerAngle) * accel
                    spinner.vy += sin(steerAngle) * accel
                }
            }

            // Physics friction
            spinner.vx *= 0.985f
            spinner.vy *= 0.985f

            val speed = sqrt(spinner.vx * spinner.vx + spinner.vy * spinner.vy)
            if (speed > 580f) {
                spinner.vx = (spinner.vx / speed) * 580f
                spinner.vy = (spinner.vy / speed) * 580f
            }

            spinner.x += spinner.vx * deltaSeconds
            spinner.y += spinner.vy * deltaSeconds

            // Check ring out
            val distCenter = sqrt(
                (spinner.x - arenaCenter.x) * (spinner.x - arenaCenter.x) +
                (spinner.y - arenaCenter.y) * (spinner.y - arenaCenter.y)
            )

            if (distCenter > arenaRadius + spinner.radius) {
                spinner.isAlive = false
                eliminationOrder.add(spinner.player)
                particleSystem.emitSparks(spinner.x, spinner.y, spinner.player.color, count = 22)
                particleSystem.emitShockwave(spinner.x, spinner.y, Color(0xFFEF4444), maxRadius = 55f)
                soundAndHaptics?.playHit()
            }
        }

        // Handle collision between alive spinners
        for (i in spinners.indices) {
            val s1 = spinners[i]
            if (!s1.isAlive) continue

            for (j in (i + 1) until spinners.size) {
                val s2 = spinners[j]
                if (!s2.isAlive) continue

                val dx = s2.x - s1.x
                val dy = s2.y - s1.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = s1.radius + s2.radius

                if (dist < minDist && dist > 0.001f) {
                    val nx = dx / dist
                    val ny = dy / dist

                    val overlap = 0.5f * (minDist - dist)
                    s1.x -= nx * overlap
                    s1.y -= ny * overlap
                    s2.x += nx * overlap
                    s2.y += ny * overlap

                    val kx = s1.vx - s2.vx
                    val ky = s1.vy - s2.vy
                    val p = (nx * kx + ny * ky)
                    val bounceFactor = 1.7f

                    s1.vx -= p * nx * bounceFactor
                    s1.vy -= p * ny * bounceFactor
                    s2.vx += p * nx * bounceFactor
                    s2.vy += p * ny * bounceFactor

                    val knockImpulse = 200f
                    s1.vx -= nx * knockImpulse
                    s1.vy -= ny * knockImpulse
                    s2.vx += nx * knockImpulse
                    s2.vy += ny * knockImpulse

                    val hitX = (s1.x + s2.x) / 2f
                    val hitY = (s1.y + s2.y) / 2f
                    particleSystem.emitSparks(hitX, hitY, Color(0xFFFBBF24), count = 16)
                    particleSystem.emitShockwave(hitX, hitY, Color.White, maxRadius = 36f)

                    soundAndHaptics?.playHit()
                }
            }
        }
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        val spinner = spinners.find { it.player.id == playerId && it.isAlive } ?: return
        if (x != 0f || y != 0f) {
            spinner.vx += x * 38f
            spinner.vy += y * 38f
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        val spinner = spinners.find { it.player.id == playerId && it.isAlive } ?: return
        spinner.targetPos = position
    }

    override fun onPointerMove(playerId: Int, position: Offset) {
        val spinner = spinners.find { it.player.id == playerId && it.isAlive } ?: return
        spinner.targetPos = position
    }

    override fun onPointerUp(playerId: Int) {
        val spinner = spinners.find { it.player.id == playerId } ?: return
        spinner.targetPos = null
    }

    override fun onButtonTap(playerId: Int) {
        // Boost impulse in current direction or towards nearest rival
        val spinner = spinners.find { it.player.id == playerId && it.isAlive } ?: return
        val other = spinners.filter { it.isAlive && it != spinner }.minByOrNull {
            (it.x - spinner.x) * (it.x - spinner.x) + (it.y - spinner.y) * (it.y - spinner.y)
        }
        val target = other?.let { Offset(it.x, it.y) } ?: arenaCenter
        val angle = atan2(target.y - spinner.y, target.x - spinner.x)
        spinner.vx += cos(angle) * 240f
        spinner.vy += sin(angle) * 240f
        soundAndHaptics?.playTap()
    }

    override fun getPlayerScore(playerId: Int): Int =
        if (spinners.find { it.player.id == playerId }?.isAlive == true) 1 else 0

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val reversed = eliminationOrder.reversed()
        val playerResults = reversed.mapIndexed { index, player ->
            PlayerResult(
                player = player,
                score = (players.size - index) * 10,
                rank = index + 1
            )
        }

        return GameResult(
            gameId = id,
            rankings = playerResults,
            durationSeconds = 30,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        // Deep purple canvas background (Reference 3)
        drawScope.drawRect(Color(0xFF38254A))

        // Ambient dark decorative background spots
        drawScope.drawCircle(
            color = Color(0xFF2C1C3B),
            center = Offset(bounds.width * 0.15f, bounds.height * 0.15f),
            radius = 120f
        )
        drawScope.drawCircle(
            color = Color(0xFF2C1C3B),
            center = Offset(bounds.width * 0.85f, bounds.height * 0.85f),
            radius = 140f
        )

        // Warm Golden Arena Disk (Reference 3)
        drawScope.drawCircle(
            color = Color(0xFFF9D68D),
            center = arenaCenter,
            radius = arenaRadius
        )

        // Shaded Arena Outer Rim (Dark wood / shadow)
        drawScope.drawCircle(
            color = Color(0xFFE8BD68),
            center = arenaCenter,
            radius = arenaRadius - 6f,
            style = Stroke(width = 14f)
        )

        // Bold Arena Dark Border
        drawScope.drawCircle(
            color = Color(0xFF23182C),
            center = arenaCenter,
            radius = arenaRadius,
            style = Stroke(width = 12f)
        )

        // Draw Buzzsaw Spinners (Reference 3)
        spinners.forEach { spinner ->
            if (!spinner.isAlive) return@forEach

            val center = Offset(spinner.x, spinner.y)
            val rad = spinner.radius
            val pColor = spinner.player.color

            // Buzzsaw blade path with 8 sharp triangular teeth
            val teeth = 8
            val path = Path()
            val outerR = rad * 1.35f
            val innerR = rad * 0.82f

            for (i in 0 until (teeth * 2)) {
                val toothAngle = Math.toRadians((spinner.rotation + i * (360.0 / (teeth * 2))).toDouble())
                val r = if (i % 2 == 0) outerR else innerR
                val px = (center.x + cos(toothAngle) * r).toFloat()
                val py = (center.y + sin(toothAngle) * r).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            // Buzzsaw dark body with thick black outline
            drawScope.drawPath(path, color = Color(0xFF1E293B))
            drawScope.drawPath(path, color = Color.Black, style = Stroke(width = 4f))

            // Player's colored circular ring
            drawScope.drawCircle(
                color = pColor,
                center = center,
                radius = rad * 0.58f
            )
            drawScope.drawCircle(
                color = Color.Black,
                center = center,
                radius = rad * 0.58f,
                style = Stroke(width = 3f)
            )

            // Inner dark recess and white center hub
            drawScope.drawCircle(
                color = Color(0xFF0F172A),
                center = center,
                radius = rad * 0.32f
            )
            drawScope.drawCircle(
                color = Color.White,
                center = center,
                radius = rad * 0.16f
            )
        }

        particleSystem.draw(drawScope)
    }
}
