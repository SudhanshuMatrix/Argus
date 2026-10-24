package com.ajayinnovations.argus.alert

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.SoundPolicy
import com.ajayinnovations.argus.core.VoiceClips
import com.ajayinnovations.argus.settings.Settings

/**
 * Plays the bundled ElevenLabs clip for a level, in the parent's language and title, falling
 * back to the neutral clip. Fully offline. Respects silent mode unless the "Speak warnings even
 * on silent" setting is on. All player work runs on the main thread; nothing here throws.
 */
class VoicePlayer(
    private val context: Context,
    private val settings: Settings,
    private val sound: PhoneSound,
) {
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    /** [userAsked]: "Hear it again" plays even on silent. */
    fun play(level: Level, userAsked: Boolean = false) {
        if (!SoundPolicy.shouldSpeak(sound.ringer(), settings.speakOnSilent, userAsked)) return
        val names = VoiceClips.candidates(level, settings.language, settings.parentTitle)
        if (names.isEmpty()) return
        main.post { start(names) }
    }

    fun stop() {
        main.post { release() }
    }

    private fun start(names: List<String>) {
        release()
        for (name in names) {
            val id = rawId(name)
            if (id == 0) continue
            val created = try {
                // Alarm usage: heard even when media volume is down, and it doesn't stop her music for good
                MediaPlayer.create(context, id, ATTRIBUTES, audioSessionId())
            } catch (t: Throwable) {
                null
            }
            if (created == null) continue
            try {
                created.setOnCompletionListener { if (player === it) release() else it.release() }
                created.setOnErrorListener { mp, _, _ -> if (player === mp) release() else mp.release(); true }
                player = created
                created.start()
                return
            } catch (t: Throwable) {
                release()
            }
        }
        Log.w(TAG, "No playable clip (${names.size} tried)")
    }

    private fun release() {
        val current = player ?: return
        player = null
        try {
            current.release()
        } catch (t: Throwable) {
            // Already released
        }
    }

    // Looked up by name so a missing title clip falls back to neutral; VoiceClipsTest checks every bundled one exists
    @SuppressLint("DiscouragedApi")
    private fun rawId(name: String): Int = context.resources.getIdentifier(name, "raw", context.packageName)

    private fun audioSessionId(): Int =
        context.getSystemService(AudioManager::class.java)?.generateAudioSessionId() ?: AudioManager.AUDIO_SESSION_ID_GENERATE

    private companion object {
        const val TAG = "VoicePlayer"
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
