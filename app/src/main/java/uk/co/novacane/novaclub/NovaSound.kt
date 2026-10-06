// Nova Club's sound: a quiet ambient track that loops while the app is open, and soft sound effects for taps,
// switches and bookings. Both can be turned off, and the choice is remembered. (The sounds are made by
// tools/make_sounds.py and live in res/raw.)
package com.novacane.novaclub

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import uk.co.novacane.novaclub.R

// Every sound effect, with the file it plays and how loud
enum class Sfx(val file: Int, val volume: Float) {
    TAP(R.raw.sfx_tap, 0.35f),                     // a soft glassy tap
    SWITCH(R.raw.sfx_switch, 0.4f),                // moving between tabs or areas
    STEP_UP(R.raw.sfx_step_up, 0.4f),              // one more hour
    STEP_DOWN(R.raw.sfx_step_down, 0.4f),          // one less hour
    TOGGLE_ON(R.raw.sfx_toggle_on, 0.4f),          // a switch turned on
    TOGGLE_OFF(R.raw.sfx_toggle_off, 0.4f),        // a switch turned off
    BOOKING_IN(R.raw.sfx_booking_in, 0.6f),        // a booking has just landed in the calendar
    BOOKING_OUT(R.raw.sfx_booking_out, 0.55f),     // a booking has just gone, freeing up time
    CONFIRM(R.raw.sfx_confirm, 0.5f),              // all good (e.g. the time is still free)
    CLASH(R.raw.sfx_clash, 0.5f)                   // not possible (e.g. someone's just taken that time)
}

class NovaSound(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("nova_sound", Context.MODE_PRIVATE)

    // Whether the ambient track and the sound effects are on (remembered between visits; both on to start with)
    var ambientOn by mutableStateOf(prefs.getBoolean("ambient", true))
        private set
    var effectsOn by mutableStateOf(prefs.getBoolean("effects", true))
        private set

    // ---------- SOUND EFFECTS ----------
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val loaded = Sfx.entries.associateWith { pool.load(app, it.file, 1) }   // load every effect up front

    fun play(sfx: Sfx) {
        if (!effectsOn) return
        loaded[sfx]?.let { pool.play(it, sfx.volume, sfx.volume, 1, 0, 1f) }
    }

    // ---------- AMBIENT ----------
    private val ambientVolume = 0.3f                                   // quiet: it sits underneath, never on top
    private var player: MediaPlayer? = null
    private var inForeground = false

    private fun startAmbient() {
        if (!ambientOn || !inForeground) return
        // Don't talk over someone's own music
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.isMusicActive && player?.isPlaying != true) return
        val p = player ?: MediaPlayer.create(app, R.raw.ambient_nova)?.apply {
            isLooping = true
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setVolume(ambientVolume, ambientVolume)
        }?.also { player = it }
        p?.start()
    }

    private fun pauseAmbient() { player?.takeIf { it.isPlaying }?.pause() }

    // Called when the app comes to the front and goes to the back, so the music only plays while you're in the app
    fun onForeground() { inForeground = true; startAmbient() }
    fun onBackground() { inForeground = false; pauseAmbient() }

    fun setAmbient(on: Boolean) {
        ambientOn = on
        prefs.edit().putBoolean("ambient", on).apply()
        if (on) startAmbient() else pauseAmbient()
    }

    fun setEffects(on: Boolean) {
        effectsOn = on
        prefs.edit().putBoolean("effects", on).apply()
        if (on) play(Sfx.TOGGLE_ON)
    }

    // Let go of the audio when the app closes
    fun release() {
        player?.release(); player = null
        pool.release()
    }
}

// The app's sound, handed to every screen (null in previews, where there's no sound)
val LocalNovaSound = staticCompositionLocalOf<NovaSound?> { null }
