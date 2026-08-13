package com.kletaq.app.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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

        // Load raw sound resource IDs when sound files are present in res/raw/
        // Example:
        // loadSound(SoundEffectType.XP_GAIN, R.raw.sound_xp_gain)
        // loadSound(SoundEffectType.LEVEL_UP, R.raw.sound_level_up)
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
    }
}
