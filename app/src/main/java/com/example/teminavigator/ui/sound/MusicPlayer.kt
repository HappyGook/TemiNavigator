package com.example.teminavigator.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.example.teminavigator.R
import com.example.teminavigator.ui.langs.AppLanguage

/**
 * Music player: allows the playing of music
 *
 * Uses Android's [MediaPlayer] to play the music in the background of the app.
 * Keeps a single instance at a time.
 * Plays music depending on set language.
 *
 * Usage: Created once in NavigationViewModel, controlled by the music toggle in the settings.
 * Call functions to start, pause, resume or stop the music streams.
 *
 * @param context Context used to create the MediaPlayer.
 */
class MusicPlayer(private val context: Context) {

    // Current player instance, null when nothing is loaded/prepared
    private var player: MediaPlayer? = null

    // Media playback attributes
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /**
     * Starts playing the music track
     *
     * @param language  App language selecting the track to play
     * @param loop      Whether to loop the track, default `true`
     */
    fun play(language: AppLanguage, loop: Boolean = true) {
        stop()  // Stop any previous playback before starting new track
        player = MediaPlayer.create(
            context,
            trackFor(language),
            attributes,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )?.apply {
            isLooping = loop
            start()
        }
    }

    /**
     * Pauses playback if a track is currently playing
     */
    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    /**
     * Resumes playback if a track is loaded but paused
     * (no effect if a track is already playing or nothing loaded)
     */
    fun resume() {
        player?.takeIf { !it.isPlaying }?.start()
    }

    /**
     * Sets the playback volume (0.0 silent .. 1.0 full)
     * @param volume volume applied
     */
    fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    /**
     * Stops playback and releases [MediaPlayer]
     */
    fun stop() {
        player?.release()
        player = null
    }

    /**
     * Returns raw resource for the given app language
     */
    private fun trackFor(language: AppLanguage): Int = when (language) {
        AppLanguage.GERMAN -> R.raw.special_theme
        AppLanguage.ENGLISH -> R.raw.special_theme
        AppLanguage.PIRATE -> R.raw.pirate_theme
        AppLanguage.NADSAT -> R.raw.nadsat_theme
        AppLanguage.RUSSIAN -> R.raw.quick_guiding_theme
    }
}