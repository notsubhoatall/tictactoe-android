package com.example.myempty.tictactoe

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class AppThemeMode {
    DEFAULT, LIGHT, DARK, AMOLED
}

enum class Screen {
    MENU,
    GAME,
    NEARBY_LOBBY,
    ONLINE
}

data class CustomThemeColors(
    val bg: Color,
    val surface: Color,
    val cardBg: Color = surface,
    val cellBg: Color = surface,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentX: Color,
    val accentO: Color,
    val isLight: Boolean
)

@Composable
fun getAppColors(themeMode: AppThemeMode): CustomThemeColors {
    return when (themeMode) {
        AppThemeMode.DEFAULT -> CustomThemeColors(
            bg = Color(0xFF0C0D12),
            surface = Color(0xFF141620),
            surfaceBorder = Color(0xFF222536),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF8A90A6),
            accentX = Color(0xFF00E5FF),
            accentO = Color(0xFFFF4081),
            isLight = false
        )
        AppThemeMode.LIGHT -> CustomThemeColors(
            bg = Color(0xFFF4F6FA),
            surface = Color(0xFFFFFFFF),
            surfaceBorder = Color(0xFFDFE3EC),
            textPrimary = Color(0xFF12141C),
            textSecondary = Color(0xFF707688),
            accentX = Color(0xFF0070F3),
            accentO = Color(0xFFE53935),
            isLight = true
        )
        AppThemeMode.DARK -> CustomThemeColors(
            bg = Color(0xFF101116),
            surface = Color(0xFF191B24),
            surfaceBorder = Color(0xFF2B2E3D),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF888E9E),
            accentX = Color(0xFF5390F5),
            accentO = Color(0xFFFF5252),
            isLight = false
        )
        AppThemeMode.AMOLED -> CustomThemeColors(
            bg = Color(0xFF000000),
            surface = Color(0xFF0E0E10),
            surfaceBorder = Color(0xFF242428),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF808080),
            accentX = Color(0xFF00E5FF),
            accentO = Color(0xFFFF4081),
            isLight = false
        )
    }
}

