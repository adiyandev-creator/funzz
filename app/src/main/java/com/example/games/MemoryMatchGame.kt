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
import kotlin.random.Random

class MemoryMatchGame : MiniGame {
    override val id: String = "memory_match"
    override val title: String = "Memory Match"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    class Card(
        val symbolId: Int, // 0..7
        var isFaceUp: Boolean = false,
        var isMatched: Boolean = false,
        var ownerPlayerId: Int? = null
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

        // 8 pairs = 16 cards
        val symbols = (0..7).flatMap { listOf(it, it) }.shuffled()
        symbols.forEach { sym ->
            cards.add(Card(symbolId = sym))
        }
    }

    override fun update(deltaSeconds: Float) {
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
                // Next player's turn
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size
            }
            return
        }

        // Check if all cards are matched
        if (cards.all { it.isMatched }) {
            gameOver = true
            soundAndHaptics?.playVictory()
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
            // Check if AI knows any matching pair already
            val knownPairs = aiKnownCards.entries
                .filter { !cards[it.key].isMatched }
                .groupBy({ it.value }, { it.key })
                .filter { it.value.size >= 2 }

            if (knownPairs.isNotEmpty()) {
                val pair = knownPairs.values.first()
                flipCard(pair[0])
            } else {
                // Pick random unknown card
                val pick = unmatchedIndices[Random.nextInt(unmatchedIndices.size)]
                flipCard(pick)
            }
        } else {
            val firstCard = cards[firstFlippedIndex!!]
            // Does AI know where the match for firstCard is?
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

        if (firstFlippedIndex == null) {
            firstFlippedIndex = index
        } else if (secondFlippedIndex == null) {
            secondFlippedIndex = index
            val first = cards[firstFlippedIndex!!]
            val second = cards[secondFlippedIndex!!]

            if (first.symbolId == second.symbolId) {
                // Match!
                first.isMatched = true
                second.isMatched = true
                val pId = players[currentPlayerIndex].id
                first.ownerPlayerId = pId
                second.ownerPlayerId = pId
                playerScores[pId] = (playerScores[pId] ?: 0) + 1

                firstFlippedIndex = null
                secondFlippedIndex = null
                soundAndHaptics?.playScore()
            } else {
                // Mismatch
                isWaitingMismatch = true
                soundAndHaptics?.playHit()
            }
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
            flipCard(cardIdx)
        }
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}
    override fun onPointerUp(playerId: Int) {}
    override fun onButtonTap(playerId: Int) {}

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

        for (row in 0..3) {
            for (col in 0..3) {
                val idx = row * 4 + col
                val card = cards.getOrNull(idx) ?: continue
                val cardX = startX + col * cellW + padding
                val cardY = startY + row * cellH + padding
                val cardW = cellW - padding * 2
                val cardH = cellH - padding * 2
                val center = Offset(cardX + cardW / 2f, cardY + cardH / 2f)

                if (card.isMatched) {
                    // Dimmed matched state with owner's tint
                    val ownerColor = card.ownerPlayerId?.let { pId ->
                        players.find { it.id == pId }?.color
                    } ?: Color.Gray

                    drawScope.drawRect(
                        color = ownerColor.copy(alpha = 0.35f),
                        topLeft = Offset(cardX, cardY),
                        size = Size(cardW, cardH)
                    )
                    drawSymbol(drawScope, card.symbolId, center, cardW * 0.28f, ownerColor)
                } else if (card.isFaceUp) {
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
    }

    private fun drawSymbol(drawScope: DrawScope, symbolId: Int, center: Offset, size: Float, color: Color) {
        when (symbolId) {
            0 -> { // Circle
                drawScope.drawCircle(color = color, center = center, radius = size)
            }
            1 -> { // Square
                drawScope.drawRect(
                    color = color,
                    topLeft = Offset(center.x - size, center.y - size),
                    size = Size(size * 2, size * 2)
                )
            }
            2 -> { // Diamond
                val path = Path().apply {
                    moveTo(center.x, center.y - size)
                    lineTo(center.x + size, center.y)
                    lineTo(center.x, center.y + size)
                    lineTo(center.x - size, center.y)
                    close()
                }
                drawScope.drawPath(path, color)
            }
            3 -> { // Triangle
                val path = Path().apply {
                    moveTo(center.x, center.y - size)
                    lineTo(center.x + size, center.y + size)
                    lineTo(center.x - size, center.y + size)
                    close()
                }
                drawScope.drawPath(path, color)
            }
            4 -> { // Cross / Plus
                val bar = size * 0.35f
                drawScope.drawRect(
                    color = color,
                    topLeft = Offset(center.x - bar, center.y - size),
                    size = Size(bar * 2, size * 2)
                )
                drawScope.drawRect(
                    color = color,
                    topLeft = Offset(center.x - size, center.y - bar),
                    size = Size(size * 2, bar * 2)
                )
            }
            5 -> { // Ring
                drawScope.drawCircle(color = color, center = center, radius = size, style = Stroke(width = 6f))
            }
            6 -> { // Star-like octagon
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
            else -> { // Double Dot
                drawScope.drawCircle(color = color, center = Offset(center.x - size * 0.45f, center.y), radius = size * 0.35f)
                drawScope.drawCircle(color = color, center = Offset(center.x + size * 0.45f, center.y), radius = size * 0.35f)
            }
        }
    }
}
