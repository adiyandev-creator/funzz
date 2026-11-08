package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.storage.PreferencesManager
import com.example.navigation.AppNavHost
import com.example.ui.theme.PartyGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { PreferencesManager(this) }
            var themeMode by remember { mutableStateOf(prefs.themeMode) }

            val isDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            PartyGridTheme(darkTheme = isDarkTheme) {
                AppNavHost(
                    onThemeChanged = {
                        themeMode = prefs.themeMode
                    }
                )
            }
        }
    }
}
