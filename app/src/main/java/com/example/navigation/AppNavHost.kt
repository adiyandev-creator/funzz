package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.GameDefinition
import com.example.data.model.GameRegistry
import com.example.data.model.Player
import com.example.ui.components.AppBottomBar
import com.example.ui.components.BottomDestination
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GameHostScreen
import com.example.ui.screens.GameLibraryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerSelectDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TournamentHostScreen
import com.example.ui.screens.TournamentScreen

@Composable
fun AppNavHost(
    onThemeChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(BottomDestination.HOME) }

    // Active Game & Tournament States
    var activeGameId by remember { mutableStateOf<String?>(null) }
    var activePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }

    var isTournamentActive by remember { mutableStateOf(false) }
    var tournamentTotalRounds by remember { mutableStateOf(3) }
    var tournamentPlaylist by remember { mutableStateOf<List<GameDefinition>>(emptyList()) }

    // Player Select Dialog State
    var pendingGameForPlayerSelect by remember { mutableStateOf<GameDefinition?>(null) }
    var playerFilterFromHome by remember { mutableStateOf<Int?>(null) }

    // Handle Hardware / Gesture Back
    BackHandler(enabled = activeGameId != null || isTournamentActive || pendingGameForPlayerSelect != null || currentTab != BottomDestination.HOME) {
        when {
            pendingGameForPlayerSelect != null -> pendingGameForPlayerSelect = null
            activeGameId != null -> activeGameId = null
            isTournamentActive -> isTournamentActive = false
            currentTab != BottomDestination.HOME -> currentTab = BottomDestination.HOME
        }
    }

    when {
        // Active Tournament
        isTournamentActive && tournamentPlaylist.isNotEmpty() -> {
            TournamentHostScreen(
                players = activePlayers,
                totalRounds = tournamentTotalRounds,
                playlist = tournamentPlaylist,
                onFinishTournament = {
                    isTournamentActive = false
                }
            )
        }

        // Active Solo / Multiplayer Mini-Game
        activeGameId != null -> {
            GameHostScreen(
                gameId = activeGameId!!,
                players = activePlayers,
                onExit = {
                    activeGameId = null
                }
            )
        }

        // Standard App Shell with Bottom Bar
        else -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    AppBottomBar(
                        currentRoute = currentTab.route,
                        onNavigate = { destination ->
                            currentTab = destination
                            if (destination != BottomDestination.GAMES) {
                                playerFilterFromHome = null
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        BottomDestination.HOME -> {
                            HomeScreen(
                                onNavigateToGame = { game ->
                                    pendingGameForPlayerSelect = game
                                },
                                onNavigateToPlayerMode = { count ->
                                    playerFilterFromHome = count
                                    currentTab = BottomDestination.GAMES
                                },
                                onNavigateToTournament = {
                                    // Start default tournament setup or navigate
                                    val defaultPlayers = listOf(
                                        Player(0, "Player 1", com.example.ui.theme.PlayerPalette.player1),
                                        Player(1, "Player 2", com.example.ui.theme.PlayerPalette.player2)
                                    )
                                    activePlayers = defaultPlayers
                                    tournamentTotalRounds = 3
                                    tournamentPlaylist = GameRegistry.games.shuffled().take(3)
                                    isTournamentActive = true
                                },
                                onNavigateToSettings = {
                                    currentTab = BottomDestination.SETTINGS
                                },
                                onBrowseAll = {
                                    currentTab = BottomDestination.GAMES
                                }
                            )
                        }

                        BottomDestination.GAMES -> {
                            GameLibraryScreen(
                                initialPlayerFilter = playerFilterFromHome,
                                onSelectGame = { game ->
                                    pendingGameForPlayerSelect = game
                                }
                            )
                        }

                        BottomDestination.FAVORITES -> {
                            FavoritesScreen(
                                onSelectGame = { game ->
                                    pendingGameForPlayerSelect = game
                                }
                            )
                        }

                        BottomDestination.SETTINGS -> {
                            SettingsScreen(
                                onThemeChanged = onThemeChanged
                            )
                        }
                    }
                }
            }

            // Player Selection Dialog Overlay
            pendingGameForPlayerSelect?.let { game ->
                PlayerSelectDialog(
                    initialCount = 2,
                    minAllowed = game.minPlayers,
                    maxAllowed = game.maxPlayers,
                    onDismiss = { pendingGameForPlayerSelect = null },
                    onStartGame = { chosenPlayers ->
                        pendingGameForPlayerSelect = null
                        activePlayers = chosenPlayers
                        activeGameId = game.id
                    }
                )
            }
        }
    }
}
