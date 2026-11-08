package com.example.engine

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.storage.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundAndHaptics(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playCountdownBeep(isGo: Boolean) {
        if (!prefs.soundEnabled) return
        try {
            if (isGo) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 180)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_MED_L, 90)
            }
        } catch (_: Exception) {}
        vibrateSmall()
    }

    fun playTap() {
        if (!prefs.soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (_: Exception) {}
        vibrateSmall()
    }

    fun playHit() {
        if (!prefs.soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 50)
        } catch (_: Exception) {}
        vibrateMedium()
    }

    fun playScore() {
        if (!prefs.soundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (_: Exception) {}
        }
        vibrateSmall()
    }

    fun playVictory() {
        if (!prefs.soundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                kotlinx.coroutines.delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            } catch (_: Exception) {}
        }
        vibratePattern()
    }

    private fun vibrateSmall() {
        if (!prefs.hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateMedium() {
        if (!prefs.hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern() {
        if (!prefs.hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 80, 50),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
