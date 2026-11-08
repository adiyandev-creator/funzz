package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameDefinition
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.model.PlayerResult
import com.example.ui.components.PodiumView

@Composable
fun TournamentHostScreen(
    players: List<Player>,
    totalRounds: Int,
    playlist: List<GameDefinition>,
    onFinishTournament: () -> Unit
) {
    var currentRoundIndex by remember { mutableIntStateOf(0) }
    val tournamentPoints = remember {
        mutableStateMapOf<Int, Int>().apply {
            players.forEach { put(it.id, 0) }
        }
    }
    var isTournamentComplete by remember { mutableStateOf(false) }

    val currentGame = remember(currentRoundIndex) {
        playlist.getOrNull(currentRoundIndex) ?: playlist.first()
    }

    if (isTournamentComplete) {
        // Grand Final Championship Podium
        val finalRankings = players.sortedByDescending { tournamentPoints[it.id] ?: 0 }.mapIndexed { idx, p ->
            PlayerResult(
                player = p,
                score = tournamentPoints[p.id] ?: 0,
                rank = idx + 1
            )
        }
        val finalResult = GameResult(
            gameId = "tournament_cup",
            rankings = finalRankings,
            durationSeconds = 120,
            isDraw = false
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .navigationBarsPadding()
                .padding(24.dp)
                .testTag("tournament_final_podium"),
            contentAlignment = Alignment.Center
        ) {
            PodiumView(
                result = finalResult,
                onRematch = {
                    currentRoundIndex = 0
                    players.forEach { tournamentPoints[it.id] = 0 }
                    isTournamentComplete = false
                },
                onReturnHome = onFinishTournament
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            GameHostScreen(
                gameId = currentGame.id,
                players = players,
                onExit = {
                    // Check if more rounds remain
                    if (currentRoundIndex + 1 < totalRounds && currentRoundIndex + 1 < playlist.size) {
                        currentRoundIndex++
                    } else {
                        isTournamentComplete = true
                    }
                }
            )

            // Tournament Banner Header
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Cup Round ${currentRoundIndex + 1} of $totalRounds",
                    color = Color(0xFFF59E0B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
