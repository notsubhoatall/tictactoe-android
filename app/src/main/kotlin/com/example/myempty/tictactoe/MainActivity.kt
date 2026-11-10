package com.example.myempty.tictactoe

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppThemeMode {
    DEFAULT, LIGHT, DARK, AMOLED
}

data class CustomThemeColors(
    val bg: Color,
    val cardBg: Color,
    val cellBg: Color,
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
            bg = Color(0xFF121212),
            cardBg = Color(0xFF1E1E1E),
            cellBg = Color(0xFF282828),
            textPrimary = Color(0xFFEEEEEE),
            textSecondary = Color(0xFF9E9E9E),
            accentX = Color(0xFF00E5FF),
            accentO = Color(0xFFFF4081),
            isLight = false
        )
        AppThemeMode.LIGHT -> CustomThemeColors(
            bg = Color(0xFFF7F9FC),
            cardBg = Color(0xFFFFFFFF),
            cellBg = Color(0xFFEDF2F7),
            textPrimary = Color(0xFF1A202C),
            textSecondary = Color(0xFF718096),
            accentX = Color(0xFF0070F3),
            accentO = Color(0xFFE53E3E),
            isLight = true
        )
        AppThemeMode.DARK -> CustomThemeColors(
            bg = Color(0xFF1E2022),
            cardBg = Color(0xFF2B2D30),
            cellBg = Color(0xFF35383C),
            textPrimary = Color(0xFFE4E6EB),
            textSecondary = Color(0xFF9AA0A6),
            accentX = Color(0xFF64B5F6),
            accentO = Color(0xFFFF8A80),
            isLight = false
        )
        AppThemeMode.AMOLED -> CustomThemeColors(
            bg = Color(0xFF000000),
            cardBg = Color(0xFF111111),
            cellBg = Color(0xFF1A1A1A),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF757575),
            accentX = Color(0xFF00FFFF),
            accentO = Color(0xFFFF0055),
            isLight = false
        )
    }
}

