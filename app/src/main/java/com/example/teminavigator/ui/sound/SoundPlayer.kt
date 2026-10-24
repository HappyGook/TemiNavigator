package com.example.teminavigator.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.teminavigator.R

/**
 * Sound player: plays short sounds (start, arrival, error) via a [SoundPool]
 *
 * [release] can be called when the player is no longer needed to free the SoundPool resources
 *
 * @param context Context used to load the sound resources.
 */
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

    // Preloaded sound effects
    private val sampleId = soundPool.load(context, R.raw.sample_sound, 1)
    private val startId = soundPool.load(context, R.raw.start_sound, 1)
    private val arrivedId = soundPool.load(context, R.raw.arrived_sound, 1)
    private val errorId = soundPool.load(context, R.raw.error_sound, 1)

    // Loading sounds is asynchronous; this flag is set once the sample is ready
    private var sampleLoaded = false

    // Load sounds
    private val pendingSounds = mutableSetOf(sampleId, startId, arrivedId, errorId)
    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == 0) pendingSounds.remove(soundId)
            else Log.w("SoundPlayer", "Failed to load sound $soundId: $status")
        }
    }

    /** Plays the navigation start sound (when a route begins).*/
    fun playStart() {
        play(startId)
    }

    /** Plays the arrival sound (when the robot reaches its destination). */
    fun playArrival() {
        play(arrivedId)
    }

    /** Plays the error sound (e.g. failed navigation). */
    fun playError() {
        play(errorId)
    }

    /**
     * Plays the given preloaded sound once at full volume
     *
     * @param soundId ID of a sound loaded into the [soundPool].
     */
    private fun play(soundId: Int) {
        if (sampleLoaded) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    /** Releases underlying [SoundPool]; the player cannot not be used afterward. */
    fun release() = soundPool.release()
}