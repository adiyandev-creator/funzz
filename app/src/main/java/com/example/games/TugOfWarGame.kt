package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.ParticleSystem
import com.example.engine.SoundAndHaptics
import kotlin.math.sin
import kotlin.random.Random

class TugOfWarGame : MiniGame {
    override val id: String = "tug_of_war"
    override val title: String = "Tug of War"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null
    private val particleSystem = ParticleSystem()

    // Rope position: -1.0 (Team Left wins) to +1.0 (Team Right wins)
    var ropePosition: Float = 0f
    private var ropeVelocity: Float = 0f

    // Team 1: Even indices (P1, P3), Team 2: Odd indices (P2, P4)
    val tapCounts = mutableMapOf<Int, Int>()
    private var gameOver: Boolean = false
    private var winningTeam: Int? = null

    // AI tap timers
    private val aiTapTimers = mutableMapOf<Int, Float>()
    private var ropeWobbleAnim: Float = 0f

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        reset()
    }

    override fun reset() {
        ropePosition = 0f
        ropeVelocity = 0f
        gameOver = false
        winningTeam = null
        tapCounts.clear()
        aiTapTimers.clear()
        particleSystem.clear()
        players.forEach {
            tapCounts[it.id] = 0
            if (it.isAi) aiTapTimers[it.id] = 0f
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) {
            particleSystem.update(deltaSeconds, gravity = 200f)
            return
        }

        particleSystem.update(deltaSeconds, gravity = 120f)
        ropeWobbleAnim += deltaSeconds * 12f

        // AI tapping
        players.filter { it.isAi }.forEach { ai ->
            var timer = aiTapTimers[ai.id] ?: 0f
            timer += deltaSeconds
            val tapInterval = 0.15f + (Random.nextFloat() * 0.06f)
            if (timer >= tapInterval) {
                timer = 0f
                onPlayerPull(ai.id)
            }
            aiTapTimers[ai.id] = timer
        }

        // Apply friction and integrate rope position
        ropeVelocity *= 0.93f
        ropePosition += ropeVelocity * deltaSeconds

        // Win threshold
        if (ropePosition <= -1.0f) {
            ropePosition = -1.0f
            gameOver = true
            winningTeam = 1 // Team Left
            soundAndHaptics?.playVictory()
            particleSystem.emitConfetti(bounds.width * 0.25f, bounds.height / 2f, count = 40)
        } else if (ropePosition >= 1.0f) {
            ropePosition = 1.0f
            gameOver = true
            winningTeam = 2 // Team Right
            soundAndHaptics?.playVictory()
            particleSystem.emitConfetti(bounds.width * 0.75f, bounds.height / 2f, count = 40)
        }
    }

    fun onPlayerPull(playerId: Int) {
        if (gameOver) return

        tapCounts[playerId] = (tapCounts[playerId] ?: 0) + 1
        soundAndHaptics?.playTap()

        val isTeam1 = (playerId % 2 == 0) // Even = Team 1 (P1, P3 pulls Left)
        val pullStrength = 0.075f

        val centerY = bounds.height / 2f
        val centerX = bounds.width / 2f
        val arenaWidth = bounds.width * 0.76f

        if (isTeam1) {
            ropeVelocity -= pullStrength
            // Emit ground dust under left team
            particleSystem.emitSparks(
                x = centerX - arenaWidth * 0.35f,
                y = centerY + 30f,
                color = Color(0xFFD1D5DB),
                count = 4,
                speed = 60f
            )
        } else {
            ropeVelocity += pullStrength
            // Emit ground dust under right team
            particleSystem.emitSparks(
                x = centerX + arenaWidth * 0.35f,
                y = centerY + 30f,
                color = Color(0xFFD1D5DB),
                count = 4,
                speed = 60f
            )
        }
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        if (gameOver) return
        val isTeam1 = (playerId % 2 == 0)
        // Pulling joystick backwards (away from center)
        if (isTeam1 && x < -0.4f) {
            ropeVelocity -= 0.025f * -x
            tapCounts[playerId] = (tapCounts[playerId] ?: 0) + 1
        } else if (!isTeam1 && x > 0.4f) {
            ropeVelocity += 0.025f * x
            tapCounts[playerId] = (tapCounts[playerId] ?: 0) + 1
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        onPlayerPull(playerId)
    }

    override fun onButtonTap(playerId: Int) {
        onPlayerPull(playerId)
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}
    override fun onPointerUp(playerId: Int) {}

    override fun getPlayerScore(playerId: Int): Int = tapCounts[playerId] ?: 0

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val rankings = mutableListOf<PlayerResult>()
        val team1Players = players.filter { it.id % 2 == 0 }
        val team2Players = players.filter { it.id % 2 != 0 }

        if (winningTeam == 1) {
            team1Players.forEach { rankings.add(PlayerResult(it, tapCounts[it.id] ?: 0, 1)) }
            team2Players.forEach { rankings.add(PlayerResult(it, tapCounts[it.id] ?: 0, 2)) }
        } else {
            team2Players.forEach { rankings.add(PlayerResult(it, tapCounts[it.id] ?: 0, 1)) }
            team1Players.forEach { rankings.add(PlayerResult(it, tapCounts[it.id] ?: 0, 2)) }
        }

        return GameResult(
            gameId = id,
            rankings = rankings,
            durationSeconds = 20,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        val centerY = bounds.height / 2f
        val centerX = bounds.width / 2f
        val arenaWidth = bounds.width * 0.78f
        val halfW = arenaWidth / 2f

        val leftBoundary = centerX - halfW
        val rightBoundary = centerX + halfW

        // Playful arena background
        drawScope.drawRect(
            color = Color(0xFF1B1B29),
            topLeft = Offset(0f, 0f),
            size = bounds
        )

        // Sandy Tug Field Mat
        drawScope.drawRect(
            color = Color(0xFF26263B),
            topLeft = Offset(leftBoundary - 25f, centerY - 80f),
            size = Size(arenaWidth + 50f, 160f)
        )
        drawScope.drawRect(
            color = Color(0xFF383854),
            topLeft = Offset(leftBoundary - 25f, centerY - 80f),
            size = Size(arenaWidth + 50f, 160f),
            style = Stroke(width = 3f)
        )

        // Center Mark Line & Mud Pit
        drawScope.drawCircle(
            color = Color(0xFF161520),
            center = Offset(centerX, centerY),
            radius = 32f
        )
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(centerX, centerY - 90f),
            end = Offset(centerX, centerY + 90f),
            strokeWidth = 3f
        )

        // Left Goal Line (Team 1 Win threshold)
        val team1Color = players.firstOrNull { it.id % 2 == 0 }?.color ?: Color(0xFF3B82F6)
        drawScope.drawLine(
            color = team1Color,
            start = Offset(leftBoundary, centerY - 85f),
            end = Offset(leftBoundary, centerY + 85f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // Right Goal Line (Team 2 Win threshold)
        val team2Color = players.firstOrNull { it.id % 2 != 0 }?.color ?: Color(0xFFEF4444)
        drawScope.drawLine(
            color = team2Color,
            start = Offset(rightBoundary, centerY - 85f),
            end = Offset(rightBoundary, centerY + 85f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // Current Ribbon X on rope
        val ribbonX = centerX + (ropePosition * halfW)
        val wobble = if (!gameOver) sin(ropeWobbleAnim) * 3f else 0f

        // Draw Team Pullers
        val team1Players = players.filter { it.id % 2 == 0 }
        val team2Players = players.filter { it.id % 2 != 0 }

        // Team 1 Characters (Left side, pulling Left)
        team1Players.forEachIndexed { idx, player ->
            val charX = ribbonX - 70f - (idx * 55f)
            val charY = centerY + wobble
            drawPuller(drawScope, charX, charY, player.color, isFacingLeft = true, isPulling = true)
        }

        // Team 2 Characters (Right side, pulling Right)
        team2Players.forEachIndexed { idx, player ->
            val charX = ribbonX + 70f + (idx * 55f)
            val charY = centerY - wobble
            drawPuller(drawScope, charX, charY, player.color, isFacingLeft = false, isPulling = true)
        }

        // Draw Thick Twisted Hemp Rope
        val ropeLength = arenaWidth * 1.35f
        val ropeStart = ribbonX - ropeLength / 2f
        val ropeEnd = ribbonX + ropeLength / 2f

        drawScope.drawLine(
            color = Color(0xFF92400E), // Rope shadow
            start = Offset(ropeStart, centerY + 2f + wobble),
            end = Offset(ropeEnd, centerY + 2f + wobble),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = Color(0xFFD97706), // Hemp Rope Body
            start = Offset(ropeStart, centerY + wobble),
            end = Offset(ropeEnd, centerY + wobble),
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )

        // Center Flag / Red Ribbon
        drawScope.drawCircle(
            color = Color.White,
            center = Offset(ribbonX, centerY + wobble),
            radius = 16f
        )
        drawScope.drawCircle(
            color = Color(0xFFE11D48),
            center = Offset(ribbonX, centerY + wobble),
            radius = 12f
        )
        // Red Ribbon Streamers Hanging
        val streamer = Path().apply {
            moveTo(ribbonX, centerY + wobble + 12f)
            lineTo(ribbonX - 10f, centerY + wobble + 38f)
            lineTo(ribbonX, centerY + wobble + 32f)
            lineTo(ribbonX + 10f, centerY + wobble + 38f)
            close()
        }
        drawScope.drawPath(streamer, color = Color(0xFFE11D48))

        // Particle sparks & confetti
        particleSystem.draw(drawScope)
    }

    private fun drawPuller(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        color: Color,
        isFacingLeft: Boolean,
        isPulling: Boolean
    ) {
        val headRadius = 18f
        val bodyColor = color

        // Leaning angle: pulling backwards
        val leanOffset = if (isFacingLeft) -12f else 12f

        // Shadow under character
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(x - 16f, y + 26f),
            size = Size(32f, 10f)
        )

        // Body
        drawScope.drawCircle(
            color = bodyColor,
            center = Offset(x + leanOffset * 0.4f, y + 6f),
            radius = headRadius * 0.9f
        )
        drawScope.drawCircle(
            color = Color.Black,
            center = Offset(x + leanOffset * 0.4f, y + 6f),
            radius = headRadius * 0.9f,
            style = Stroke(width = 3f)
        )

        // Head
        drawScope.drawCircle(
            color = Color(0xFFFFDDBB),
            center = Offset(x + leanOffset, y - 14f),
            radius = headRadius
        )
        drawScope.drawCircle(
            color = Color.Black,
            center = Offset(x + leanOffset, y - 14f),
            radius = headRadius,
            style = Stroke(width = 3f)
        )

        // Team Colored Cap / Headband
        drawScope.drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(x + leanOffset - headRadius, y - 14f - headRadius),
            size = Size(headRadius * 2, headRadius * 2)
        )

        // Straining Eyes (> < or squint)
        val eyeX = x + leanOffset + (if (isFacingLeft) -5f else 5f)
        drawScope.drawCircle(color = Color.Black, radius = 2.5f, center = Offset(eyeX - 4f, y - 15f))
        drawScope.drawCircle(color = Color.Black, radius = 2.5f, center = Offset(eyeX + 4f, y - 15f))

        // Sweat bead
        drawScope.drawCircle(color = Color(0xFF38BDF8), radius = 3f, center = Offset(x + leanOffset + (if (isFacingLeft) 10f else -10f), y - 20f))
    }
}
