package com.example.games

import com.example.engine.MiniGame

object GameFactory {
    fun createGame(gameId: String): MiniGame {
        return when (gameId) {
            "tic_tac_toe" -> TicTacToeGame()
            "reaction_battle" -> ReactionBattleGame()
            "paint_arena" -> PaintArenaGame()
            "spinner_battle" -> SpinnerBattleGame()
            "micro_racing" -> MicroRacingGame()
            "memory_match" -> MemoryMatchGame()
            "tug_of_war" -> TugOfWarGame()
            "basketball_shots" -> BasketballShotsGame()
            else -> ReactionBattleGame()
        }
    }
}
