package com.example.myempty.tictactoe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.random.Random

@Composable
fun OnlineGameScreen(onBackToLocal: () -> Unit) {
    val context = LocalContext.current
    var roomCode by remember { mutableStateOf("") }
    var inRoom by remember { mutableStateOf(false) }
    var myRole by remember { mutableStateOf("X") }
    var board by remember { mutableStateOf(List(9) { "" }) }
    var turn by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var scoreX by remember { mutableIntStateOf(0) }
    var scoreO by remember { mutableIntStateOf(0) }
    var scoreTies by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Create a new room or join with a 4-digit code") }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showMusicModal by remember { mutableStateOf(false) }
    val db = remember { FirebaseFirestore.getInstance() }

    fun listenToRoom(code: String) {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("rooms").document(code).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val rawBoard = snapshot.get("board") as? List<*>
            if (rawBoard != null && rawBoard.size == 9) {
                board = rawBoard.map { it?.toString() ?: "" }
            }
            turn = snapshot.getString("turn") ?: "X"
            winner = snapshot.getString("winner")
            scoreX = snapshot.getLong("scoreX")?.toInt() ?: 0
            scoreO = snapshot.getLong("scoreO")?.toInt() ?: 0
            scoreTies = snapshot.getLong("scoreTies")?.toInt() ?: 0
        }
    }

    fun createRoom() {
        val newCode = Random.nextInt(1000, 10000).toString()
        roomCode = newCode
        myRole = "X"
        val initialData = mapOf(
            "board" to List(9) { "" },
            "turn" to "X",
            "playerX" to true,
            "playerO" to false,
            "winner" to "",
            "scoreX" to 0,
            "scoreO" to 0,
            "scoreTies" to 0
        )
        db.collection("rooms").document(newCode).set(initialData).addOnSuccessListener {
            inRoom = true
            listenToRoom(newCode)
        }.addOnFailureListener {
            statusMessage = "Failed to create room. Check connection."
        }
    }

    fun joinRoom(code: String) {
        if (code.length != 4) {
            statusMessage = "Room code must be 4 digits"
            return
        }
        val roomRef = db.collection("rooms").document(code)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                statusMessage = "Room $code not found!"
            } else {
                val playerO = snapshot.getBoolean("playerO") ?: false
                if (!playerO) {
                    myRole = "O"
                    roomCode = code
                    roomRef.update("playerO", true).addOnSuccessListener {
                        inRoom = true
                        listenToRoom(code)
                    }
                } else {
                    statusMessage = "Room $code is full!"
                }
            }
        }.addOnFailureListener {
            statusMessage = "Network error connecting to room."
        }
    }

    fun makeMove(index: Int) {
        val currentWinner = winner
        if (board[index].isNotEmpty() || turn != myRole || !currentWinner.isNullOrEmpty()) return
        SoundManager.playMoveSound()
        val newBoard = board.toMutableList()
        newBoard[index] = myRole
        val nextTurn = if (myRole == "X") "O" else "X"
        val engine = GameEngine()
        val outcome = engine.checkWinner(newBoard)

        val updates = mutableMapOf<String, Any>("board" to newBoard, "turn" to nextTurn)
        if (outcome != null) {
            updates["winner"] = outcome
            if (outcome == "X") updates["scoreX"] = scoreX + 1
            else if (outcome == "O") updates["scoreO"] = scoreO + 1
            else if (outcome == "Draw") updates["scoreTies"] = scoreTies + 1
        }
        db.collection("rooms").document(roomCode).update(updates)
    }

    fun resetMatch() {
        SoundManager.playMoveSound()
        db.collection("rooms").document(roomCode).update(mapOf("board" to List(9) { "" }, "turn" to "X", "winner" to ""))
    }

    fun leaveRoom() {
        listenerRegistration?.remove()
        inRoom = false
        roomCode = ""
        onBackToLocal()
    }

    DisposableEffect(Unit) { onDispose { listenerRegistration?.remove() } }

    if (showMusicModal) {
        MusicPlayerModal(
            themeColors = getAppColors(AppThemeMode.DEFAULT),
            onDismiss = { showMusicModal = false }
        )
    }
    if (!inRoom) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("← Back", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBackToLocal).padding(vertical = 4.dp, horizontal = 6.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF141620)).border(1.dp, Color(0xFF222536), RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Online Cloud Match", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusMessage, fontSize = 13.sp, color = Color(0xFFFF9100), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { createRoom() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                ) {
                    Text("Create New Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF262936))
                    Text("  OR JOIN  ", fontSize = 11.sp, color = Color(0xFF757A8E), fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF262936))
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) roomCode = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter 4-Digit Room Code", color = Color(0xFF8A90A6)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF2E3246)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { joinRoom(roomCode) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E202B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text("Join Room", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Text("Powered by Firebase Firestore real-time sync", fontSize = 12.sp, color = Color(0xFF8A90A6))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clean Top Bar with Room Code, Role, and Both Sound Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room: $roomCode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF9100)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "You: $myRole",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (myRole == "X") Color(0xFF00E5FF) else Color(0xFFFF4081),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    IconButton(
                        onClick = { SoundManager.toggleBgm(context) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(if (SoundManager.isMusicPlaying) "🔊" else "🔇", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showMusicModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🎵", fontSize = 18.sp)
                    }
                }
            }

            // 3-Pillar Online Scoreboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141620))
                        .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (myRole == "X") "YOU (X)" else "OPP (X)", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF00E5FF))
                        Text(text = "$scoreX", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141620))
                        .border(1.5.dp, Color(0xFF2E3244), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF8A90A6))
                        Text(text = "$scoreTies", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141620))
                        .border(1.5.dp, Color(0xFFFF4081).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (myRole == "O") "YOU (O)" else "OPP (O)", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFFFF4081))
                        Text(text = "$scoreO", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                val currentWinner = winner
                val status = when {
                    currentWinner == "Draw" -> "Draw Game!"
                    !currentWinner.isNullOrEmpty() -> "$currentWinner Wins!"
                    turn == myRole -> "Your Turn ($myRole)"
                    else -> "Opponent's Turn ($turn)..."
                }
                Text(text = status, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = when {
                        currentWinner == "X" || (currentWinner.isNullOrEmpty() && turn == "X") -> Color(0xFF00E5FF)
                        currentWinner == "O" || (currentWinner.isNullOrEmpty() && turn == "O") -> Color(0xFFFF4081)
                        else -> Color.White
                    }
                )
            }

            Box(
                modifier = Modifier.size(330.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF141620)).border(1.5.dp, Color(0xFF222536), RoundedCornerShape(24.dp)).padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in 0..2) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                val cellValue = board.getOrElse(index) { "" }
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E202B))
                                        .border(1.dp, if (cellValue.isNotEmpty()) (if (cellValue == "X") Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFFFF4081).copy(alpha = 0.5f)) else Color.Transparent, RoundedCornerShape(16.dp))
                                        .clickable { makeMove(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = cellValue, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = if (cellValue == "X") Color(0xFF00E5FF) else Color(0xFFFF4081))
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { leaveRoom() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202330)), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF323646))) {
                    Text("Leave Room", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = { resetMatch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)), shape = RoundedCornerShape(16.dp)) {
                    Text("Rematch", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
