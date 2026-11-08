package com.example.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val gameId: String,
    val matchesPlayed: Int = 0,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val player3Wins: Int = 0,
    val player4Wins: Int = 0,
    val bestScore: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
)
