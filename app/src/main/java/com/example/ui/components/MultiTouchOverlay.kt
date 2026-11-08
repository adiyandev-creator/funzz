package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.ui.theme.PodPillBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MultiTouchOverlay(
    players: List<Player>,
    scores: Map<Int, Int> = emptyMap(),
    onPlayerTap: (Int) -> Unit,
    onJoystickMove: (Int, Float, Float) -> Unit = { _, _, _ -> },
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (players.size) {
            1 -> {
                // 1 Player: Bottom center joystick pod
                InteractiveJoystickPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.BottomCenter,
                    onTap = { onPlayerTap(players[0].id) },
                    onMove = { x, y -> onJoystickMove(players[0].id, x, y) }
                )
            }
            2 -> {
                // 2 Players: Top-Center vs Bottom-Center
                InteractiveJoystickPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopCenter,
                    onTap = { onPlayerTap(players[0].id) },
                    onMove = { x, y -> onJoystickMove(players[0].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.BottomCenter,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) },
                    onMove = { x, y -> if (!players[1].isAi) onJoystickMove(players[1].id, x, y) }
                )
            }
            3 -> {
                // 3 Players: Top-Left (P1), Top-Right (P2), Bottom-Left (P3)
                InteractiveJoystickPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopStart,
                    onTap = { onPlayerTap(players[0].id) },
                    onMove = { x, y -> onJoystickMove(players[0].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.TopEnd,
                    isReversed = true,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) },
                    onMove = { x, y -> if (!players[1].isAi) onJoystickMove(players[1].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[2],
                    score = scores[players[2].id] ?: 0,
                    alignment = Alignment.BottomStart,
                    onTap = { if (!players[2].isAi) onPlayerTap(players[2].id) },
                    onMove = { x, y -> if (!players[2].isAi) onJoystickMove(players[2].id, x, y) }
                )
            }
            4 -> {
                // 4 Players: 4 Corners (Exact Reference Layout from image_69fcaf87.png!)
                InteractiveJoystickPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopStart,
                    onTap = { onPlayerTap(players[0].id) },
                    onMove = { x, y -> onJoystickMove(players[0].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.TopEnd,
                    isReversed = true,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) },
                    onMove = { x, y -> if (!players[1].isAi) onJoystickMove(players[1].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[2],
                    score = scores[players[2].id] ?: 0,
                    alignment = Alignment.BottomStart,
                    onTap = { if (!players[2].isAi) onPlayerTap(players[2].id) },
                    onMove = { x, y -> if (!players[2].isAi) onJoystickMove(players[2].id, x, y) }
                )
                InteractiveJoystickPod(
                    player = players[3],
                    score = scores[players[3].id] ?: 0,
                    alignment = Alignment.BottomEnd,
                    isReversed = true,
                    onTap = { if (!players[3].isAi) onPlayerTap(players[3].id) },
                    onMove = { x, y -> if (!players[3].isAi) onJoystickMove(players[3].id, x, y) }
                )
            }
        }

        // Clean circular white 'X' exit button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(42.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                .testTag("ingame_exit_button")
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onExitClick() })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Exit",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.InteractiveJoystickPod(
    player: Player,
    score: Int,
    alignment: Alignment,
    isReversed: Boolean = false,
    onTap: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Knob offset animation
    val knobOffsetX = remember { Animatable(0f) }
    val knobOffsetY = remember { Animatable(0f) }
    val maxRadiusPx = with(density) { 26.dp.toPx() }

    Row(
        modifier = Modifier
            .align(alignment)
            .padding(14.dp)
            .testTag("player_pod_${player.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isReversed) {
            // Attached Score Capsule
            PodScoreBadge(score = score, playerColor = player.color)
            Spacer(modifier = Modifier.width((-12).dp))
            // Joystick Ring & Draggable Knob
            JoystickRing(
                player = player,
                knobOffsetX = knobOffsetX.value,
                knobOffsetY = knobOffsetY.value,
                onDrag = { dx, dy ->
                    val newX = knobOffsetX.value + dx
                    val newY = knobOffsetY.value + dy
                    val dist = sqrt(newX * newX + newY * newY)
                    val clampedDist = dist.coerceAtMost(maxRadiusPx)
                    val ratio = if (dist > 0.001f) clampedDist / dist else 0f
                    val finalX = newX * ratio
                    val finalY = newY * ratio

                    scope.launch {
                        knobOffsetX.snapTo(finalX)
                        knobOffsetY.snapTo(finalY)
                    }

                    val normX = finalX / maxRadiusPx
                    val normY = finalY / maxRadiusPx
                    onMove(normX, normY)
                },
                onRelease = {
                    scope.launch {
                        knobOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                    scope.launch {
                        knobOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                    onMove(0f, 0f)
                },
                onTap = onTap
            )
        } else {
            // Joystick Ring & Draggable Knob
            JoystickRing(
                player = player,
                knobOffsetX = knobOffsetX.value,
                knobOffsetY = knobOffsetY.value,
                onDrag = { dx, dy ->
                    val newX = knobOffsetX.value + dx
                    val newY = knobOffsetY.value + dy
                    val dist = sqrt(newX * newX + newY * newY)
                    val clampedDist = dist.coerceAtMost(maxRadiusPx)
                    val ratio = if (dist > 0.001f) clampedDist / dist else 0f
                    val finalX = newX * ratio
                    val finalY = newY * ratio

                    scope.launch {
                        knobOffsetX.snapTo(finalX)
                        knobOffsetY.snapTo(finalY)
                    }

                    val normX = finalX / maxRadiusPx
                    val normY = finalY / maxRadiusPx
                    onMove(normX, normY)
                },
                onRelease = {
                    scope.launch {
                        knobOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                    scope.launch {
                        knobOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                    onMove(0f, 0f)
                },
                onTap = onTap
            )
            Spacer(modifier = Modifier.width((-12).dp))
            // Attached Score Capsule
            PodScoreBadge(score = score, playerColor = player.color)
        }
    }
}

@Composable
private fun JoystickRing(
    player: Player,
    knobOffsetX: Float,
    knobOffsetY: Float,
    onDrag: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    onTap: () -> Unit
) {
    val ringSize = 78.dp
    val knobSize = 42.dp

    Box(
        modifier = Modifier
            .size(ringSize)
            .background(player.color, CircleShape)
            .border(3.5.dp, Color.Black.copy(alpha = 0.9f), CircleShape)
            .pointerInput(player.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(player.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Dark Well
        Box(
            modifier = Modifier
                .size(ringSize - 16.dp)
                .background(Color(0xFF141322), CircleShape)
                .border(2.dp, Color.Black, CircleShape)
        )

        // Draggable Joystick Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) }
                .size(knobSize)
                .background(player.color, CircleShape)
                .border(2.5.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner Core Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun PodScoreBadge(
    score: Int,
    playerColor: Color
) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 48.dp)
            .background(PodPillBackground, RoundedCornerShape(24.dp))
            .border(2.5.dp, Color.Black.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = playerColor
        )
    }
}
