package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameDefinition
import com.example.data.model.GameRegistry
import com.example.ui.theme.CardNavyBackground
import com.example.ui.theme.CardNavyBorder
import com.example.ui.theme.ColorPlayer1Red
import com.example.ui.theme.ColorPlayer2Blue
import com.example.ui.theme.ColorPlayer3Green
import com.example.ui.theme.ColorPlayer4Yellow
import com.example.ui.theme.ColorTeamPurple

@Composable
fun HomeScreen(
    onNavigateToGame: (GameDefinition) -> Unit,
    onNavigateToPlayerMode: (Int) -> Unit,
    onNavigateToTournament: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBrowseAll: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .testTag("home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBrowseAll,
                modifier = Modifier.testTag("all_games_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Game Library",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Reference Header: 1234 PLAYER GAMES
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "1", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ColorPlayer1Red)
                Text(text = "2", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ColorPlayer2Blue)
                Text(text = "3", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ColorPlayer3Green)
                Text(text = "4", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ColorPlayer4Yellow)
            }
            Text(
                text = "PLAYER GAMES",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4-Color Underline Bar (from Reference 2)
            Row(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                Box(modifier = Modifier.weight(1f).height(4.dp).background(ColorPlayer1Red))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(ColorPlayer2Blue))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(ColorPlayer3Green))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(ColorPlayer4Yellow))
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 2x2 Player Selection Cards (Reference 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TactilePlayerCard(
                number = "1",
                numberColor = ColorPlayer1Red,
                label = "PLAYER",
                pillColor = ColorPlayer1Red,
                onClick = { onNavigateToPlayerMode(1) },
                modifier = Modifier.weight(1f)
            )

            TactilePlayerCard(
                number = "2",
                numberColor = ColorPlayer2Blue,
                label = "PLAYERS",
                pillColor = ColorPlayer2Blue,
                onClick = { onNavigateToPlayerMode(2) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TactilePlayerCard(
                number = "3",
                numberColor = ColorPlayer3Green,
                label = "PLAYERS",
                pillColor = ColorPlayer3Green,
                onClick = { onNavigateToPlayerMode(3) },
                modifier = Modifier.weight(1f)
            )

            TactilePlayerCard(
                number = "4",
                numberColor = ColorPlayer4Yellow,
                label = "PLAYERS",
                pillColor = ColorPlayer4Yellow,
                onClick = { onNavigateToPlayerMode(4) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2vs2 Team Play Card (Reference 2)
        TactileTeamCard(
            onClick = { onNavigateToTournament() },
            modifier = Modifier.width(220.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Quick Bottom Action Row: Tournament Cup & Browse All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToTournament)
                    .padding(14.dp)
                    .testTag("home_tournament_cup_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tournament",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onBrowseAll)
                    .padding(14.dp)
                    .testTag("home_library_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = ColorPlayer2Blue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All Games (8)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TactilePlayerCard(
    number: String,
    numberColor: Color,
    label: String,
    pillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(26.dp)
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(outerShape)
            .background(CardNavyBackground)
            .border(2.dp, CardNavyBorder, outerShape)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 12.dp)
            .testTag("player_card_$number"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Horizontal Embossed Accent Bars
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Box(modifier = Modifier.size(20.dp, 4.dp).background(Color(0xFF383852), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.size(20.dp, 4.dp).background(Color(0xFF383852), RoundedCornerShape(2.dp)))
            }

            // Giant Bold Number
            Text(
                text = number,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = numberColor,
                lineHeight = 58.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Oval Button Pill
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(pillColor)
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun TactileTeamCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(26.dp)
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(outerShape)
            .background(CardNavyBackground)
            .border(2.dp, CardNavyBorder, outerShape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .testTag("team_card_2vs2"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 2 vs 2 Title with colored numbers
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "2",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorPlayer1Red
                )
                Text(
                    text = "vs",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "2",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorPlayer2Blue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Purple Pill Button
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(ColorTeamPurple)
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TEAM PLAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
