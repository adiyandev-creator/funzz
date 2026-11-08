package com.example

import androidx.compose.ui.geometry.Size
import com.example.data.model.GameRegistry
import com.example.data.model.Player
import com.example.games.GameFactory
import com.example.games.TicTacToeGame
import com.example.ui.theme.PlayerPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGameRegistryHasEightGames() {
        assertEquals(8, GameRegistry.games.size)
        val expectedIds = listOf(
            "spinner_battle",
            "paint_arena",
            "micro_racing",
            "reaction_battle",
            "tic_tac_toe",
            "memory_match",
            "tug_of_war",
            "basketball_shots"
        )
        expectedIds.forEach { id ->
            val game = GameRegistry.getGameById(id)
            assertNotNull("Game with id $id should exist in registry", game)
            assertTrue("Game minPlayers should be >= 1", game!!.minPlayers >= 1)
            assertTrue("Game maxPlayers should be <= 4", game.maxPlayers <= 4)
        }
    }

    @Test
    fun testGameFactoryCreatesInstances() {
        GameRegistry.games.forEach { gameDef ->
            val gameInstance = GameFactory.createGame(gameDef.id)
            assertNotNull("GameFactory should create instance for ${gameDef.id}", gameInstance)
            assertEquals(gameDef.id, gameInstance.id)
        }
    }

    @Test
    fun testTicTacToeLogic() {
        val game = TicTacToeGame()
        val players = listOf(
            Player(0, "P1", PlayerPalette.player1),
            Player(1, "P2", PlayerPalette.player2)
        )
        game.init(players, Size(300f, 300f), null)

        // Play row 0: P1, P2, P1, P2, P1
        game.onCellSelected(0) // P1 -> cell 0
        assertEquals(1, game.board[0])
        assertEquals(1, game.currentPlayerIndex)

        game.onCellSelected(3) // P2 -> cell 3
        assertEquals(2, game.board[3])
        assertEquals(0, game.currentPlayerIndex)

        game.onCellSelected(1) // P1 -> cell 1
        assertEquals(1, game.board[1])

        game.onCellSelected(4) // P2 -> cell 4
        assertEquals(2, game.board[4])

        game.onCellSelected(2) // P1 -> cell 2 (Wins row 0!)
        assertEquals(1, game.board[2])
        assertTrue("Game should be over on row complete", game.isGameOver())
        assertEquals(0, game.winnerPlayerIndex)
    }
}
