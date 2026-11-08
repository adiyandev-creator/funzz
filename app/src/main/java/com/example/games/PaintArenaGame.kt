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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PaintArenaGame : MiniGame {
    override val id: String = "paint_arena"
    override val title: String = "Paint Arena"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    class Roller(
        val player: Player,
        var x: Float,
        var y: Float,
        var targetX: Float,
        var targetY: Float,
        var speed: Float = 260f,
        val radius: Float = 22f
    )

    private val rollers = mutableListOf<Roller>()

    // Grid representing painted cells: -1 = unpainted, 0..3 = player index
    private val cols = 22
    private val rows = 32
    val grid = IntArray(cols * rows) { -1 }

    var matchTimer: Float = 35f
    private var gameOver: Boolean = false
    val coveragePercentages = FloatArray(4) { 0f }

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        reset()
    }

    override fun reset() {
        matchTimer = 35f
        gameOver = false
        for (i in grid.indices) grid[i] = -1
        for (i in coveragePercentages.indices) coveragePercentages[i] = 0f

        rollers.clear()
        val spawnCoords = listOf(
            Offset(bounds.width * 0.25f, bounds.height * 0.25f),
            Offset(bounds.width * 0.75f, bounds.height * 0.75f),
            Offset(bounds.width * 0.75f, bounds.height * 0.25f),
            Offset(bounds.width * 0.25f, bounds.height * 0.75f)
        )

        players.forEachIndexed { index, player ->
            val spawn = spawnCoords.getOrElse(index) { Offset(bounds.width / 2f, bounds.height / 2f) }
            rollers.add(
                Roller(
                    player = player,
                    x = spawn.x,
                    y = spawn.y,
                    targetX = spawn.x,
                    targetY = spawn.y
                )
            )
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        matchTimer -= deltaSeconds
        if (matchTimer <= 0f) {
            matchTimer = 0f
            gameOver = true
            soundAndHaptics?.playVictory()
            calculateCoverage()
            return
        }

        val cellWidth = bounds.width / cols
        val cellHeight = bounds.height / rows

        rollers.forEach { roller ->
            // AI logic
            if (roller.player.isAi) {
                val dx = roller.targetX - roller.x
                val dy = roller.targetY - roller.y
                if (sqrt(dx * dx + dy * dy) < 30f || Random.nextFloat() < 0.03f) {
                    roller.targetX = Random.nextFloat() * (bounds.width - 60f) + 30f
                    roller.targetY = Random.nextFloat() * (bounds.height - 60f) + 30f
                }
            }

            // Move roller towards target
            val dx = roller.targetX - roller.x
            val dy = roller.targetY - roller.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 5f) {
                val angle = atan2(dy, dx)
                val moveDist = minOf(dist, roller.speed * deltaSeconds)
                roller.x += cos(angle) * moveDist
                roller.y += sin(angle) * moveDist

                // Clamp to screen bounds
                roller.x = roller.x.coerceIn(roller.radius, bounds.width - roller.radius)
                roller.y = roller.y.coerceIn(roller.radius, bounds.height - roller.radius)

                // Paint underlying grid tiles in footprint
                paintCellsAt(roller.x, roller.y, roller.player.id, cellWidth, cellHeight)
            }
        }

        calculateCoverage()
    }

    private fun paintCellsAt(x: Float, y: Float, playerId: Int, cellW: Float, cellH: Float) {
        val centerCol = (x / cellW).toInt()
        val centerRow = (y / cellH).toInt()

        for (r in (centerRow - 1)..(centerRow + 1)) {
            for (c in (centerCol - 1)..(centerCol + 1)) {
                if (c in 0 until cols && r in 0 until rows) {
                    val idx = r * cols + c
                    if (grid[idx] != playerId) {
                        grid[idx] = playerId
                    }
                }
            }
        }
    }

    private fun calculateCoverage() {
        val totalCells = grid.size.toFloat()
        val counts = IntArray(4) { 0 }
        for (v in grid) {
            if (v in 0..3) counts[v]++
        }
        for (i in 0..3) {
            coveragePercentages[i] = (counts[i] / totalCells) * 100f
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        val roller = rollers.find { it.player.id == playerId } ?: return
        roller.targetX = position.x
        roller.targetY = position.y
    }

    override fun onPointerMove(playerId: Int, position: Offset) {
        val roller = rollers.find { it.player.id == playerId } ?: return
        roller.targetX = position.x
        roller.targetY = position.y
    }

    override fun onPointerUp(playerId: Int) {}
    override fun onButtonTap(playerId: Int) {}

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val ranked = players.sortedByDescending { coveragePercentages.getOrElse(it.id) { 0f } }
        val playerResults = ranked.mapIndexed { index, player ->
            val pct = coveragePercentages.getOrElse(player.id) { 0f }.toInt()
            PlayerResult(
                player = player,
                score = pct,
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
        val cellW = bounds.width / cols
        val cellH = bounds.height / rows

        // Draw Arena Grid
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val owner = grid[r * cols + c]
                val cellColor = if (owner >= 0) {
                    val playerColor = players.find { it.id == owner }?.color ?: Color.Gray
                    playerColor.copy(alpha = 0.55f)
                } else {
                    Color(0xFF22262E)
                }

                drawScope.drawRect(
                    color = cellColor,
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }

        // Draw Rollers
        rollers.forEach { roller ->
            val pColor = roller.player.color
            // Outer ring
            drawScope.drawCircle(
                color = Color.White,
                center = Offset(roller.x, roller.y),
                radius = roller.radius + 3f
            )
            // Inner color
            drawScope.drawCircle(
                color = pColor,
                center = Offset(roller.x, roller.y),
                radius = roller.radius
            )
            // Center roller core
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                center = Offset(roller.x, roller.y),
                radius = roller.radius * 0.4f
            )
        }
    }
}
