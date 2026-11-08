package com.example.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM game_stats")
    fun getAllStats(): Flow<List<GameStatsEntity>>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun getStats(gameId: String): GameStatsEntity?

    @Query("SELECT * FROM game_stats WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<GameStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: GameStatsEntity)

    @Query("UPDATE game_stats SET isFavorite = :isFavorite WHERE gameId = :gameId")
    suspend fun updateFavorite(gameId: String, isFavorite: Boolean)

    @Query("DELETE FROM game_stats")
    suspend fun resetAllStats()
}
