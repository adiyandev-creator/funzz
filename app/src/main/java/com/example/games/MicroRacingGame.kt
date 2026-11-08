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

class MicroRacingGame : MiniGame {
    override val id: String = "micro_racing"
    override val title: String = "Micro Racing"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    class Car(
        val player: Player,
        var x: Float,
        var y: Float,
        var angle: Float = 0f, // in radians
        var speed: Float = 0f,
        val maxSpeed: Float = 320f,
        var isSteeringRight: Boolean = false,
        var isSteeringLeft: Boolean = false,
        var currentCheckpoint: Int = 0,
        var lapsCompleted: Int = 0,
        var isFinished: Boolean = false
    )

    private val cars = mutableListOf<Car>()
    private var checkpoints = listOf<Offset>()
    private val totalLaps = 3
    private var gameOver: Boolean = false
    private val finishOrder = mutableListOf<Player>()

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        setupTrackCheckpoints()
        reset()
    }

    private fun setupTrackCheckpoints() {
        val cx = bounds.width / 2f
        val cy = bounds.height / 2f
        val rx = bounds.width * 0.38f
        val ry = bounds.height * 0.36f

        // Elliptical racetrack waypoints clockwise
        checkpoints = listOf(
            Offset(cx, cy - ry),                 // Top (Start/Finish)
            Offset(cx + rx * 0.7f, cy - ry * 0.7f),
            Offset(cx + rx, cy),                 // Right
            Offset(cx + rx * 0.7f, cy + ry * 0.7f),
            Offset(cx, cy + ry),                 // Bottom
            Offset(cx - rx * 0.7f, cy + ry * 0.7f),
            Offset(cx - rx, cy),                 // Left
            Offset(cx - rx * 0.7f, cy - ry * 0.7f)
        )
    }

    override fun reset() {
        cars.clear()
        finishOrder.clear()
        gameOver = false

        val startLine = checkpoints.firstOrNull() ?: Offset(bounds.width / 2f, bounds.height * 0.15f)

        players.forEachIndexed { index, player ->
            val carYOffset = index * 18f - (players.size * 9f)
            cars.add(
                Car(
                    player = player,
                    x = startLine.x + carYOffset,
                    y = startLine.y,
                    angle = 0f, // facing right
                    speed = 0f
                )
            )
        }
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        val cx = bounds.width / 2f
        val cy = bounds.height / 2f
        val outerRx = bounds.width * 0.44f
        val outerRy = bounds.height * 0.42f
        val innerRx = bounds.width * 0.22f
        val innerRy = bounds.height * 0.20f

        cars.forEach { car ->
            if (car.isFinished) return@forEach

            // AI control
            if (car.player.isAi && checkpoints.isNotEmpty()) {
                val targetCp = checkpoints[car.currentCheckpoint]
                val dx = targetCp.x - car.x
                val dy = targetCp.y - car.y
                val targetAngle = atan2(dy, dx)

                // Steer towards target waypoint
                var diff = targetAngle - car.angle
                while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
                while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()

                car.isSteeringRight = diff > 0.1f
                car.isSteeringLeft = diff < -0.1f
            }

            // Steering input
            val turnSpeed = 3.6f * deltaSeconds
            if (car.isSteeringRight) car.angle += turnSpeed
            if (car.isSteeringLeft) car.angle -= turnSpeed

            // Acceleration & speed
            val targetSpeed = car.maxSpeed
            car.speed = (car.speed + 380f * deltaSeconds).coerceAtMost(targetSpeed)

            // Integrate position
            car.x += cos(car.angle) * car.speed * deltaSeconds
            car.y += sin(car.angle) * car.speed * deltaSeconds

            // Track boundary constraint (elliptical track walls)
            val nx = (car.x - cx)
            val ny = (car.y - cy)
            val outerDist = (nx * nx) / (outerRx * outerRx) + (ny * ny) / (outerRy * outerRy)
            val innerDist = (nx * nx) / (innerRx * innerRx) + (ny * ny) / (innerRy * innerRy)

            if (outerDist > 1.0f) {
                // Hit outer wall: bounce inwards
                car.speed *= 0.55f
                val bounceAngle = atan2(-ny, -nx)
                car.angle = bounceAngle
                car.x = cx + (nx / sqrt(outerDist)) * 0.98f
                car.y = cy + (ny / sqrt(outerDist)) * 0.98f
                soundAndHaptics?.playHit()
            } else if (innerDist < 1.0f) {
                // Hit inner barrier: bounce outwards
                car.speed *= 0.55f
                val bounceAngle = atan2(ny, nx)
                car.angle = bounceAngle
                car.x = cx + (nx / sqrt(innerDist)) * 1.02f
                car.y = cy + (ny / sqrt(innerDist)) * 1.02f
                soundAndHaptics?.playHit()
            }

            // Checkpoint progression
            if (checkpoints.isNotEmpty()) {
                val nextCp = checkpoints[car.currentCheckpoint]
                val dX = nextCp.x - car.x
                val dY = nextCp.y - car.y
                if (sqrt(dX * dX + dY * dY) < 90f) {
                    car.currentCheckpoint = (car.currentCheckpoint + 1) % checkpoints.size
                    if (car.currentCheckpoint == 0) {
                        car.lapsCompleted++
                        soundAndHaptics?.playScore()
                        if (car.lapsCompleted >= totalLaps) {
                            car.isFinished = true
                            finishOrder.add(car.player)
                            if (finishOrder.size >= players.size || finishOrder.size == 1) {
                                gameOver = true
                                soundAndHaptics?.playVictory()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        val car = cars.find { it.player.id == playerId } ?: return
        // Right side of screen or player pad steers right, left steers left
        if (position.x > bounds.width / 2f) {
            car.isSteeringRight = true
            car.isSteeringLeft = false
        } else {
            car.isSteeringLeft = true
            car.isSteeringRight = false
        }
    }

    override fun onPointerUp(playerId: Int) {
        val car = cars.find { it.player.id == playerId } ?: return
        car.isSteeringRight = false
        car.isSteeringLeft = false
    }

    override fun onButtonTap(playerId: Int) {
        // Toggle turn on tap
        val car = cars.find { it.player.id == playerId } ?: return
        car.angle += 0.45f
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val rankings = mutableListOf<PlayerResult>()
        // First finishers
        finishOrder.forEachIndexed { index, player ->
            rankings.add(PlayerResult(player, (totalLaps - index) * 10, index + 1))
        }
        // Unfinished cars ranked by laps and checkpoints
        val unfinished = cars.filter { !it.isFinished }.sortedWith(
            compareByDescending<Car> { it.lapsCompleted }.thenByDescending { it.currentCheckpoint }
        )
        unfinished.forEachIndexed { index, car ->
            rankings.add(PlayerResult(car.player, car.lapsCompleted * 5, finishOrder.size + index + 1))
        }

        return GameResult(
            gameId = id,
            rankings = rankings,
            durationSeconds = 40,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        val cx = bounds.width / 2f
        val cy = bounds.height / 2f
        val outerRx = bounds.width * 0.44f
        val outerRy = bounds.height * 0.42f
        val innerRx = bounds.width * 0.22f
        val innerRy = bounds.height * 0.20f

        // Draw Asphalt Track surface
        drawScope.drawOval(
            color = Color(0xFF1E2430),
            topLeft = Offset(cx - outerRx, cy - outerRy),
            size = Size(outerRx * 2, outerRy * 2)
        )
        // Inner grass infield
        drawScope.drawOval(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - innerRx, cy - innerRy),
            size = Size(innerRx * 2, innerRy * 2)
        )

        // Outer Curb
        drawScope.drawOval(
            color = Color(0xFFEF4444),
            topLeft = Offset(cx - outerRx, cy - outerRy),
            size = Size(outerRx * 2, outerRy * 2),
            style = Stroke(width = 6f)
        )
        // Inner Curb
        drawScope.drawOval(
            color = Color.White,
            topLeft = Offset(cx - innerRx, cy - innerRy),
            size = Size(innerRx * 2, innerRy * 2),
            style = Stroke(width = 6f)
        )

        // Start / Finish Line at top
        val sfY = cy - (outerRy + innerRy) / 2f
        drawScope.drawLine(
            color = Color.White,
            start = Offset(cx - 30f, sfY),
            end = Offset(cx + 30f, sfY),
            strokeWidth = 10f
        )

        // Draw Cars
        cars.forEach { car ->
            val carCenter = Offset(car.x, car.y)
            val carLength = 24f
            val carWidth = 14f
            val cosA = cos(car.angle)
            val sinA = sin(car.angle)

            // Car chassis
            val forward = Offset(cosA * carLength / 2f, sinA * carLength / 2f)
            val side = Offset(-sinA * carWidth / 2f, cosA * carWidth / 2f)

            val p1 = carCenter + forward + side
            val p2 = carCenter + forward - side
            val p3 = carCenter - forward - side
            val p4 = carCenter - forward + side

            val path = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }

            drawScope.drawPath(path = path, color = car.player.color)

            // Headlights
            drawScope.drawCircle(
                color = Color.White,
                center = carCenter + forward * 0.8f,
                radius = 3f
            )
        }
    }
}
