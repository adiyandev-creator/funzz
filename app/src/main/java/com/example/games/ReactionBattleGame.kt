package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import kotlin.math.sqrt
import kotlin.random.Random

class ReactionBattleGame : MiniGame {
    override val id: String = "reaction_battle"
    override val title: String = "Reaction Battle"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null
    private val particleSystem = com.example.engine.ParticleSystem()

    enum class RoundState {
        WAITING,
        SIGNAL_ACTIVE,
        ROUND_OVER
    }

    var roundState: RoundState = RoundState.WAITING
    private var delayTimer: Float = 0f
    private var targetDelay: Float = 2.5f
    private var signalStartTime: Long = 0L

    val scores = mutableMapOf<Int, Int>()
    val reactionTimes = mutableMapOf<Int, Long>()
    val falseStarts = mutableSetOf<Int>()
    private var roundWinnerId: Int? = null

    private var roundIntermissionTimer: Float = 0f
    private var gameOver: Boolean = false
    private val winningScore: Int = 3

    // AI simulation
    private val aiTargetReaction = mutableMapOf<Int, Long>()

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        scores.clear()
        players.forEach { scores[it.id] = 0 }
        reset()
    }

    override fun reset() {
        gameOver = false
        startNewRound()
    }

    private fun startNewRound() {
        roundState = RoundState.WAITING
        delayTimer = 0f
        targetDelay = Random.nextFloat() * 2.5f + 1.8f // 1.8 to 4.3 seconds
        signalStartTime = 0L
        reactionTimes.clear()
        falseStarts.clear()
        roundWinnerId = null
        roundIntermissionTimer = 0f

        // Precalculate AI reaction speeds (240ms to 420ms)
        aiTargetReaction.clear()
        players.filter { it.isAi }.forEach {
            aiTargetReaction[it.id] = Random.nextLong(240, 420)
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        particleSystem.update(deltaSeconds, gravity = 0f)

        when (roundState) {
            RoundState.WAITING -> {
                delayTimer += deltaSeconds
                if (delayTimer >= targetDelay) {
                    roundState = RoundState.SIGNAL_ACTIVE
                    signalStartTime = System.currentTimeMillis()
                    particleSystem.emitShockwave(bounds.width / 2f, bounds.height / 2f, Color(0xFF10B981), maxRadius = 160f)
                    soundAndHaptics?.playScore()
                }
            }
            RoundState.SIGNAL_ACTIVE -> {
                val elapsedMs = System.currentTimeMillis() - signalStartTime

                // AI players react
                players.filter { it.isAi }.forEach { ai ->
                    val targetMs = aiTargetReaction[ai.id] ?: 300L
                    if (elapsedMs >= targetMs && !reactionTimes.containsKey(ai.id) && !falseStarts.contains(ai.id)) {
                        onPlayerTap(ai.id)
                    }
                }
            }
            RoundState.ROUND_OVER -> {
                roundIntermissionTimer += deltaSeconds
                if (roundIntermissionTimer >= 1.8f) {
                    if (scores.values.any { it >= winningScore }) {
                        gameOver = true
                        soundAndHaptics?.playVictory()
                    } else {
                        startNewRound()
                    }
                }
            }
        }
    }

    fun onPlayerTap(playerId: Int) {
        if (gameOver) return

        when (roundState) {
            RoundState.WAITING -> {
                // False start!
                if (!falseStarts.contains(playerId)) {
                    falseStarts.add(playerId)
                    val pColor = players.find { it.id == playerId }?.color ?: Color.Red
                    particleSystem.emitSparks(bounds.width / 2f, bounds.height / 2f, pColor, count = 10)
                    soundAndHaptics?.playHit()
                    // If all remaining players false started, reset round
                    if (falseStarts.size >= players.size) {
                        startNewRound()
                    }
                }
            }
            RoundState.SIGNAL_ACTIVE -> {
                if (falseStarts.contains(playerId) || reactionTimes.containsKey(playerId)) return

                val time = System.currentTimeMillis() - signalStartTime
                reactionTimes[playerId] = time
                soundAndHaptics?.playTap()

                if (roundWinnerId == null) {
                    roundWinnerId = playerId
                    scores[playerId] = (scores[playerId] ?: 0) + 1
                    roundState = RoundState.ROUND_OVER
                    val pColor = players.find { it.id == playerId }?.color ?: Color(0xFF10B981)
                    particleSystem.emitConfetti(bounds.width / 2f, bounds.height / 2f, listOf(pColor, Color.White, Color.Yellow), count = 28)
                    particleSystem.emitShockwave(bounds.width / 2f, bounds.height / 2f, pColor, maxRadius = 140f)
                }
            }
            RoundState.ROUND_OVER -> {}
        }
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        if (sqrt(x * x + y * y) > 0.35f) {
            onPlayerTap(playerId)
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        onPlayerTap(playerId)
    }

    override fun onButtonTap(playerId: Int) {
        onPlayerTap(playerId)
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}
    override fun onPointerUp(playerId: Int) {}

    override fun getPlayerScore(playerId: Int): Int = scores[playerId] ?: 0

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val rankedPlayers = players.sortedByDescending { scores[it.id] ?: 0 }
        val playerResults = rankedPlayers.mapIndexed { index, player ->
            PlayerResult(
                player = player,
                score = scores[player.id] ?: 0,
                rank = index + 1
            )
        }

        return GameResult(
            gameId = id,
            rankings = playerResults,
            durationSeconds = 25,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        val center = Offset(bounds.width / 2f, bounds.height / 2f)
        val minDim = minOf(bounds.width, bounds.height)

        // Center Indicator Orb
        val orbRadius = minDim * 0.22f
        val orbColor = when (roundState) {
            RoundState.WAITING -> Color(0xFFEF4444) // Red: Wait
            RoundState.SIGNAL_ACTIVE -> Color(0xFF10B981) // Green: TAP NOW!
            RoundState.ROUND_OVER -> Color(0xFF3B82F6) // Blue: Finished round
        }

        // Draw ambient glow
        drawScope.drawCircle(
            color = orbColor.copy(alpha = 0.25f),
            center = center,
            radius = orbRadius * 1.35f
        )
        // Draw main orb
        drawScope.drawCircle(
            color = orbColor,
            center = center,
            radius = orbRadius
        )

        // Inner highlight
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            center = Offset(center.x - orbRadius * 0.25f, center.y - orbRadius * 0.25f),
            radius = orbRadius * 0.35f
        )

        particleSystem.draw(drawScope)
    }
}
