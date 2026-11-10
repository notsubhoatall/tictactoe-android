package com.example.myempty.tictactoe

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class MusicTrack(
    val fileName: String,
    val title: String
)

object SoundManager {
    private var bgmPlayer: MediaPlayer? = null
    var isMusicPlaying by mutableStateOf(false)
        private set

    val playlist = mutableStateListOf<MusicTrack>()
    var currentTrackIndex by mutableIntStateOf(0)
        private set

    private var isInitialized = false

    fun initPlaylist(context: Context) {
        if (isInitialized) return
        try {
            val assetManager = context.assets
            val files = assetManager.list("bgm") ?: emptyArray()
            val musicFiles = files.filter { it.endsWith(".mp3") || it.endsWith(".ogg") || it.endsWith(".wav") }

            playlist.clear()
            musicFiles.forEach { file ->
                val displayName = file.substringBeforeLast(".")
                    .replace("_", " ")
                    .replace("-", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                playlist.add(MusicTrack(fileName = file, title = displayName))
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startBgm(context: Context, trackIndex: Int = currentTrackIndex) {
        initPlaylist(context)
        if (playlist.isEmpty()) return

        val targetIndex = trackIndex.coerceIn(0, playlist.size - 1)
        currentTrackIndex = targetIndex
        val track = playlist[targetIndex]

        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = MediaPlayer()

            val afd: AssetFileDescriptor = context.assets.openFd("bgm/${track.fileName}")
            bgmPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            bgmPlayer?.isLooping = false
            bgmPlayer?.setOnCompletionListener {
                playNext(context)
            }
            bgmPlayer?.prepare()
            bgmPlayer?.start()
            isMusicPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNext(context: Context) {
        if (playlist.isEmpty()) return
        val next = (currentTrackIndex + 1) % playlist.size
        startBgm(context, next)
    }

    fun playPrevious(context: Context) {
        if (playlist.isEmpty()) return
        val prev = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        startBgm(context, prev)
    }

    fun toggleBgm(context: Context) {
        if (isMusicPlaying) {
            pauseBgm()
        } else {
            if (bgmPlayer != null) {
                bgmPlayer?.start()
                isMusicPlaying = true
            } else {
                startBgm(context)
            }
        }
    }

    fun pauseBgm() {
        bgmPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        isMusicPlaying = false
    }

    fun playTrack(context: Context, index: Int) {
        startBgm(context, index)
    }

    fun release() {
        bgmPlayer?.release()
        bgmPlayer = null
        isMusicPlaying = false
    }

    // Keep existing SFX methods if your app uses them
    fun playMoveSound() {}
    fun playWinSound() {}
    fun playDrawSound() {}
}
