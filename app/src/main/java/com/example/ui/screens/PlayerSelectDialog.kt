package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Player
import com.example.ui.theme.PlayerPalette

@Composable
fun PlayerSelectDialog(
    initialCount: Int = 2,
    minAllowed: Int = 1,
    maxAllowed: Int = 4,
    onDismiss: () -> Unit,
    onStartGame: (List<Player>) -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(initialCount.coerceIn(minAllowed, maxAllowed)) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(24.dp)
                .testTag("player_select_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How many players?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1 to 4 Players Selection Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (count in 1..4) {
                    val isAvailable = count in minAllowed..maxAllowed
                    val isSelected = count == selectedCount

                    val optionColor = PlayerPalette.getColor(count - 1)
                    val cardBorder = if (isSelected) {
                        optionColor
                    } else {
                        MaterialTheme.colorScheme.outline
                    }

                    val cardBg = if (isSelected) {
                        optionColor.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isAvailable) cardBorder else Color.LightGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = isAvailable) {
                                selectedCount = count
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("player_option_$count"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Player avatars preview
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                for (i in 0 until count) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(PlayerPalette.getColor(i), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = when (count) {
                                    1 -> "1 PLAYER (vs AI)"
                                    else -> "$count PLAYERS"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (isAvailable) MaterialTheme.colorScheme.onBackground else Color.Gray
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = optionColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Button
            Button(
                onClick = {
                    val playersList = mutableListOf<Player>()
                    for (i in 0 until selectedCount) {
                        playersList.add(
                            Player(
                                id = i,
                                name = "Player ${i + 1}",
                                color = PlayerPalette.getColor(i),
                                isAi = (selectedCount == 1 && i > 0)
                            )
                        )
                    }
                    if (selectedCount == 1 && minAllowed > 1) {
                        // In games requiring at least 2 players, add AI as Player 2
                        playersList.add(
                            Player(
                                id = 1,
                                name = "AI Bot",
                                color = PlayerPalette.getColor(1),
                                isAi = true
                            )
                        )
                    }
                    onStartGame(playersList)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_start_game_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Start Game",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
