package com.kletaq.app.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.kletaq.app.R
import com.kletaq.app.data.repository.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffectType {
    XP_GAIN,
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    TIMER_COMPLETE,
    BUTTON_CLICK,
    STREAK_FREEZE
}

@Singleton
class SoundEffectsManager @Inject constructor(
    private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundEffectType, Int>()
    private var isLoaded = false

    fun initialize() {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isLoaded = true
            }
        }

        // Load generated raw audio assets into SoundPool
        loadSound(SoundEffectType.XP_GAIN, R.raw.sound_xp_gain)
        loadSound(SoundEffectType.LEVEL_UP, R.raw.sound_level_up)
        loadSound(SoundEffectType.ACHIEVEMENT_UNLOCKED, R.raw.sound_achievement_unlocked)
        loadSound(SoundEffectType.TIMER_COMPLETE, R.raw.sound_timer_complete)
        loadSound(SoundEffectType.BUTTON_CLICK, R.raw.sound_button_click)
        loadSound(SoundEffectType.STREAK_FREEZE, R.raw.sound_streak_freeze)
    }

    private fun loadSound(type: SoundEffectType, resId: Int) {
        try {
            val id = soundPool?.load(context, resId, 1) ?: return
            if (id != 0) {
                soundMap[type] = id
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundEffectsManager", "Error loading sound for $type: ${e.message}")
        }
    }

    fun playSound(effect: SoundEffectType) {
        val soundEnabled = UserSettingsRepository.userSettingsState.value.soundEffectsEnabled
        if (!soundEnabled) return

        val soundId = soundMap[effect] ?: return
        soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isLoaded = false
    }
}
