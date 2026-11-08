package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class GameCategory(val displayName: String) {
    ACTION("Action"),
    RACING("Racing"),
    SPORTS("Sports"),
    PUZZLES("Puzzles"),
    PARTY("Party")
}

data class Player(
    val id: Int, // 0 to 3
    val name: String,
    val color: Color,
    val isAi: Boolean = false,
    var score: Int = 0
)

data class GameResult(
    val gameId: String,
    val rankings: List<PlayerResult>,
    val durationSeconds: Int,
    val isDraw: Boolean = false
)

data class PlayerResult(
    val player: Player,
    val score: Int,
    val rank: Int // 1st, 2nd, 3rd, 4th
)

data class GameDefinition(
    val id: String,
    val title: String,
    val category: GameCategory,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val difficulty: String = "Easy",
    val avgDurationSeconds: Int = 30,
    val isAvailable: Boolean = true
)

object GameRegistry {
    val games = listOf(
        GameDefinition(
            id = "spinner_battle",
            title = "Spinner Battle",
            category = GameCategory.ACTION,
            description = "Spin fast, collide with rivals, and knock them out of the ring to be the last one spinning.",
            minPlayers = 2,
            maxPlayers = 4,
            difficulty = "Medium",
            avgDurationSeconds = 40
        ),
        GameDefinition(
            id = "paint_arena",
            title = "Paint Arena",
            category = GameCategory.PARTY,
            description = "Roll around the canvas to paint the floor with your color. The player with the highest coverage wins!",
            minPlayers = 2,
            maxPlayers = 4,
            difficulty = "Easy",
            avgDurationSeconds = 45
        ),
        GameDefinition(
            id = "micro_racing",
            title = "Micro Racing",
            category = GameCategory.RACING,
            description = "Steer tiny high-speed slot cars around the circuit. Complete laps without crashing into walls.",
            minPlayers = 1,
            maxPlayers = 4,
            difficulty = "Medium",
            avgDurationSeconds = 45
        ),
        GameDefinition(
            id = "reaction_battle",
            title = "Reaction Battle",
            category = GameCategory.PARTY,
            description = "Wait for the green signal and tap instantly! Don't tap early or you will be penalized.",
            minPlayers = 1,
            maxPlayers = 4,
            difficulty = "Easy",
            avgDurationSeconds = 20
        ),
        GameDefinition(
            id = "tic_tac_toe",
            title = "Tic-Tac-Toe",
            category = GameCategory.PUZZLES,
            description = "The classic battle of wits. Align three symbols in a row against a friend or smart AI.",
            minPlayers = 1,
            maxPlayers = 2,
            difficulty = "Easy",
            avgDurationSeconds = 30
        ),
        GameDefinition(
            id = "memory_match",
            title = "Memory Match",
            category = GameCategory.PUZZLES,
            description = "Uncover cards, memorize symbol locations, and collect matching pairs before your opponents.",
            minPlayers = 1,
            maxPlayers = 4,
            difficulty = "Easy",
            avgDurationSeconds = 60
        ),
        GameDefinition(
            id = "tug_of_war",
            title = "Tug of War",
            category = GameCategory.PARTY,
            description = "Tap with maximum speed to pull the center flag past your team's goal line.",
            minPlayers = 2,
            maxPlayers = 4,
            difficulty = "Easy",
            avgDurationSeconds = 25
        ),
        GameDefinition(
            id = "basketball_shots",
            title = "Basketball Shots",
            category = GameCategory.SPORTS,
            description = "Aim the trajectory arc, set power, and sink as many basketball hoops as possible.",
            minPlayers = 1,
            maxPlayers = 4,
            difficulty = "Medium",
            avgDurationSeconds = 45
        )
    )

    fun getGameById(id: String): GameDefinition? = games.find { it.id == id }
}
