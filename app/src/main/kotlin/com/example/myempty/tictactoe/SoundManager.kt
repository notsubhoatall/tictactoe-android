package com.example.myempty.tictactoe

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream

object SoundManager {
    private const val TAG = "SoundManager"
    private var bgmPlayer: MediaPlayer? = null
    private var toneGen: ToneGenerator? = null

    var isMusicPlaying by mutableStateOf(false)
        private set
    var playInBackground by mutableStateOf(false)
    var currentTrackIndex by mutableIntStateOf(0)
        private set

    // All 12 tracks (original 4 + 8 new additions)
    private val playlist = listOf(
        "Arcade Victory Lap" to "bgm/arcade_victory_lap.mp3",
        "Bouncing Past The Bastion" to "bgm/bouncing_past_the_bastion.mp3",
        "High Gear Pursuit" to "bgm/high_gear_pursuit.mp3",
        "Quest Complete" to "bgm/quest_complete.mp3",
        "Beyond The Last Gate" to "bgm/beyond_the_last_gate.mp3",
        "Final Boss Rush" to "bgm/final_boss_rush.mp3",
        "Guava Summer Rush" to "bgm/guava_summer_rush.mp3",
        "Midday Pursuit" to "bgm/midday_pursuit.mp3",
        "Ritmo De Podio" to "bgm/ritmo_de_podio.mp3",
        "Slipstream Horizon" to "bgm/slipstream_horizon.mp3",
        "The Final Lap" to "bgm/the_final_lap.mp3",
        "Velocity Surge" to "bgm/velocity_surge.mp3"
    )

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {}
    }

    fun getTrackList(): List<String> = playlist.map { it.first }

    fun startBgm(context: Context, trackIndex: Int = currentTrackIndex) {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = null

            currentTrackIndex = trackIndex.coerceIn(0, playlist.size - 1)
            val assetPath = playlist[currentTrackIndex].second

            val cacheFile = File(context.cacheDir, "current_bgm.mp3")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                setDataSource(cacheFile.absolutePath)
                isLooping = true
                prepare()
                start()
            }
            isMusicPlaying = true
            Log.d(TAG, "Playing successfully: $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "Playback error on $trackIndex: ${e.message}")
            isMusicPlaying = false
        }
    }

    fun pauseBgm() {
        if (!playInBackground) {
            try {
                bgmPlayer?.pause()
            } catch (_: Exception) {}
            isMusicPlaying = false
        }
    }

    fun resumeBgm(context: Context) {
        if (bgmPlayer != null && !bgmPlayer!!.isPlaying) {
            try {
                bgmPlayer?.start()
                isMusicPlaying = true
            } catch (_: Exception) {
                startBgm(context, currentTrackIndex)
            }
        } else if (bgmPlayer == null) {
            startBgm(context, currentTrackIndex)
        }
    }

    fun toggleBgm(context: Context) {
        if (bgmPlayer != null && bgmPlayer!!.isPlaying) {
            try {
                bgmPlayer?.pause()
            } catch (_: Exception) {}
            isMusicPlaying = false
        } else {
            if (bgmPlayer != null) {
                try {
                    bgmPlayer?.start()
                    isMusicPlaying = true
                } catch (_: Exception) {
                    startBgm(context, currentTrackIndex)
                }
            } else {
                startBgm(context, currentTrackIndex)
            }
        }
    }

    fun nextTrack(context: Context) {
        val nextIdx = (currentTrackIndex + 1) % playlist.size
        startBgm(context, nextIdx)
    }

    fun previousTrack(context: Context) {
        val prevIdx = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        startBgm(context, prevIdx)
    }

    fun selectTrack(context: Context, index: Int) {
        startBgm(context, index)
    }

    fun playMoveSound() {
        try { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 30) } catch (_: Exception) {}
    }

    fun playWinSound() {
        try { toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150) } catch (_: Exception) {}
    }

    fun playDrawSound() {
        try { toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 90) } catch (_: Exception) {}
    }

    fun release() {
        try {
            bgmPlayer?.release()
            bgmPlayer = null
            toneGen?.release()
            toneGen = null
            isMusicPlaying = false
        } catch (_: Exception) {}
    }
}
