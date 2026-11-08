package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .testTag("pause_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Game Paused",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resume_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Resume")
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("restart_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Restart Game")
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("exit_game_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Exit to Menu")
            }
        }
    }
}
