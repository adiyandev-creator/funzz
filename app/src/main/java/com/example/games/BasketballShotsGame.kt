package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import kotlin.math.sqrt
import kotlin.random.Random

class BasketballShotsGame : MiniGame {
    override val id: String = "basketball_shots"
    override val title: String = "Basketball Shots"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    class Ball(
        val player: Player,
        var x: Float,
        var y: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var isAirborne: Boolean = false,
        var hasScored: Boolean = false,
        val radius: Float = 16f
    )

    private val balls = mutableListOf<Ball>()
    val scores = mutableMapOf<Int, Int>()

    private var hoopPos: Offset = Offset.Zero
    private val rimRadius: Float = 28f
    private val backboardWidth: Float = 14f
    private val backboardHeight: Float = 90f

    var matchTimer: Float = 35f
    private var gameOver: Boolean = false

    // Drag aim vectors per player
    private val dragStartMap = mutableMapOf<Int, Offset>()
    private val dragCurrentMap = mutableMapOf<Int, Offset>()
    private val joystickAimMap = mutableMapOf<Int, Offset>()
    private val particleSystem = com.example.engine.ParticleSystem()

    // AI timer
    private val aiShotTimers = mutableMapOf<Int, Float>()

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        // Hoop located on right-center or top-center
        hoopPos = Offset(bounds.width * 0.82f, bounds.height * 0.38f)
        scores.clear()
        players.forEach { scores[it.id] = 0 }
        reset()
    }

    override fun reset() {
        matchTimer = 35f
        gameOver = false
        balls.clear()
        dragStartMap.clear()
        dragCurrentMap.clear()
        aiShotTimers.clear()

        val spawnX = bounds.width * 0.22f
        val startY = bounds.height * 0.55f
        val spacing = bounds.height * 0.10f

        players.forEachIndexed { index, player ->
            val yPos = startY + (index - (players.size - 1) / 2f) * spacing
            balls.add(Ball(player = player, x = spawnX, y = yPos))
            if (player.isAi) aiShotTimers[player.id] = Random.nextFloat() * 1.5f
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        matchTimer -= deltaSeconds
        if (matchTimer <= 0f) {
            matchTimer = 0f
            gameOver = true
            soundAndHaptics?.playVictory()
            return
        }

        val gravity = 980f * deltaSeconds
        particleSystem.update(deltaSeconds, gravity = 300f)

        // AI shooting
        players.filter { it.isAi }.forEach { ai ->
            var timer = aiShotTimers[ai.id] ?: 0f
            timer += deltaSeconds
            if (timer >= 1.6f) {
                timer = 0f
                shootAiBall(ai)
            }
            aiShotTimers[ai.id] = timer
        }

        // Update ball physics
        balls.forEach { ball ->
            if (ball.isAirborne) {
                ball.vy += gravity
                ball.x += ball.vx * deltaSeconds
                ball.y += ball.vy * deltaSeconds

                // Check collision with backboard
                val bbX = hoopPos.x + rimRadius + 6f
                val bbTop = hoopPos.y - backboardHeight / 2f
                val bbBottom = hoopPos.y + backboardHeight / 2f

                if (ball.x + ball.radius >= bbX && ball.x - ball.radius <= bbX + backboardWidth &&
                    ball.y in bbTop..bbBottom
                ) {
                    ball.vx = -kotlin.math.abs(ball.vx) * 0.65f
                    ball.x = bbX - ball.radius
                    particleSystem.emitSparks(bbX, ball.y, Color(0xFFFBBF24), count = 8)
                    soundAndHaptics?.playHit()
                }

                // Check collision with rim
                val rimLeft = Offset(hoopPos.x - rimRadius, hoopPos.y)
                val rimRight = Offset(hoopPos.x + rimRadius, hoopPos.y)

                checkRimBounce(ball, rimLeft)
                checkRimBounce(ball, rimRight)

                // Check basket score (passing through rim downwards)
                if (!ball.hasScored && ball.vy > 0 &&
                    ball.x in (hoopPos.x - rimRadius * 0.85f)..(hoopPos.x + rimRadius * 0.85f) &&
                    ball.y in (hoopPos.y - 12f)..(hoopPos.y + 18f)
                ) {
                    ball.hasScored = true
                    scores[ball.player.id] = (scores[ball.player.id] ?: 0) + 2
                    particleSystem.emitConfetti(hoopPos.x, hoopPos.y, listOf(Color(0xFFEA580C), Color(0xFFFBBF24), Color.White))
                    particleSystem.emitShockwave(hoopPos.x, hoopPos.y, Color(0xFFEA580C), maxRadius = 45f)
                    soundAndHaptics?.playScore()
                }

                // Ball hits floor or goes offscreen -> respawn
                if (ball.y > bounds.height - 30f || ball.x > bounds.width + 40f || ball.x < -40f) {
                    resetBall(ball)
                }
            }
        }
    }

    private fun checkRimBounce(ball: Ball, rim: Offset) {
        val dx = ball.x - rim.x
        val dy = ball.y - rim.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < ball.radius + 6f && dist > 0.001f) {
            val nx = dx / dist
            val ny = dy / dist
            val dot = ball.vx * nx + ball.vy * ny
            ball.vx = (ball.vx - 1.6f * dot * nx) * 0.75f
            ball.vy = (ball.vy - 1.6f * dot * ny) * 0.75f
            soundAndHaptics?.playHit()
        }
    }

    private fun shootAiBall(player: Player) {
        val ball = balls.find { it.player.id == player.id } ?: return
        if (ball.isAirborne) return

        // Calibrate trajectory towards hoop with slight random noise
        val dx = hoopPos.x - ball.x
        val dy = hoopPos.y - ball.y
        val shotVx = (dx * 1.55f) + (Random.nextFloat() * 40f - 20f)
        val shotVy = -sqrt(2f * 980f * (kotlin.math.abs(dy) + 160f)) * 0.85f + (Random.nextFloat() * 40f - 20f)

        ball.vx = shotVx
        ball.vy = shotVy
        ball.isAirborne = true
        ball.hasScored = false
        soundAndHaptics?.playTap()
    }

    private fun resetBall(ball: Ball) {
        ball.isAirborne = false
        ball.hasScored = false
        ball.vx = 0f
        ball.vy = 0f
        ball.x = bounds.width * 0.22f
        val idx = players.indexOfFirst { it.id == ball.player.id }
        val spacing = bounds.height * 0.10f
        ball.y = bounds.height * 0.55f + (idx - (players.size - 1) / 2f) * spacing
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        val ball = balls.find { it.player.id == playerId } ?: return
        if (ball.isAirborne) return

        val mag = sqrt(x * x + y * y)
        if (mag > 0.15f) {
            joystickAimMap[playerId] = Offset(-x, -y)
        } else if (joystickAimMap.containsKey(playerId)) {
            val aim = joystickAimMap.remove(playerId) ?: return
            val shotSpeed = 640f
            ball.vx = (aim.x * shotSpeed).coerceIn(200f, 680f)
            ball.vy = (aim.y * shotSpeed).coerceIn(-720f, -250f)
            ball.isAirborne = true
            ball.hasScored = false
            soundAndHaptics?.playTap()
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        val ball = balls.find { it.player.id == playerId } ?: return
        if (!ball.isAirborne) {
            dragStartMap[playerId] = position
            dragCurrentMap[playerId] = position
        }
    }

    override fun onPointerMove(playerId: Int, position: Offset) {
        if (dragStartMap.containsKey(playerId)) {
            dragCurrentMap[playerId] = position
        }
    }

    override fun onPointerUp(playerId: Int) {
        val start = dragStartMap.remove(playerId) ?: return
        val current = dragCurrentMap.remove(playerId) ?: start
        val ball = balls.find { it.player.id == playerId } ?: return

        if (!ball.isAirborne) {
            // Drag backwards to shoot forwards (slingshot mechanic)
            val pullX = (start.x - current.x) * 3.8f
            val pullY = (start.y - current.y) * 3.8f

            ball.vx = pullX.coerceIn(180f, 650f)
            ball.vy = pullY.coerceIn(-680f, -220f)
            ball.isAirborne = true
            ball.hasScored = false
            soundAndHaptics?.playTap()
        }
    }

    override fun onButtonTap(playerId: Int) {
        val ball = balls.find { it.player.id == playerId } ?: return
        if (!ball.isAirborne) {
            // Quick preset shot
            ball.vx = 420f + (Random.nextFloat() * 40f)
            ball.vy = -540f + (Random.nextFloat() * 40f)
            ball.isAirborne = true
            ball.hasScored = false
            soundAndHaptics?.playTap()
        }
    }

    override fun getPlayerScore(playerId: Int): Int = scores[playerId] ?: 0
    override fun getRemainingSeconds(): Int? = matchTimer.toInt()

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val ranked = players.sortedByDescending { scores[it.id] ?: 0 }
        val playerResults = ranked.mapIndexed { index, player ->
            PlayerResult(
                player = player,
                score = scores[player.id] ?: 0,
                rank = index + 1
            )
        }

        return GameResult(
            gameId = id,
            rankings = playerResults,
            durationSeconds = 35,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        // Floor line
        val floorY = bounds.height - 40f
        drawScope.drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, floorY),
            end = Offset(bounds.width, floorY),
            strokeWidth = 6f
        )

        // Backboard
        val bbX = hoopPos.x + rimRadius + 6f
        val bbTop = hoopPos.y - backboardHeight / 2f
        drawScope.drawRect(
            color = Color.White,
            topLeft = Offset(bbX, bbTop),
            size = Size(backboardWidth, backboardHeight)
        )
        // Backboard red inner square
        drawScope.drawRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(bbX, hoopPos.y - 20f),
            size = Size(backboardWidth, 40f),
            style = Stroke(width = 4f)
        )

        // Hoop Rim (Orange)
        drawScope.drawLine(
            color = Color(0xFFF97316),
            start = Offset(hoopPos.x - rimRadius, hoopPos.y),
            end = Offset(hoopPos.x + rimRadius, hoopPos.y),
            strokeWidth = 8f
        )

        // Net (White lines)
        val netBottomY = hoopPos.y + 36f
        val netWidth = rimRadius * 1.5f
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(hoopPos.x - rimRadius, hoopPos.y),
            end = Offset(hoopPos.x - netWidth * 0.4f, netBottomY),
            strokeWidth = 3f
        )
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(hoopPos.x + rimRadius, hoopPos.y),
            end = Offset(hoopPos.x + netWidth * 0.4f, netBottomY),
            strokeWidth = 3f
        )
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(hoopPos.x, hoopPos.y),
            end = Offset(hoopPos.x, netBottomY),
            strokeWidth = 3f
        )

        // Draw Aim Trajectory Guide
        dragStartMap.forEach { (pId, start) ->
            val cur = dragCurrentMap[pId] ?: start
            val ball = balls.find { it.player.id == pId } ?: return@forEach
            val pullX = (start.x - cur.x) * 3.8f
            val pullY = (start.y - cur.y) * 3.8f

            var simX = ball.x
            var simY = ball.y
            var simVx = pullX.coerceIn(180f, 650f)
            var simVy = pullY.coerceIn(-680f, -220f)
            val dt = 0.04f

            for (step in 0..12) {
                simVy += 980f * dt
                simX += simVx * dt
                simY += simVy * dt
                drawScope.drawCircle(
                    color = ball.player.color.copy(alpha = (1f - step / 14f)),
                    center = Offset(simX, simY),
                    radius = 4f
                )
            }
        }

        // Draw Joystick Aim Trajectory
        joystickAimMap.forEach { (pId, aim) ->
            val ball = balls.find { it.player.id == pId } ?: return@forEach
            var simX = ball.x
            var simY = ball.y
            var simVx = (aim.x * 640f).coerceIn(200f, 680f)
            var simVy = (aim.y * 640f).coerceIn(-720f, -250f)
            val dt = 0.04f

            for (step in 0..14) {
                simVy += 980f * dt
                simX += simVx * dt
                simY += simVy * dt
                drawScope.drawCircle(
                    color = ball.player.color.copy(alpha = (1f - step / 16f)),
                    center = Offset(simX, simY),
                    radius = 5f
                )
            }
        }

        // Draw Balls
        balls.forEach { ball ->
            val center = Offset(ball.x, ball.y)
            // Outer rubber
            drawScope.drawCircle(
                color = Color(0xFFEA580C), // Basketball orange
                center = center,
                radius = ball.radius
            )
            // Seams
            drawScope.drawCircle(
                color = Color(0xFF1E293B),
                center = center,
                radius = ball.radius,
                style = Stroke(width = 2.5f)
            )
            // Player tint indicator
            drawScope.drawCircle(
                color = ball.player.color,
                center = center,
                radius = ball.radius * 0.45f
            )
        }

        particleSystem.draw(drawScope)
    }
}
