package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.GameResult
import com.example.data.model.Player

interface MiniGame {
    val id: String
    val title: String

    fun init(players: List<Player>, bounds: Size, soundAndHaptics: SoundAndHaptics?)
    fun update(deltaSeconds: Float)
    fun draw(drawScope: DrawScope)
    fun onPointerDown(playerId: Int, position: Offset)
    fun onPointerMove(playerId: Int, position: Offset)
    fun onPointerUp(playerId: Int)
    fun onButtonTap(playerId: Int)
    fun onJoystick(playerId: Int, x: Float, y: Float) {}
    fun getPlayerScore(playerId: Int): Int = 0
    fun getRemainingSeconds(): Int? = null
    fun isGameOver(): Boolean
    fun getResults(): GameResult
    fun reset()
}
