package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import kotlin.random.Random

class TugOfWarGame : MiniGame {
    override val id: String = "tug_of_war"
    override val title: String = "Tug of War"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    // Rope position: -1.0 (Team Left wins) to +1.0 (Team Right wins)
    var ropePosition: Float = 0f
    private var ropeVelocity: Float = 0f

    // Team 1: Even indices (P1, P3), Team 2: Odd indices (P2, P4)
    val tapCounts = mutableMapOf<Int, Int>()
    private var gameOver: Boolean = false
    private var winningTeam: Int? = null

    // AI tap timers
    private val aiTapTimers = mutableMapOf<Int, Float>()

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
        players.forEach {
            tapCounts[it.id] = 0
            if (it.isAi) aiTapTimers[it.id] = 0f
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        // AI tapping
        players.filter { it.isAi }.forEach { ai ->
            var timer = aiTapTimers[ai.id] ?: 0f
            timer += deltaSeconds
            // AI taps every 0.12s - 0.18s
            val tapInterval = 0.14f + (Random.nextFloat() * 0.05f)
            if (timer >= tapInterval) {
                timer = 0f
                onPlayerPull(ai.id)
            }
            aiTapTimers[ai.id] = timer
        }

        // Apply friction and integrate rope position
        ropeVelocity *= 0.94f
        ropePosition += ropeVelocity * deltaSeconds

        // Win threshold
        if (ropePosition <= -1.0f) {
            ropePosition = -1.0f
            gameOver = true
            winningTeam = 1 // Team Left
            soundAndHaptics?.playVictory()
        } else if (ropePosition >= 1.0f) {
            ropePosition = 1.0f
            gameOver = true
            winningTeam = 2 // Team Right
            soundAndHaptics?.playVictory()
        }
    }

    fun onPlayerPull(playerId: Int) {
        if (gameOver) return

        tapCounts[playerId] = (tapCounts[playerId] ?: 0) + 1
        soundAndHaptics?.playTap()

        val isTeam1 = (playerId % 2 == 0) // Even = Team 1 (P1, P3 pulls Left)
        val pullStrength = 0.08f

        if (isTeam1) {
            ropeVelocity -= pullStrength
        } else {
            ropeVelocity += pullStrength
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

        // Background Mud / Field Track
        drawScope.drawRect(
            color = Color(0xFF1E2430),
            topLeft = Offset(leftBoundary - 20f, centerY - 60f),
            size = Size(arenaWidth + 40f, 120f)
        )

        // Center Mark Line
        drawScope.drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(centerX, centerY - 70f),
            end = Offset(centerX, centerY + 70f),
            strokeWidth = 4f
        )

        // Left Goal Line (Team 1 Win)
        val team1Color = players.firstOrNull { it.id % 2 == 0 }?.color ?: Color(0xFF3B82F6)
        drawScope.drawLine(
            color = team1Color,
            start = Offset(leftBoundary, centerY - 65f),
            end = Offset(leftBoundary, centerY + 65f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Right Goal Line (Team 2 Win)
        val team2Color = players.firstOrNull { it.id % 2 != 0 }?.color ?: Color(0xFFEF4444)
        drawScope.drawLine(
            color = team2Color,
            start = Offset(rightBoundary, centerY - 65f),
            end = Offset(rightBoundary, centerY + 65f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Rope
        val ribbonX = centerX + (ropePosition * halfW)
        val ropeLength = arenaWidth * 1.15f
        val ropeStart = ribbonX - ropeLength / 2f
        val ropeEnd = ribbonX + ropeLength / 2f

        drawScope.drawLine(
            color = Color(0xFFD97706), // Hemp Rope
            start = Offset(ropeStart, centerY),
            end = Offset(ropeEnd, centerY),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )

        // Center Ribbon / Flag on Rope
        drawScope.drawCircle(
            color = Color.White,
            center = Offset(ribbonX, centerY),
            radius = 16f
        )
        drawScope.drawCircle(
            color = Color(0xFFE11D48),
            center = Offset(ribbonX, centerY),
            radius = 11f
        )
    }
}
