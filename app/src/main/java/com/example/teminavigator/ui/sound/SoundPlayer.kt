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
    private var sampleLoaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (soundId == sampleId && status == 0) sampleLoaded = true
        }
    }

    fun playSample() {
        if (sampleLoaded) soundPool.play(sampleId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = soundPool.release()
}