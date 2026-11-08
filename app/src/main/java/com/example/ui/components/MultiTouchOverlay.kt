package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.ui.theme.PodPillBackground

@Composable
fun MultiTouchOverlay(
    players: List<Player>,
    scores: Map<Int, Int> = emptyMap(),
    onPlayerTap: (Int) -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (players.size) {
            1 -> {
                // 1 Player: Bottom center pod
                TactileControlPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.BottomCenter,
                    onTap = { onPlayerTap(players[0].id) }
                )
            }
            2 -> {
                // 2 Players: Top-Center vs Bottom-Center (Head to Head)
                TactileControlPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopCenter,
                    onTap = { onPlayerTap(players[0].id) }
                )
                TactileControlPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.BottomCenter,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) }
                )
            }
            3 -> {
                // 3 Players: Top-Left (P1), Top-Right (P2), Bottom-Start (P3) (Exact Reference 3 layout!)
                TactileControlPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopStart,
                    onTap = { onPlayerTap(players[0].id) }
                )
                TactileControlPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.TopEnd,
                    isReversed = true,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) }
                )
                TactileControlPod(
                    player = players[2],
                    score = scores[players[2].id] ?: 0,
                    alignment = Alignment.BottomStart,
                    onTap = { if (!players[2].isAi) onPlayerTap(players[2].id) }
                )
            }
            4 -> {
                // 4 Players: 4 Quadrant Pods
                TactileControlPod(
                    player = players[0],
                    score = scores[players[0].id] ?: 0,
                    alignment = Alignment.TopStart,
                    onTap = { onPlayerTap(players[0].id) }
                )
                TactileControlPod(
                    player = players[1],
                    score = scores[players[1].id] ?: 0,
                    alignment = Alignment.TopEnd,
                    isReversed = true,
                    onTap = { if (!players[1].isAi) onPlayerTap(players[1].id) }
                )
                TactileControlPod(
                    player = players[2],
                    score = scores[players[2].id] ?: 0,
                    alignment = Alignment.BottomStart,
                    onTap = { if (!players[2].isAi) onPlayerTap(players[2].id) }
                )
                TactileControlPod(
                    player = players[3],
                    score = scores[players[3].id] ?: 0,
                    alignment = Alignment.BottomEnd,
                    isReversed = true,
                    onTap = { if (!players[3].isAi) onPlayerTap(players[3].id) }
                )
            }
        }

        // Small White Circular 'X' Exit Button (Reference 3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(40.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
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
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.TactileControlPod(
    player: Player,
    score: Int,
    alignment: Alignment,
    isReversed: Boolean = false,
    onTap: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "pod_scale")

    Row(
        modifier = Modifier
            .align(alignment)
            .padding(16.dp)
            .scale(scale)
            .testTag("player_pod_${player.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isReversed) {
            // Score badge first, then ring button
            PodScoreBadge(score = score, playerColor = player.color)
            Spacer(modifier = Modifier.width((-12).dp))
            PodRingButton(player = player, isPressed = isPressed, onTouch = { pressed ->
                isPressed = pressed
                if (pressed) onTap()
            })
        } else {
            // Ring button first, then score badge
            PodRingButton(player = player, isPressed = isPressed, onTouch = { pressed ->
                isPressed = pressed
                if (pressed) onTap()
            })
            Spacer(modifier = Modifier.width((-12).dp))
            PodScoreBadge(score = score, playerColor = player.color)
        }
    }
}

@Composable
private fun PodRingButton(
    player: Player,
    isPressed: Boolean,
    onTouch: (Boolean) -> Unit
) {
    val buttonSize = 72.dp
    val outerRingWidth = 10.dp

    Box(
        modifier = Modifier
            .size(buttonSize)
            .background(player.color, CircleShape)
            .border(3.dp, Color.Black.copy(alpha = 0.85f), CircleShape)
            .pointerInput(player.id) {
                detectTapGestures(
                    onPress = {
                        onTouch(true)
                        tryAwaitRelease()
                        onTouch(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Dark Recess
        Box(
            modifier = Modifier
                .size(buttonSize - outerRingWidth * 2)
                .background(Color(0xFF161522), CircleShape)
                .border(2.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner Core Indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (isPressed) player.color else player.color.copy(alpha = 0.45f),
                        CircleShape
                    )
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
            .size(width = 62.dp, height = 48.dp)
            .background(PodPillBackground, RoundedCornerShape(24.dp))
            .border(2.5.dp, Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
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