data class UpdateInfo(
    val isAvailable: Boolean = false,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

object GitHubUpdateChecker {
    private const val GITHUB_REPO = "notsubhoatall/Tic-Tac-Toe"

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val currentVersion = packageInfo.versionName ?: "1.0"

            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")
                val notes = json.optString("body", "A new update is available on GitHub!")

                if (tagName.isNotEmpty() && isNewerVersion(currentVersion, tagName)) {
                    return@withContext UpdateInfo(
                        isAvailable = true,
                        latestVersion = tagName,
                        releaseNotes = notes,
                        downloadUrl = htmlUrl
                    )
                }
            }
        } catch (_: Exception) {}
        UpdateInfo(isAvailable = false)
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currParts.size, latestParts.size)

        for (i in 0 until maxLen) {
            val currNum = currParts.getOrElse(i) { 0 }
            val latestNum = latestParts.getOrElse(i) { 0 }
            if (latestNum > currNum) return true
            if (latestNum < currNum) return false
        }
        return false
    }
}

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        if (SoundManager.isMusicPlaying) {
            SoundManager.resumeBgm(this)
        }
    }

    override fun onPause() {
        super.onPause()
        // Respect background music setting
        if (!SoundManager.playInBackground) {
            SoundManager.pauseBgm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE)
        val initialTheme = try { AppThemeMode.valueOf(prefs.getString("saved_theme", AppThemeMode.DEFAULT.name) ?: AppThemeMode.DEFAULT.name) } catch (_: Exception) { AppThemeMode.DEFAULT }

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.MENU) }
            var selectedMode by remember { mutableStateOf(GameMode.AI) }
            var selectedTheme by remember { mutableStateOf(initialTheme) }

            var myRole by remember { mutableStateOf("X") }
            var connectionStatusText by remember { mutableStateOf("Ready to connect") }
            var lastRemoteMove by remember { mutableStateOf<Int?>(null) }
            var remoteRestartTrigger by remember { mutableStateOf(0) }
            var nearbyManager by remember { mutableStateOf<NearbyConnectionManager?>(null) }

            val colors = getAppColors(selectedTheme)

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = colors.bg.toArgb()
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = colors.isLight
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
            ) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        Screen.MENU -> MenuScreen(
                            themeColors = colors,
                            currentTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it; prefs.edit().putString("saved_theme", it.name).apply() },
                            onStartGame = { mode ->
                                selectedMode = mode
                                if (mode == GameMode.NEARBY_P2P) {
                                    currentScreen = Screen.NEARBY_LOBBY
                                } else {
                                    currentScreen = Screen.GAME
                                }
                            },
                            onStartOnline = {
                                currentScreen = Screen.ONLINE
                            }
                        )
                        Screen.NEARBY_LOBBY -> NearbyLobbyScreen(
                            themeColors = colors,
                            statusText = connectionStatusText,
                            onHost = { mgr ->
                                nearbyManager = mgr
                                mgr.startAdvertising()
                            },
                            onJoin = { mgr ->
                                nearbyManager = mgr
                                mgr.startDiscovery()
                            },
                            onBack = {
                                nearbyManager?.stop()
                                currentScreen = Screen.MENU
                            },
                            onConnected = { isHost ->
                                myRole = if (isHost) "X" else "O"
                                lastRemoteMove = null
                                currentScreen = Screen.GAME
                            },
                            onMoveReceived = { move ->
                                lastRemoteMove = move
                            },
                            onRestartReceived = {
                                lastRemoteMove = null
                                remoteRestartTrigger++
                            },
                            onStatusUpdate = { status ->
                                connectionStatusText = status
                            }
                        )
                        Screen.GAME -> GameScreen(
                            gameMode = selectedMode,
                            myRole = myRole,
                            nearbyManager = nearbyManager,
                            remoteMove = lastRemoteMove,
                            remoteRestartSignal = remoteRestartTrigger,
                            onMoveProcessed = { lastRemoteMove = null },
                            themeColors = colors,
                            onBackToMenu = {
                                nearbyManager?.stop()
                                lastRemoteMove = null
                                currentScreen = Screen.MENU
                            }
                        )
                        Screen.ONLINE -> OnlineGameScreen(
                            onBackToLocal = {
                                currentScreen = Screen.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScreen(
    themeColors: CustomThemeColors,
    currentTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onStartGame: (GameMode) -> Unit,
    onStartOnline: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var updateInfo by remember { mutableStateOf(UpdateInfo()) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = GitHubUpdateChecker.checkForUpdates(context)
        if (result.isAvailable) {
            updateInfo = result
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateInfo.isAvailable) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text("Update Available", fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "v${updateInfo.latestVersion} is now available.",
                        color = themeColors.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        updateInfo.releaseNotes.take(180),
                        fontSize = 12.sp,
                        color = themeColors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                        uriHandler.openUri(updateInfo.downloadUrl)
                    }
                ) {
                    Text("Download", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later", color = themeColors.textSecondary)
                }
            },
            containerColor = Color(0xFF141620)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIC TAC TOE",
                letterSpacing = 2.sp,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (updateInfo.isAvailable) {
                    Text(
                        text = "NEW UPDATE 🚀",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier
                            .clickable { showUpdateDialog = true }
                            .padding(end = 12.dp)
                    )
                }
                MusicControlBar(themeColors = themeColors)
            }
        }

        // Vibrant Neon Hero Title (Image 20 style)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Row {
                Text(
                    text = "TIC ",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "TAC ",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFFFF4081)
                )
                Text(
                    text = "TOE",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
            }
            Text(
                text = "Modern Classic",
                fontSize = 14.sp,
                color = Color(0xFF8A90A6),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Colored Border Pill Menu (Image 20 style)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Single Player (Cyan Border)
            ColoredMenuPill(
                title = "VS AI (Single Player)",
                borderColor = Color(0xFF00E5FF),
                textColor = Color(0xFF00E5FF),
                bgColor = Color(0xFF00E5FF).copy(alpha = 0.04f),
                onClick = { onStartGame(GameMode.AI) }
            )

            // 2 Players (Pink/Magenta Border)
            ColoredMenuPill(
                title = "2 Players (Pass & Play)",
                borderColor = Color(0xFFFF4081),
                textColor = Color(0xFFFF4081),
                bgColor = Color(0xFFFF4081).copy(alpha = 0.04f),
                onClick = { onStartGame(GameMode.TWO_PLAYER) }
            )

            // Nearby / Hotspot Play (Emerald Green Border)
            ColoredMenuPill(
                title = "Nearby / Hotspot Play",
                borderColor = Color(0xFF00E676),
                textColor = Color(0xFF00E676),
                bgColor = Color(0xFF00E676).copy(alpha = 0.04f),
                onClick = { onStartGame(GameMode.NEARBY_P2P) }
            )

            // Online Multiplayer (Vibrant Orange Pill)
            ColoredMenuPill(
                title = "Online Multiplayer (Cloud) 🌐",
                borderColor = Color(0xFFFF9100),
                textColor = Color.White,
                bgColor = Color(0xFFFF9100),
                solid = true,
                onClick = onStartOnline
            )
        }

        // Theme Selector & Footer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Theme",
                fontSize = 12.sp,
                color = themeColors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColors.surface)
                    .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppThemeMode.values().forEach { mode ->
                    val isSelected = currentTheme == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.20f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onThemeSelected(mode) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color(0xFF00E5FF) else themeColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Crafted with Jetpack Compose",
                fontSize = 12.sp,
                color = themeColors.textSecondary
            )
            Text(
                text = "Built by notsubhoatall ↗",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                modifier = Modifier
                    .clickable { uriHandler.openUri("https://github.com/notsubhoatall") }
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ColoredMenuPill(
    title: String,
    borderColor: Color,
    textColor: Color,
    bgColor: Color,
    solid: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bgColor)
            .border(
                width = if (solid) 0.dp else 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameScreen(
    gameMode: GameMode,
    myRole: String = "X",
    nearbyManager: NearbyConnectionManager? = null,
    remoteMove: Int? = null,
    remoteRestartSignal: Int = 0,
    onMoveProcessed: () -> Unit = {},
    themeColors: CustomThemeColors,
    onBackToMenu: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { GameEngine() }
    val scope = rememberCoroutineScope()

    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var scoreX by remember { mutableIntStateOf(0) }
    var scoreO by remember { mutableIntStateOf(0) }
    var scoreTies by remember { mutableIntStateOf(0) }

    fun resetGame(notifyPeer: Boolean = true) {
        SoundManager.playMoveSound()
        board = List(9) { "" }
        currentPlayer = "X"
        winner = null
        isThinking = false
        onMoveProcessed()

        if (gameMode == GameMode.NEARBY_P2P && notifyPeer) {
            nearbyManager?.sendRestart()
        }
    }

    LaunchedEffect(remoteRestartSignal) {
        if (remoteRestartSignal > 0) {
            resetGame(notifyPeer = false)
        }
    }

    LaunchedEffect(remoteMove) {
        remoteMove?.let { index ->
            if (winner == null && board[index].isEmpty()) {
                SoundManager.playMoveSound()
                val newBoard = board.toMutableList()
                newBoard[index] = currentPlayer
                board = newBoard

                val outcome = engine.checkWinner(board)
                if (outcome != null) {
                    winner = outcome
                    if (winner == "Draw") { scoreTies++; SoundManager.playDrawSound() } else if (winner == "X") { scoreX++; SoundManager.playWinSound() } else { scoreO++; SoundManager.playWinSound() }
                } else {
                    currentPlayer = if (currentPlayer == "X") "O" else "X"
                }
            }
            onMoveProcessed()
        }
    }

    fun handleCellClick(index: Int) {
        if (board[index].isNotEmpty() || winner != null || isThinking) return
        if (gameMode == GameMode.NEARBY_P2P && currentPlayer != myRole) return

        SoundManager.playMoveSound()
        val newBoard = board.toMutableList()
        newBoard[index] = currentPlayer
        board = newBoard

        if (gameMode == GameMode.NEARBY_P2P) {
            nearbyManager?.sendMove(index)
        }

        val outcome = engine.checkWinner(board)
        if (outcome != null) {
            winner = outcome
            if (winner == "Draw") { scoreTies++; SoundManager.playDrawSound() } else if (winner == "X") { scoreX++; SoundManager.playWinSound() } else { scoreO++; SoundManager.playWinSound() }
            return
        }

        if (gameMode == GameMode.AI && currentPlayer == "X") {
            currentPlayer = "O"
            isThinking = true

            scope.launch {
                delay(300)
                val bestMove = engine.getBestMove(board)
                if (bestMove != -1) {
                    val aiBoard = board.toMutableList()
                    aiBoard[bestMove] = "O"
                    board = aiBoard
                    SoundManager.playMoveSound()

                    val aiOutcome = engine.checkWinner(board)
                    if (aiOutcome != null) {
                        winner = aiOutcome
                        if (winner == "Draw") { scoreTies++; SoundManager.playDrawSound() } else if (winner == "X") { scoreX++; SoundManager.playWinSound() } else { scoreO++; SoundManager.playWinSound() }
                    } else {
                        currentPlayer = "X"
                    }
                }
                isThinking = false
            }
        } else {
            currentPlayer = if (currentPlayer == "X") "O" else "X"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackToMenu)
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            )

            Text(
                text = when (gameMode) {
                    GameMode.AI -> "VS AI"
                    GameMode.TWO_PLAYER -> "2 PLAYERS"
                    GameMode.NEARBY_P2P -> "NEARBY ($myRole)"
                    else -> "MATCH"
                },
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )

            MusicControlBar(themeColors = themeColors)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF141620)).border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (gameMode == GameMode.AI) "YOU (X)" else "X", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF00E5FF))
                    Text(text = "$scoreX", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF141620)).border(1.5.dp, Color(0xFF2E3244), RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF8A90A6))
                    Text(text = "$scoreTies", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF141620)).border(1.5.dp, Color(0xFFFF4081).copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (gameMode == GameMode.AI) "AI (O)" else "O", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFFFF4081))
                    Text(text = "$scoreO", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
        // Status Turn / Winner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            val statusText = when {
                winner == "Draw" -> "Draw Game!"
                winner != null -> "$winner Wins!"
                isThinking -> "AI Thinking..."
                gameMode == GameMode.NEARBY_P2P && currentPlayer == myRole -> "Your Turn ($myRole)"
                gameMode == GameMode.NEARBY_P2P -> "Opponent's Turn"
                else -> "Player $currentPlayer's Turn"
            }

            Text(
                text = statusText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    winner == "X" || (winner == null && currentPlayer == "X") -> Color(0xFF00E5FF)
                    winner == "O" || (winner == null && currentPlayer == "O") -> Color(0xFFFF4081)
                    else -> Color.White
                }
            )
        }

        // 3x3 Grid
        Box(
            modifier = Modifier
                .size(330.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(themeColors.surface)
                .border(1.5.dp, themeColors.surfaceBorder, RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cellValue = board[index]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E202B))
                                    .border(
                                        1.dp,
                                        if (cellValue.isNotEmpty()) {
                                            if (cellValue == "X") Color(0xFF00E5FF).copy(alpha = 0.5f)
                                            else Color(0xFFFF4081).copy(alpha = 0.5f)
                                        } else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { handleCellClick(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellValue,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (cellValue == "X") Color(0xFF00E5FF) else Color(0xFFFF4081)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToMenu) {
                Text("🏠 Home", fontSize = 15.sp, color = Color.White)
            }

            Button(
                onClick = { resetGame(notifyPeer = true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202330)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF323646))
            ) {
                Text("Restart Game", color = Color.White, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = { SoundManager.toggleBgm(context) }) {
                Text(if (SoundManager.isMusicPlaying) "🔊" else "🔇", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun NearbyLobbyScreen(
    themeColors: CustomThemeColors,
    statusText: String,
    onHost: (NearbyConnectionManager) -> Unit,
    onJoin: (NearbyConnectionManager) -> Unit,
    onBack: () -> Unit,
    onConnected: (Boolean) -> Unit,
    onMoveReceived: (Int) -> Unit,
    onRestartReceived: () -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = perms.values.all { it }
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) launcher.launch(requiredPermissions) else hasPermissions = true
    }

    fun getManager(): NearbyConnectionManager {
        return NearbyConnectionManager(
            context = context,
            onMoveReceived = onMoveReceived,
            onRestartReceived = onRestartReceived,
            onConnected = onConnected,
            onDisconnected = { onStatusUpdate("Disconnected") },
            onStatusUpdate = onStatusUpdate
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "← Back",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(themeColors.surface)
                .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nearby / Hotspot Play",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = Color(0xFF00E676),
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF00E676))
                        .clickable { if (hasPermissions) onHost(getManager()) else launcher.launch(requiredPermissions) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Host Game", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E202B))
                        .border(1.dp, Color(0xFF00E676), RoundedCornerShape(14.dp))
                        .clickable { if (hasPermissions) onJoin(getManager()) else launcher.launch(requiredPermissions) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Join Game", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Text(
            text = "Connect via Bluetooth, shared Wi-Fi, or Personal Hotspot.",
            fontSize = 12.sp,
            color = themeColors.textSecondary
        )
    }
}
