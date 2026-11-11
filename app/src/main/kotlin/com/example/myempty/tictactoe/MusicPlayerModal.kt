package com.example.myempty.tictactoe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MusicControlBar(
    themeColors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showModal by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mute / Unmute Button
        IconButton(
            onClick = { SoundManager.toggleBgm(context) },
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = if (SoundManager.isMusicPlaying) "🔊" else "🔇",
                fontSize = 18.sp
            )
        }

        // Playlist / Music Settings Modal Button
        IconButton(
            onClick = { showModal = true },
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = "🎵",
                fontSize = 18.sp
            )
        }
    }

    if (showModal) {
        MusicPlayerModal(
            themeColors = themeColors,
            onDismiss = { showModal = false }
        )
    }
}

@Composable
fun MusicPlayerModal(
    themeColors: CustomThemeColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tracks = remember { SoundManager.getTrackList() }
    var isPlaying by remember { mutableStateOf(SoundManager.isMusicPlaying) }
    var currentTrack by remember { mutableStateOf(SoundManager.currentTrackIndex) }
    var bgPlayEnabled by remember { mutableStateOf(SoundManager.playInBackground) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF14151C))
                .border(1.5.dp, Color(0xFF262936), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Music Player",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E202B))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color(0xFF9094A6), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Player Controls Bar (Cyan & Pink themed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0C0D12))
                    .border(1.dp, Color(0xFF202330), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = {
                        SoundManager.previousTrack(context)
                        currentTrack = SoundManager.currentTrackIndex
                        isPlaying = SoundManager.isMusicPlaying
                    }
                ) {
                    Text("⏮", fontSize = 22.sp)
                }

                // Play / Pause Neon Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF00E5FF) else Color(0xFFFF4081))
                        .clickable {
                            SoundManager.toggleBgm(context)
                            isPlaying = SoundManager.isMusicPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                }

                // Next
                IconButton(
                    onClick = {
                        SoundManager.nextTrack(context)
                        currentTrack = SoundManager.currentTrackIndex
                        isPlaying = SoundManager.isMusicPlaying
                    }
                ) {
                    Text("⏭", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Background Play Toggle Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C0D12))
                    .border(1.dp, Color(0xFF202330), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Play in Background",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Keep music playing when app is minimized",
                        color = Color(0xFF757A8E),
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = bgPlayEnabled,
                    onCheckedChange = {
                        bgPlayEnabled = it
                        SoundManager.playInBackground = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E5FF),
                        uncheckedThumbColor = Color(0xFF9094A6),
                        uncheckedTrackColor = Color(0xFF1E202B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PLAYLIST",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF757A8E),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Playlist tracks
            Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
            tracks.forEachIndexed { index, trackName ->
                val isCurrent = (index == currentTrack)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            SoundManager.selectTrack(context, index)
                            currentTrack = index
                            isPlaying = true
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCurrent && isPlaying) "🎵" else "${index + 1}.",
                            fontSize = 13.sp,
                            color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF757A8E)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = trackName,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCurrent) Color.White else Color(0xFFC0C4D4)
                        )
                    }

                    if (isCurrent && isPlaying) {
                        Text(
                            text = "Playing",
                            fontSize = 11.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

                        }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202330))
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
