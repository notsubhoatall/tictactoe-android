package com.example.myempty.tictactoe

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator

object SoundManager {
    private var toneGenerator: ToneGenerator? = null
    private var mediaPlayer: MediaPlayer? = null
    var isMusicPlaying = false
        private set

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playMoveSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
    }

    fun playWinSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
    }

    fun playDrawSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
    }

    fun startBgm(context: Context) {
        if (mediaPlayer == null) {
            // Place your audio file at app/src/main/res/raw/bgm.mp3
            val resId = context.resources.getIdentifier("bgm", "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId).apply {
                    isLooping = true
                    setVolume(0.35f, 0.35f)
                    start()
                }
                isMusicPlaying = true
            }
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            isMusicPlaying = true
        }
    }

    fun toggleBgm(context: Context) {
        if (isMusicPlaying) {
            pauseBgm()
        } else {
            startBgm(context)
        }
    }

    fun pauseBgm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
        isMusicPlaying = false
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isMusicPlaying = false
    }
}
