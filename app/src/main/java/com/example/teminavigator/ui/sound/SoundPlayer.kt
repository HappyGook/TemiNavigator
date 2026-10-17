package com.example.teminavigator.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.teminavigator.R

class SoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleId = soundPool.load(context, R.raw.sample_sound, 1)
    // todo: add other sounds
    private val startId = soundPool.load(context, R.raw.start_sound, 1)
    private val arrivedId = soundPool.load(context, R.raw.arrived_sound, 1)
    private val errorId = soundPool.load(context, R.raw.error_sound, 1)

    private var sampleLoaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (soundId == sampleId && status == 0) sampleLoaded = true
        }
    }

    fun playStart() {
        play(startId)
    }

    fun playArrival() {
        play(arrivedId)
    }

    fun playError() {
        play(errorId)
    }

    private fun play(soundId: Int) {
        if (sampleLoaded) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = soundPool.release()
}