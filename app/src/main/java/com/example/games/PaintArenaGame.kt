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
    private val particleSystem = ParticleSystem()

    class Snail(
        val player: Player,
        var x: Float,
        var y: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var headingAngle: Float = 0f,
        val speed: Float = 240f,
        val radius: Float = 24f,
        var eyeWobble: Float = 0f
    )

    private val snails = mutableListOf<Snail>()

    // Trail history points for rendering organic paint ribbons (like image_69fcaf87.png)
    class TrailPoint(val x: Float, val y: Float, val color: Color)
    private val paintTrails = mutableListOf<TrailPoint>()

    // Grid for territory ownership calculation
    private val cols = 24
    private val rows = 36
    val grid = IntArray(cols * rows) { -1 }

    var matchTimer: Float = 30f
    private var gameOver: Boolean = false
    val scores = IntArray(4) { 0 }

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        reset()
    }

    override fun reset() {
        matchTimer = 30f
        gameOver = false
        particleSystem.clear()
        paintTrails.clear()
        for (i in grid.indices) grid[i] = -1
        for (i in scores.indices) scores[i] = 0

        snails.clear()
        val spawnCoords = listOf(
            Offset(bounds.width * 0.25f, bounds.height * 0.28f),
            Offset(bounds.width * 0.75f, bounds.height * 0.72f),
            Offset(bounds.width * 0.75f, bounds.height * 0.28f),
            Offset(bounds.width * 0.25f, bounds.height * 0.72f)
        )

        players.forEachIndexed { index, player ->
            val spawn = spawnCoords.getOrElse(index) { Offset(bounds.width / 2f, bounds.height / 2f) }
            val angle = (index * 90f) * (Math.PI / 180f).toFloat()
            snails.add(
                Snail(
                    player = player,
                    x = spawn.x,
                    y = spawn.y,
                    headingAngle = angle
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
            calculateScores()
            return
        }

        particleSystem.update(deltaSeconds, gravity = 0f)
        val cellW = bounds.width / cols
        val cellH = bounds.height / rows

        snails.forEach { snail ->
            snail.eyeWobble += 8f * deltaSeconds

            // AI movement
            if (snail.player.isAi) {
                // Seek least painted or rival regions
                if (Random.nextFloat() < 0.05f || snail.vx == 0f) {
                    val targetX = Random.nextFloat() * (bounds.width - 80f) + 40f
                    val targetY = Random.nextFloat() * (bounds.height - 80f) + 40f
                    val angle = atan2(targetY - snail.y, targetX - snail.x)
                    snail.vx = cos(angle)
                    snail.vy = sin(angle)
                }
            }

            // Move snail if velocity vector is active
            val inputMag = sqrt(snail.vx * snail.vx + snail.vy * snail.vy)
            if (inputMag > 0.05f) {
                val targetAngle = atan2(snail.vy, snail.vx)
                snail.headingAngle = targetAngle

                val moveX = cos(targetAngle) * snail.speed * deltaSeconds
                val moveY = sin(targetAngle) * snail.speed * deltaSeconds

                snail.x = (snail.x + moveX).coerceIn(snail.radius + 10f, bounds.width - snail.radius - 10f)
                snail.y = (snail.y + moveY).coerceIn(snail.radius + 10f, bounds.height - snail.radius - 10f)

                // Add to paint ribbon trail
                paintTrails.add(TrailPoint(snail.x, snail.y, snail.player.color))
                if (paintTrails.size > 800) {
                    paintTrails.removeAt(0)
                }

                // Paint underlying grid
                paintCells(snail.x, snail.y, snail.player.id, cellW, cellH)

                // Emit paint bubbles occasionally
                if (Random.nextFloat() < 0.25f) {
                    particleSystem.emitSparks(
                        x = snail.x - cos(targetAngle) * snail.radius,
                        y = snail.y - sin(targetAngle) * snail.radius,
                        color = snail.player.color.copy(alpha = 0.6f),
                        count = 2,
                        speed = 40f
                    )
                }
            }
        }

        calculateScores()
    }

    private fun paintCells(x: Float, y: Float, playerId: Int, cellW: Float, cellH: Float) {
        val centerCol = (x / cellW).toInt()
        val centerRow = (y / cellH).toInt()

        for (r in (centerRow - 2)..(centerRow + 2)) {
            for (c in (centerCol - 2)..(centerCol + 2)) {
                if (c in 0 until cols && r in 0 until rows) {
                    val idx = r * cols + c
                    if (grid[idx] != playerId) {
                        grid[idx] = playerId
                    }
                }
            }
        }
    }

    private fun calculateScores() {
        val counts = IntArray(4) { 0 }
        for (v in grid) {
            if (v in 0..3) counts[v]++
        }
        val total = grid.size.toFloat()
        for (i in 0..3) {
            scores[i] = ((counts[i] / total) * 100f).toInt()
            players.find { it.id == i }?.score = scores[i]
        }
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        val snail = snails.find { it.player.id == playerId } ?: return
        snail.vx = x
        snail.vy = y
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        val snail = snails.find { it.player.id == playerId } ?: return
        val dx = position.x - snail.x
        val dy = position.y - snail.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 5f) {
            snail.vx = dx / dist
            snail.vy = dy / dist
        }
    }

    override fun onPointerMove(playerId: Int, position: Offset) {
        onPointerDown(playerId, position)
    }

    override fun onPointerUp(playerId: Int) {
        val snail = snails.find { it.player.id == playerId } ?: return
        snail.vx = 0f
        snail.vy = 0f
    }

    override fun onButtonTap(playerId: Int) {}

    override fun getPlayerScore(playerId: Int): Int = scores.getOrElse(playerId) { 0 }
    override fun getRemainingSeconds(): Int? = matchTimer.toInt()

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val ranked = players.sortedByDescending { scores.getOrElse(it.id) { 0 } }
        val playerResults = ranked.mapIndexed { index, player ->
            PlayerResult(
                player = player,
                score = scores.getOrElse(player.id) { 0 },
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
        val w = bounds.width
        val h = bounds.height

        // Clean Wood Floor Texture Background (like image_69fcaf87.png)
        drawScope.drawRect(Color(0xFFFBF4E8))
        // Subtle vertical wood plank lines
        val plankW = w / 6f
        for (i in 1..5) {
            drawScope.drawLine(
                color = Color(0xFFEADBCE),
                start = Offset(i * plankW, 0f),
                end = Offset(i * plankW, h),
                strokeWidth = 2f
            )
        }

        // Draw Thick Colorful Paint Trails (Reference image_69fcaf87.png style)
        paintTrails.forEach { pt ->
            drawScope.drawCircle(
                color = pt.color.copy(alpha = 0.85f),
                center = Offset(pt.x, pt.y),
                radius = 28f
            )
        }

        // Particles
        particleSystem.draw(drawScope)

        // Draw Animated Snails
        snails.forEach { snail ->
            val center = Offset(snail.x, snail.y)
            val rad = snail.radius
            val pColor = snail.player.color
            val cosA = cos(snail.headingAngle)
            val sinA = sin(snail.headingAngle)

            // Snail Body (Beige/Cream)
            val bodyCenter = center + Offset(cosA * 8f, sinA * 8f)
            drawScope.drawCircle(
                color = Color(0xFFFFF3D6),
                center = bodyCenter,
                radius = rad * 0.9f
            )
            drawScope.drawCircle(
                color = Color.Black,
                center = bodyCenter,
                radius = rad * 0.9f,
                style = Stroke(width = 3.5f)
            )

            // Snail Swirl Shell (Colored by Player)
            drawScope.drawCircle(
                color = pColor,
                center = center,
                radius = rad
            )
            drawScope.drawCircle(
                color = Color.Black,
                center = center,
                radius = rad,
                style = Stroke(width = 4f)
            )

            // Shell Spiral Swirl Line
            val spiralPath = Path().apply {
                val spiralTurns = 2
                for (step in 0..30) {
                    val theta = step * (Math.PI * 2 * spiralTurns / 30f).toFloat()
                    val r = (step / 30f) * rad * 0.75f
                    val px = center.x + cos(theta) * r
                    val py = center.y + sin(theta) * r
                    if (step == 0) moveTo(px, py) else lineTo(px, py)
                }
            }
            drawScope.drawPath(spiralPath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Two Googly Eyes on Antennae facing movement direction
            val eyeDist = rad * 1.15f
            val eyeSpread = 12f
            val perpX = -sinA * eyeSpread
            val perpY = cosA * eyeSpread

            val eye1Pos = center + Offset(cosA * eyeDist + perpX, sinA * eyeDist + perpY)
            val eye2Pos = center + Offset(cosA * eyeDist - perpX, sinA * eyeDist - perpY)

            // Stalks
            drawScope.drawLine(Color.Black, bodyCenter, eye1Pos, strokeWidth = 3f)
            drawScope.drawLine(Color.Black, bodyCenter, eye2Pos, strokeWidth = 3f)

            // Eye Whites
            drawScope.drawCircle(Color.White, center = eye1Pos, radius = 7f)
            drawScope.drawCircle(Color.Black, center = eye1Pos, radius = 7f, style = Stroke(width = 2.5f))
            drawScope.drawCircle(Color.White, center = eye2Pos, radius = 7f)
            drawScope.drawCircle(Color.Black, center = eye2Pos, radius = 7f, style = Stroke(width = 2.5f))

            // Pupils looking ahead
            val pupilOffset = Offset(cosA * 2.5f, sinA * 2.5f)
            drawScope.drawCircle(Color.Black, center = eye1Pos + pupilOffset, radius = 3.5f)
            drawScope.drawCircle(Color.Black, center = eye2Pos + pupilOffset, radius = 3.5f)
        }
    }
}
