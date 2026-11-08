package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("partygrid_prefs", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var aiDifficulty: String
        get() = prefs.getString("ai_difficulty", "Normal") ?: "Normal"
        set(value) = prefs.edit().putString("ai_difficulty", value).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "System") ?: "System"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    fun resetPreferences() {
        prefs.edit().clear().apply()
    }
}
