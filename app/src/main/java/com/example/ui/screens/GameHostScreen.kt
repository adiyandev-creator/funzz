package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameDefinition
import com.example.data.model.GameRegistry
import com.example.data.model.GameResult
import com.example.data.model.Player
import com.example.data.storage.AppDatabase
import com.example.data.storage.GameStatsEntity
import com.example.engine.MiniGame
import com.example.engine.SoundAndHaptics
import com.example.games.GameFactory
import com.example.ui.components.MultiTouchOverlay
import com.example.ui.components.PauseDialog
import com.example.ui.components.PodiumView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameHostScreen(
    gameId: String,
    players: List<Player>,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundAndHaptics = remember { SoundAndHaptics(context) }
    val database = remember { AppDatabase.getDatabase(context) }

    val gameDefinition: GameDefinition = remember(gameId) {
        GameRegistry.getGameById(gameId) ?: GameRegistry.games.first()
    }

    val miniGame: MiniGame = remember(gameId) {
        GameFactory.createGame(gameId)
    }

    var isPaused by remember { mutableStateOf(false) }
    var isCountdownActive by remember { mutableStateOf(true) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameResult by remember { mutableStateOf<GameResult?>(null) }

    // Canvas bounds
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    DisposableEffect(Unit) {
        onDispose {
            soundAndHaptics.release()
        }
    }

    // Countdown 3, 2, 1, GO!
    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            countdownValue = 3
            soundAndHaptics.playCountdownBeep(isGo = false)
            delay(800)
            countdownValue = 2
            soundAndHaptics.playCountdownBeep(isGo = false)
            delay(800)
            countdownValue = 1
            soundAndHaptics.playCountdownBeep(isGo = false)
            delay(800)
            countdownValue = 0 // GO!
            soundAndHaptics.playCountdownBeep(isGo = true)
            delay(500)
            isCountdownActive = false
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(isCountdownActive, isPaused, isGameOver, canvasSize) {
        if (!isCountdownActive && !isPaused && !isGameOver && canvasSize.width > 0f) {
            miniGame.init(players, canvasSize, soundAndHaptics)
            var lastTimeNanos = 0L

            while (!isGameOver && !isPaused) {
                withFrameNanos { frameNanos ->
                    if (lastTimeNanos != 0L) {
                        val deltaSeconds = ((frameNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                        miniGame.update(deltaSeconds)

                        if (miniGame.isGameOver()) {
                            isGameOver = true
                            val result = miniGame.getResults()
                            gameResult = result

                            // Persist statistics locally
                            scope.launch {
                                val current = database.statsDao().getStats(gameId)
                                val winner = result.rankings.firstOrNull { it.rank == 1 }
                                val p1Win = if (winner?.player?.id == 0) 1 else 0
                                val p2Win = if (winner?.player?.id == 1) 1 else 0
                                val p3Win = if (winner?.player?.id == 2) 1 else 0
                                val p4Win = if (winner?.player?.id == 3) 1 else 0

                                val updated = GameStatsEntity(
                                    gameId = gameId,
                                    matchesPlayed = (current?.matchesPlayed ?: 0) + 1,
                                    player1Wins = (current?.player1Wins ?: 0) + p1Win,
                                    player2Wins = (current?.player2Wins ?: 0) + p2Win,
                                    player3Wins = (current?.player3Wins ?: 0) + p3Win,
                                    player4Wins = (current?.player4Wins ?: 0) + p4Win,
                                    bestScore = maxOf(current?.bestScore ?: 0, winner?.score ?: 0),
                                    isFavorite = current?.isFavorite ?: false,
                                    lastPlayedTimestamp = System.currentTimeMillis()
                                )
                                database.statsDao().insertOrUpdate(updated)
                            }
                        }
                    }
                    lastTimeNanos = frameNanos
                }
            }
        }
    }

    fun restartGame() {
        isGameOver = false
        gameResult = null
        isPaused = false
        miniGame.reset()
        isCountdownActive = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("game_host_screen")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(widthPx, heightPx) {
            canvasSize = Size(widthPx, heightPx)
        }

        // MiniGame Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            miniGame.onPointerDown(0, offset)
                            tryAwaitRelease()
                            miniGame.onPointerUp(0)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            miniGame.onPointerDown(0, offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            miniGame.onPointerMove(0, change.position)
                        },
                        onDragEnd = {
                            miniGame.onPointerUp(0)
                        },
                        onDragCancel = {
                            miniGame.onPointerUp(0)
                        }
                    )
                }
        ) {
            miniGame.draw(this)
        }

        // Multi-touch Overlay with Reference 3 Pods
        if (!isCountdownActive && !isGameOver) {
            val liveScores = remember(miniGame, isGameOver) {
                players.associate { it.id to it.score }
            }
            MultiTouchOverlay(
                players = players,
                scores = liveScores,
                onPlayerTap = { pId ->
                    miniGame.onButtonTap(pId)
                },
                onExitClick = {
                    isPaused = true
                }
            )
        }

        // Minimal Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isPaused = true },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(42.dp)
                    .testTag("pause_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Game Title Pill
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = gameDefinition.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Countdown Overlay (3, 2, 1, GO!)
        AnimatedVisibility(
            visible = isCountdownActive,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isCountdownActive) 1.15f else 0.8f,
                animationSpec = tween(200),
                label = "countdown_scale"
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdownValue > 0) "$countdownValue" else "GO!",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = if (countdownValue > 0) Color.White else Color(0xFF22C55E)
                )
            }
        }

        // Game Over Podium Results Dialog
        if (isGameOver && gameResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PodiumView(
                    result = gameResult!!,
                    onRematch = { restartGame() },
                    onReturnHome = onExit
                )
            }
        }

        // Pause Menu Dialog
        if (isPaused) {
            PauseDialog(
                onResume = { isPaused = false },
                onRestart = { restartGame() },
                onExit = onExit
            )
        }
    }
}
