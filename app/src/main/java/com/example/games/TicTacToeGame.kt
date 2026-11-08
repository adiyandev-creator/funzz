package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import kotlin.random.Random

class TicTacToeGame : MiniGame {
    override val id: String = "tic_tac_toe"
    override val title: String = "Tic-Tac-Toe"

    private var players: List<Player> = emptyList()
    private var bounds: Size = Size.Zero
    private var soundAndHaptics: SoundAndHaptics? = null

    // 0 = empty, 1 = Player 1 (X), 2 = Player 2 (O)
    val board = IntArray(9) { 0 }
    var currentPlayerIndex: Int = 0
    var winnerPlayerIndex: Int? = null
    var isDrawState: Boolean = false
    private var gameOver: Boolean = false
    private var winningLine: List<Int>? = null
    private var aiTimer: Float = 0f

    override fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?) {
        this.players = players
        this.bounds = bounds
        this.soundAndHaptics = soundAndHaptics
        reset()
    }

    override fun reset() {
        for (i in 0 until 9) board[i] = 0
        currentPlayerIndex = 0
        winnerPlayerIndex = null
        isDrawState = false
        gameOver = false
        winningLine = null
        aiTimer = 0f
    }

    override fun update(deltaSeconds: Float) {
        if (gameOver) return

        val activePlayer = players.getOrNull(currentPlayerIndex)
        if (activePlayer != null && activePlayer.isAi) {
            aiTimer += deltaSeconds
            if (aiTimer >= 0.6f) {
                aiTimer = 0f
                performAiMove()
            }
        }
    }

    private fun performAiMove() {
        val emptyIndices = board.indices.filter { board[it] == 0 }
        if (emptyIndices.isEmpty()) return

        // 1. Check if AI can win
        for (i in emptyIndices) {
            board[i] = 2
            if (checkWinningLine(2) != null) {
                onCellSelected(i)
                return
            }
            board[i] = 0
        }

        // 2. Block player 1 win
        for (i in emptyIndices) {
            board[i] = 1
            if (checkWinningLine(1) != null) {
                board[i] = 0
                onCellSelected(i)
                return
            }
            board[i] = 0
        }

        // 3. Take center if available
        if (board[4] == 0) {
            onCellSelected(4)
            return
        }

        // 4. Random move
        val choice = emptyIndices[Random.nextInt(emptyIndices.size)]
        onCellSelected(choice)
    }

    fun onCellSelected(index: Int) {
        if (gameOver || index !in 0..8 || board[index] != 0) return

        val playerMark = currentPlayerIndex + 1
        board[index] = playerMark
        soundAndHaptics?.playTap()

        val line = checkWinningLine(playerMark)
        if (line != null) {
            winningLine = line
            winnerPlayerIndex = currentPlayerIndex
            gameOver = true
            soundAndHaptics?.playVictory()
        } else if (board.none { it == 0 }) {
            isDrawState = true
            gameOver = true
            soundAndHaptics?.playHit()
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        }
    }

    private fun checkWinningLine(mark: Int): List<Int>? {
        val winCombos = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // cols
            listOf(0, 4, 8), listOf(2, 4, 6)             // diagonals
        )
        for (combo in winCombos) {
            if (board[combo[0]] == mark && board[combo[1]] == mark && board[combo[2]] == mark) {
                return combo
            }
        }
        return null
    }

    override fun onPointerDown(playerId: Int, position: Offset) {
        if (gameOver) return
        val activePlayer = players.getOrNull(currentPlayerIndex) ?: return
        if (activePlayer.id != playerId || activePlayer.isAi) return

        val boardSize = minOf(bounds.width * 0.85f, bounds.height * 0.55f)
        val startX = (bounds.width - boardSize) / 2f
        val startY = (bounds.height - boardSize) / 2f
        val cellSize = boardSize / 3f

        if (position.x in startX..(startX + boardSize) && position.y in startY..(startY + boardSize)) {
            val col = ((position.x - startX) / cellSize).toInt().coerceIn(0, 2)
            val row = ((position.y - startY) / cellSize).toInt().coerceIn(0, 2)
            onCellSelected(row * 3 + col)
        }
    }

    override fun onPointerMove(playerId: Int, position: Offset) {}
    override fun onPointerUp(playerId: Int) {}
    override fun onButtonTap(playerId: Int) {}

    override fun isGameOver(): Boolean = gameOver

    override fun getResults(): GameResult {
        val rankings = mutableListOf<PlayerResult>()
        val winner = winnerPlayerIndex?.let { players.getOrNull(it) }

        if (winner != null) {
            rankings.add(PlayerResult(winner, 1, 1))
            val losers = players.filter { it.id != winner.id }
            losers.forEach { rankings.add(PlayerResult(it, 0, 2)) }
        } else {
            players.forEach { rankings.add(PlayerResult(it, 0, 1)) }
        }

        return GameResult(
            gameId = id,
            rankings = rankings,
            durationSeconds = 15,
            isDraw = isDrawState
        )
    }

    override fun draw(drawScope: DrawScope) {
        val width = bounds.width
        val height = bounds.height
        val boardSize = minOf(width * 0.85f, height * 0.55f)
        val startX = (width - boardSize) / 2f
        val startY = (height - boardSize) / 2f
        val cellSize = boardSize / 3f

        // Draw Board Background
        drawScope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(startX, startY),
            size = Size(boardSize, boardSize)
        )

        // Draw Grid Lines
        val gridColor = Color(0xFF475569)
        for (i in 1..2) {
            // Vertical
            drawScope.drawLine(
                color = gridColor,
                start = Offset(startX + i * cellSize, startY + 12f),
                end = Offset(startX + i * cellSize, startY + boardSize - 12f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            // Horizontal
            drawScope.drawLine(
                color = gridColor,
                start = Offset(startX + 12f, startY + i * cellSize),
                end = Offset(startX + boardSize - 12f, startY + i * cellSize),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        // Draw Marks
        for (row in 0..2) {
            for (col in 0..2) {
                val mark = board[row * 3 + col]
                val cellCenterX = startX + col * cellSize + cellSize / 2f
                val cellCenterY = startY + row * cellSize + cellSize / 2f
                val markRadius = cellSize * 0.32f

                if (mark == 1) { // Player 1 (X)
                    val p1Color = players.getOrNull(0)?.color ?: Color(0xFF3B82F6)
                    val offset = markRadius * 0.8f
                    drawScope.drawLine(
                        color = p1Color,
                        start = Offset(cellCenterX - offset, cellCenterY - offset),
                        end = Offset(cellCenterX + offset, cellCenterY + offset),
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                    drawScope.drawLine(
                        color = p1Color,
                        start = Offset(cellCenterX + offset, cellCenterY - offset),
                        end = Offset(cellCenterX - offset, cellCenterY + offset),
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                } else if (mark == 2) { // Player 2 (O)
                    val p2Color = players.getOrNull(1)?.color ?: Color(0xFFEF4444)
                    drawScope.drawCircle(
                        color = p2Color,
                        center = Offset(cellCenterX, cellCenterY),
                        radius = markRadius,
                        style = Stroke(width = 14f)
                    )
                }
            }
        }

        // Draw Win Strike Line if exists
        winningLine?.let { line ->
            val startRow = line[0] / 3
            val startCol = line[0] % 3
            val endRow = line[2] / 3
            val endCol = line[2] % 3

            val p1 = Offset(startX + startCol * cellSize + cellSize / 2f, startY + startRow * cellSize + cellSize / 2f)
            val p2 = Offset(startX + endCol * cellSize + cellSize / 2f, startY + endRow * cellSize + cellSize / 2f)

            drawScope.drawLine(
                color = Color.White,
                start = p1,
                end = p2,
                strokeWidth = 12f,
                cap = StrokeCap.Round
            )
        }
    }
}
