package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameCategory
import com.example.data.model.GameDefinition
import com.example.data.model.GameRegistry
import com.example.data.storage.AppDatabase
import com.example.ui.components.GameTileCard
import com.example.ui.theme.ColorPlayer1Red
import com.example.ui.theme.ColorPlayer2Blue
import com.example.ui.theme.ColorPlayer3Green
import com.example.ui.theme.ColorPlayer4Yellow
import kotlinx.coroutines.launch

@Composable
fun GameLibraryScreen(
    initialPlayerFilter: Int? = null,
    onSelectGame: (GameDefinition) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val allStats by database.statsDao().getAllStats().collectAsState(initial = emptyList())
    val favoritesMap = remember(allStats) { allStats.associate { it.gameId to it.isFavorite } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }
    var selectedPlayerFilter by remember { mutableIntStateOf(initialPlayerFilter ?: 0) } // 0 = All

    val filteredGames = remember(searchQuery, selectedCategory, selectedPlayerFilter) {
        GameRegistry.games.filter { game ->
            val matchesSearch = game.title.contains(searchQuery, ignoreCase = true) ||
                    game.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || game.category == selectedCategory
            val matchesPlayer = selectedPlayerFilter == 0 ||
                    (selectedPlayerFilter >= game.minPlayers && selectedPlayerFilter <= game.maxPlayers)
            matchesSearch && matchesCategory && matchesPlayer
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("game_library_screen")
    ) {
        // Header Section
        item(span = { GridItemSpan(3) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header with 1234 banner (Reference 1 style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Collection",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1", fontWeight = FontWeight.Black, color = ColorPlayer1Red, fontSize = 16.sp)
                        Text("2", fontWeight = FontWeight.Black, color = ColorPlayer2Blue, fontSize = 16.sp)
                        Text("3", fontWeight = FontWeight.Black, color = ColorPlayer3Green, fontSize = 16.sp)
                        Text("4", fontWeight = FontWeight.Black, color = ColorPlayer4Yellow, fontSize = 16.sp)
                        Text(
                            " PLAYER",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    placeholder = { Text("Search arcade games...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterPill(
                            label = "All",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(GameCategory.entries.toTypedArray()) { cat ->
                        FilterPill(
                            label = cat.displayName,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                // Player Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "1P", "2P", "3P", "4P").forEachIndexed { index, label ->
                        val isSelected = selectedPlayerFilter == index
                        val color = when (index) {
                            1 -> ColorPlayer1Red
                            2 -> ColorPlayer2Blue
                            3 -> ColorPlayer3Green
                            4 -> ColorPlayer4Yellow
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) color.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedPlayerFilter = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3-Column Games Grid (Reference 1)
        items(filteredGames) { game ->
            val isFav = favoritesMap[game.id] == true
            GameTileCard(
                game = game,
                isFavorite = isFav,
                onFavoriteToggle = {
                    scope.launch {
                        database.statsDao().updateFavorite(game.id, !isFav)
                    }
                },
                onClick = { onSelectGame(game) }
            )
        }

        item(span = { GridItemSpan(3) }) {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