enum class Screen {
    MENU, GAME, NEARBY_LOBBY
}

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        if (SoundManager.isMusicPlaying) {
            SoundManager.startBgm(this)
        }
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.MENU) }
            var selectedMode by remember { mutableStateOf(GameMode.AI) }
            var selectedTheme by remember { mutableStateOf(AppThemeMode.DEFAULT) }

            // P2P State
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

            Surface(modifier = Modifier.fillMaxSize(), color = colors.bg) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        Screen.MENU -> MenuScreen(
                            themeColors = colors,
                            currentTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it },
                            onStartGame = { mode ->
                                selectedMode = mode
                                if (mode == GameMode.NEARBY_P2P) {
                                    currentScreen = Screen.NEARBY_LOBBY
                                } else {
                                    currentScreen = Screen.GAME
                                }
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
    onStartGame: (GameMode) -> Unit
) {
    val context = LocalContext.current
    var isMusicOn by remember { mutableStateOf(SoundManager.isMusicPlaying) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                SoundManager.toggleBgm(context)
                isMusicOn = SoundManager.isMusicPlaying
            }) {
                Text(text = if (isMusicOn) "🔊" else "🔇", fontSize = 20.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text("TIC ", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = themeColors.accentX, fontFamily = FontFamily.Monospace)
                Text("TAC ", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = themeColors.accentO, fontFamily = FontFamily.Monospace)
                Text("TOE", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary, fontFamily = FontFamily.Monospace)
            }
            Text(
                text = "Modern Classic",
                fontSize = 15.sp,
                color = themeColors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onStartGame(GameMode.AI) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, themeColors.accentX)
            ) {
                Text("VS AI (Single Player)", color = themeColors.accentX, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onStartGame(GameMode.TWO_PLAYER) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, themeColors.accentO)
            ) {
                Text("2 Players (Pass & Play)", color = themeColors.accentO, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onStartGame(GameMode.NEARBY_P2P) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF00E676))
            ) {
                Text("Nearby / Hotspot Play", color = Color(0xFF00E676), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                "Theme",
                fontSize = 13.sp,
                color = themeColors.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppThemeMode.values().forEach { mode ->
                    val isSelected = currentTheme == mode
                    val chipBg = if (isSelected) themeColors.accentX.copy(alpha = 0.2f) else themeColors.cardBg

                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) themeColors.accentX else themeColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
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
        if (!allGranted) {
            launcher.launch(requiredPermissions)
        } else {
            hasPermissions = true
        }
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("< Back", fontSize = 16.sp, color = themeColors.textSecondary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nearby / Hotspot Multiplayer", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                statusText,
                fontSize = 14.sp,
                color = themeColors.accentX,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { if (hasPermissions) onHost(getManager()) else launcher.launch(requiredPermissions) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accentX)
            ) {
                Text("Host Game (Hotspot or BT)", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (hasPermissions) onJoin(getManager()) else launcher.launch(requiredPermissions) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accentO)
            ) {
                Text("Join Game", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "Tip: You can connect via Bluetooth, shared Wi-Fi, or by turning on Personal Hotspot on one phone and connecting with the other.",
            fontSize = 12.sp,
            color = themeColors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
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
    val engine = remember { GameEngine() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var isMusicOn by remember { mutableStateOf(SoundManager.isMusicPlaying) }

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

    // Peer requested restart
    LaunchedEffect(remoteRestartSignal) {
        if (remoteRestartSignal > 0) {
            resetGame(notifyPeer = false)
        }
    }

    // Remote move listener
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
                    if (winner == "Draw") SoundManager.playDrawSound() else SoundManager.playWinSound()
                } else {
                    currentPlayer = if (currentPlayer == "X") "O" else "X"
                }
            }
            onMoveProcessed() // Clear the move so it cannot re-fire
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
            if (winner == "Draw") SoundManager.playDrawSound() else SoundManager.playWinSound()
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
                        if (winner == "Draw") SoundManager.playDrawSound() else SoundManager.playWinSound()
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToMenu) {
                Text("< Back", fontSize = 16.sp, color = themeColors.textSecondary)
            }
            Text(
                text = when (gameMode) {
                    GameMode.AI -> "VS AI"
                    GameMode.TWO_PLAYER -> "2 Players"
                    GameMode.NEARBY_P2P -> "Nearby P2P (You: $myRole)"
                },
                color = themeColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            IconButton(onClick = {
                SoundManager.toggleBgm(context)
                isMusicOn = SoundManager.isMusicPlaying
            }) {
                Text(if (isMusicOn) "🔊" else "🔇", fontSize = 18.sp)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val statusText = when {
                winner == "Draw" -> "It's a Draw!"
                winner != null -> "$winner Wins!"
                isThinking -> "AI is thinking..."
                gameMode == GameMode.NEARBY_P2P && currentPlayer == myRole -> "Your Turn ($myRole)"
                gameMode == GameMode.NEARBY_P2P -> "Opponent's Turn ($currentPlayer)"
                else -> "Player $currentPlayer's Turn"
            }
            val statusColor = when {
                winner == "X" || currentPlayer == "X" -> themeColors.accentX
                winner == "O" || currentPlayer == "O" -> themeColors.accentO
                else -> themeColors.textPrimary
            }

            Text(
                text = statusText,
                color = statusColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .size(320.dp)
                .background(themeColors.cardBg, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cellValue = board[index]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(themeColors.cellBg, RoundedCornerShape(12.dp))
                                    .clickable { handleCellClick(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellValue,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (cellValue == "X") themeColors.accentX else themeColors.accentO
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { resetGame(notifyPeer = true) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 24.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.textSecondary.copy(alpha = 0.5f))
        ) {
            Text(
                text = if (gameMode == GameMode.NEARBY_P2P) "Restart Match" else "Restart Game",
                color = themeColors.textPrimary,
                fontSize = 16.sp
            )
        }
    }
}
