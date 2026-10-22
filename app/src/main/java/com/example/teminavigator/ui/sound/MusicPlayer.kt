package com.example.teminavigator.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.example.teminavigator.R
import com.example.teminavigator.ui.langs.AppLanguage

class MusicPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun play(language: AppLanguage, loop: Boolean = true) {
        stop()
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

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        player?.takeIf { !it.isPlaying }?.start()
    }

    fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    fun stop() {
        player?.release()
        player = null
    }

    private fun trackFor(language: AppLanguage): Int = when (language) {
        AppLanguage.GERMAN,
        AppLanguage.ENGLISH,
        AppLanguage.PIRATE,
        AppLanguage.NADSAT,
        AppLanguage.RUSSIAN -> R.raw.song_1
    }
}