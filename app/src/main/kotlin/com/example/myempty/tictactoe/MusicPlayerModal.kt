package com.example.myempty.tictactoe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MusicControlBar(
    themeColors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundManager.initPlaylist(context)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mute / Unmute Button
        IconButton(onClick = { SoundManager.toggleBgm(context) }) {
            Text(if (SoundManager.isMusicPlaying) "🔊" else "🔇", fontSize = 18.sp)
        }

        // Playlist / Track Picker Button
        IconButton(onClick = { showPlaylistDialog = true }) {
            Text("🎵", fontSize = 18.sp)
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Close", color = themeColors.accentX)
                }
            },
            title = {
                Text(
                    text = "Music Player",
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Playback Controls (Prev, Toggle, Next)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { SoundManager.playPrevious(context) }) {
                            Text("⏮️", fontSize = 22.sp)
                        }
                        IconButton(onClick = { SoundManager.toggleBgm(context) }) {
                            Text(if (SoundManager.isMusicPlaying) "⏸️" else "▶️", fontSize = 24.sp)
                        }
                        IconButton(onClick = { SoundManager.playNext(context) }) {
                            Text("⏭️", fontSize = 22.sp)
                        }
                    }

                    HorizontalDivider(color = themeColors.textSecondary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (SoundManager.playlist.isEmpty()) {
                        Text(
                            text = "No songs found in assets/bgm/\nDrop .mp3 files there to listen!",
                            color = themeColors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            itemsIndexed(SoundManager.playlist) { index, track ->
                                val isCurrent = index == SoundManager.currentTrackIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isCurrent) themeColors.accentX.copy(alpha = 0.15f)
                                            else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            SoundManager.playTrack(context, index)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = track.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) themeColors.accentX else themeColors.textPrimary
                                    )
                                    if (isCurrent && SoundManager.isMusicPlaying) {
                                        Text("Playing", fontSize = 11.sp, color = themeColors.accentX)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = themeColors.cardBg
        )
    }
}
