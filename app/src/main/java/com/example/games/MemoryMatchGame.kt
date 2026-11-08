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
import com.example.engine.ParticleSystem
import com.example.engine.SoundAndHaptics
import kotlin.random.Random

class MemoryMatchGame : MiniGame {
    override val id: String = "memory_match"
    override val title: String = "Memory Match"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null
    private val particleSystem = ParticleSystem()

    class Card(
        val symbolId: Int, // 0..7
        var isFaceUp: Boolean = false,
        var isMatched: Boolean = false,
        var ownerPlayerId: Int? = null,
        var flipProgress: Float = 0f // 0f (face down) to 1f (face up)
    )

    val cards = mutableListOf<Card>()
    var currentPlayerIndex: Int = 0
    val playerScores = mutableMapOf<Int, Int>()

    private var firstFlippedIndex: Int? = null
    private var secondFlippedIndex: Int? = null
    private var flipWaitTimer: Float = 0f
    private var isWaitingMismatch: Boolean = false
    private var gameOver: Boolean = false

    // AI memory
    private val aiKnownCards = mutableMapOf<Int, Int>() // cardIndex -> symbolId
    private var aiTimer: Float = 0f

    // Joystick cursor
    private val playerCursors = mutableMapOf<Int, Int>()
    private val joystickCooldowns = mutableMapOf<Int, Float>()

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        playerScores.clear()
        players.forEach { playerScores[it.id] = 0 }
        reset()
    }

    override fun reset() {
        cards.clear()
        firstFlippedIndex = null
        secondFlippedIndex = null
        flipWaitTimer = 0f
        isWaitingMismatch = false
        gameOver = false
        currentPlayerIndex = 0
        aiKnownCards.clear()
        particleSystem.clear()
        playerCursors.clear()
        joystickCooldowns.clear()

        players.forEach {
            playerScores[it.id] = 0
            playerCursors[it.id] = 0
            joystickCooldowns[it.id] = 0f
        }

        // 8 pairs = 16 cards
        val symbols = (0..7).flatMap { listOf(it, it) }.shuffled()
        symbols.forEach { sym ->
            cards.add(Card(symbolId = sym))
        }
    }

    override fun update(deltaSeconds: Float) {
        particleSystem.update(deltaSeconds, gravity = 200f)

        // Smooth flip animation
        cards.forEach { card ->
            val target = if (card.isFaceUp || card.isMatched) 1f else 0f
            if (card.flipProgress < target) {
                card.flipProgress = (card.flipProgress + deltaSeconds * 7f).coerceAtMost(1f)
            } else if (card.flipProgress > target) {
                card.flipProgress = (card.flipProgress - deltaSeconds * 7f).coerceAtLeast(0f)
            }
        }

        // Joystick cooldowns
        players.forEach {
            val cd = joystickCooldowns[it.id] ?: 0f
            if (cd > 0f) {
                joystickCooldowns[it.id] = (cd - deltaSeconds).coerceAtLeast(0f)
            }
        }

        if (gameOver) return

        if (isWaitingMismatch) {
            flipWaitTimer += deltaSeconds
            if (flipWaitTimer >= 0.85f) {
                flipWaitTimer = 0f
                isWaitingMismatch = false
                firstFlippedIndex?.let { cards[it].isFaceUp = false }
                secondFlippedIndex?.let { cards[it].isFaceUp = false }
                firstFlippedIndex = null
                secondFlippedIndex = null
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size
            }
            return
        }

        // Check if all cards are matched
        if (cards.all { it.isMatched }) {
            gameOver = true
            soundAndHaptics?.playVictory()
            particleSystem.emitConfetti(bounds.width / 2f, bounds.height / 2f, count = 45)
            return
        }

        // AI turn logic
        val activePlayer = players.getOrNull(currentPlayerIndex)
        if (activePlayer != null && activePlayer.isAi) {
            aiTimer += deltaSeconds
            if (aiTimer >= 0.65f) {
                aiTimer = 0f
                performAiStep()
            }
        }
    }

    private fun performAiStep() {
        val unmatchedIndices = cards.indices.filter { !cards[it].isMatched && !cards[it].isFaceUp }
        if (unmatchedIndices.isEmpty()) return

        if (firstFlippedIndex == null) {
            val knownPairs = aiKnownCards.entries
                .filter { !cards[it.key].isMatched }
                .groupBy({ it.value }, { it.key })
                .filter { it.value.size >= 2 }

            if (knownPairs.isNotEmpty()) {
                val pair = knownPairs.values.first()
                flipCard(pair[0])
            } else {
                val pick = unmatchedIndices[Random.nextInt(unmatchedIndices.size)]
                flipCard(pick)
            }
        } else {
            val firstCard = cards[firstFlippedIndex!!]
            val matchingIndex = aiKnownCards.entries
                .find { it.value == firstCard.symbolId && it.key != firstFlippedIndex && !cards[it.key].isMatched }
                ?.key

            if (matchingIndex != null) {
                flipCard(matchingIndex)
            } else {
                val others = unmatchedIndices.filter { it != firstFlippedIndex }
                if (others.isNotEmpty()) {
                    val pick = others[Random.nextInt(others.size)]
                    flipCard(pick)
                }
            }
        }
    }

    fun flipCard(index: Int) {
        if (index !in cards.indices || cards[index].isMatched || cards[index].isFaceUp || isWaitingMismatch) return

        cards[index].isFaceUp = true
        aiKnownCards[index] = cards[index].symbolId
        soundAndHaptics?.playTap()

        val gridW = minOf(bounds.width * 0.88f, bounds.height * 0.65f)
        val startX = (bounds.width - gridW) / 2f
        val startY = (bounds.height - gridW) / 2f
        val cellW = gridW / 4f
        val col = index % 4
        val row = index / 4
        val cardCenterX = startX + col * cellW + cellW / 2f
        val cardCenterY = startY + row * cellW + cellW / 2f

        if (firstFlippedIndex == null) {
            firstFlippedIndex = index
        } else if (secondFlippedIndex == null) {
            secondFlippedIndex = index
            val first = cards[firstFlippedIndex!!]
            val second = cards[secondFlippedIndex!!]

            if (first.symbolId == second.symbolId) {
                first.isMatched = true
                second.isMatched = true
                val pId = players[currentPlayerIndex].id
                first.ownerPlayerId = pId
                second.ownerPlayerId = pId
                playerScores[pId] = (playerScores[pId] ?: 0) + 1

                firstFlippedIndex = null
                secondFlippedIndex = null
                soundAndHaptics?.playScore()

                val activeColor = players[currentPlayerIndex].color
                particleSystem.emitSparks(cardCenterX, cardCenterY, activeColor, count = 18, speed = 90f)
            } else {
                isWaitingMismatch = true
                soundAndHaptics?.playHit()
            }
        }
    }

    override fun onJoystick(playerId: Int, x: Float, y: Float) {
        if (gameOver || isWaitingMismatch) return
        val activePlayer = players.getOrNull(currentPlayerIndex) ?: return
        if (activePlayer.id != playerId || activePlayer.isAi) return

        val cd = joystickCooldowns[playerId] ?: 0f
        if (cd > 0f) return

        var cursor = playerCursors[playerId] ?: 0
        var row = cursor / 4
        var col = cursor % 4
        var moved = false

        if (x > 0.5f && col < 3) {
            col++
            moved = true
        } else if (x < -0.5f && col > 0) {
            col--
            moved = true
        }

        if (y > 0.5f && row < 3) {
            row++
            moved = true
        } else if (y < -0.5f && row > 0) {
            row--
            moved = true
        }

        if (moved) {
            playerCursors[playerId] = row * 4 + col
            joystickCooldowns[playerId] = 0.20f
            soundAndHaptics?.playTap()
        }
    }

    override fun onButtonTap(playerId: Int) {
        val activePlayer = players.getOrNull(currentPlayerIndex) ?: return
        if (activePlayer.id == playerId && !activePlayer.isAi) {
            val cursor = playerCursors[playerId] ?: 0
            flipCard(cursor)
        }
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        if (gameOver || isWaitingMismatch) return
        val activePlayer = players.getOrNull(currentPlayerIndex) ?: return
        if (activePlayer.id != playerId || activePlayer.isAi) return

        val gridW = minOf(bounds.width * 0.88f, bounds.height * 0.65f)
        val gridH = gridW
        val startX = (bounds.width - gridW) / 2f
        val startY = (bounds.height - gridH) / 2f
        val cellW = gridW / 4f
        val cellH = gridH / 4f

        if (position.x in startX..(startX + gridW) && position.y in startY..(startY + gridH)) {
            val col = ((position.x - startX) / cellW).toInt().coerceIn(0, 3)
            val row = ((position.y - startY) / cellH).toInt().coerceIn(0, 3)
            val cardIdx = row * 4 + col
            playerCursors[playerId] = cardIdx
            flipCard(cardIdx)
        }
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}
    override fun onPointerUp(playerId: Int) {}

    override fun getPlayerScore(playerId: Int): Int = playerScores[playerId] ?: 0

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val ranked = players.sortedByDescending { playerScores[it.id] ?: 0 }
        val playerResults = ranked.mapIndexed { index, player ->
            PlayerResult(
                player = player,
                score = playerScores[player.id] ?: 0,
                rank = index + 1
            )
        }

        return GameResult(
            gameId = id,
            rankings = playerResults,
            durationSeconds = 45,
            isDraw = false
        )
    }

    override fun draw(drawScope: DrawScope) {
        val gridW = minOf(bounds.width * 0.88f, bounds.height * 0.65f)
        val gridH = gridW
        val startX = (bounds.width - gridW) / 2f
        val startY = (bounds.height - gridH) / 2f
        val cellW = gridW / 4f
        val cellH = gridH / 4f
        val padding = 6f

        val activePlayer = players.getOrNull(currentPlayerIndex)
        val activeCursor = activePlayer?.let { playerCursors[it.id] } ?: 0

        for (row in 0..3) {
            for (col in 0..3) {
                val idx = row * 4 + col
                val card = cards.getOrNull(idx) ?: continue
                val cardX = startX + col * cellW + padding
                val cardY = startY + row * cellH + padding
                val cardW = cellW - padding * 2
                val cardH = cellH - padding * 2
                val center = Offset(cardX + cardW / 2f, cardY + cardH / 2f)

                // Cursor highlight
                if (idx == activeCursor && !gameOver && activePlayer != null) {
                    drawScope.drawRect(
                        color = activePlayer.color.copy(alpha = 0.35f),
                        topLeft = Offset(cardX - 4f, cardY - 4f),
                        size = Size(cardW + 8f, cardH + 8f)
                    )
                    drawScope.drawRect(
                        color = activePlayer.color,
                        topLeft = Offset(cardX - 4f, cardY - 4f),
                        size = Size(cardW + 8f, cardH + 8f),
                        style = Stroke(width = 3.5f)
                    )
                }

                if (card.isMatched) {
                    val ownerColor = card.ownerPlayerId?.let { pId ->
                        players.find { it.id == pId }?.color
                    } ?: Color.Gray

                    drawScope.drawRect(
                        color = ownerColor.copy(alpha = 0.35f),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH)
                    )
                    drawSymbol(drawScope, card.symbolId, center, cardW * 0.28f, ownerColor)
                } else if (card.flipProgress > 0.5f) {
                    // Face up card
                    drawScope.drawRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH)
                    )
                    drawScope.drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH),
                        style = Stroke(width = 4f)
                    )
                    drawSymbol(drawScope, card.symbolId, center, cardW * 0.32f, Color(0xFF0F172A))
                } else {
                    // Face down card back
                    drawScope.drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFF334155),
                        center = center,
                        radius = cardW * 0.22f
                    )
                }
            }
        }

        // Draw particle sparks & victory confetti
        particleSystem.draw(drawScope)
    }

    private fun drawSymbol(drawScope: DrawScope, symbolId: Int, center: Offset, size: Float, color: Color) {
        when (symbolId) {
            0 -> drawScope.drawCircle(color = color, center = center, radius = size)
            1 -> drawScope.drawRect(color = color, topLeft = Offset(center.x - size, center.y - size), size = Size(size * 2, size * 2))
            2 -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - size)
                    lineTo(center.x + size, center.y)
                    lineTo(center.x, center.y + size)
                    lineTo(center.x - size, center.y)
                    close()
                }
                drawScope.drawPath(path, color)
            }
            3 -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - size)
                    lineTo(center.x + size, center.y + size)
                    lineTo(center.x - size, center.y + size)
                    close()
                }
                drawScope.drawPath(path, color)
            }
            4 -> {
                val bar = size * 0.35f
                drawScope.drawRect(color = color, topLeft = Offset(center.x - bar, center.y - size), size = Size(bar * 2, size * 2))
                drawScope.drawRect(color = color, topLeft = Offset(center.x - size, center.y - bar), size = Size(size * 2, bar * 2))
            }
            5 -> drawScope.drawCircle(color = color, center = center, radius = size, style = Stroke(width = 6f))
            6 -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - size)
                    lineTo(center.x + size * 0.4f, center.y - size * 0.4f)
                    lineTo(center.x + size, center.y)
                    lineTo(center.x + size * 0.4f, center.y + size * 0.4f)
                    lineTo(center.x, center.y + size)
                    lineTo(center.x - size * 0.4f, center.y + size * 0.4f)
                    lineTo(center.x - size, center.y)
                    lineTo(center.x - size * 0.4f, center.y - size * 0.4f)
                    close()
                }
                drawScope.drawPath(path, color)
            }
            else -> {
                drawScope.drawCircle(color = color, center = Offset(center.x - size * 0.45f, center.y), radius = size * 0.35f)
                drawScope.drawCircle(color = color, center = Offset(center.x + size * 0.45f, center.y), radius = size * 0.35f)
            }
        }
    }
}
