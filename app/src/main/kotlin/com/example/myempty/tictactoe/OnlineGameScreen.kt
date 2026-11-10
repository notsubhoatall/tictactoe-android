package com.example.myempty.tictactoe

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreen(onBackToLocal: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var myUserId by remember { mutableStateOf("") }
    var currentRoomId by remember { mutableStateOf("") }
    var inputRoomId by remember { mutableStateOf("") }
    var activeRoom by remember { mutableStateOf<OnlineRoom?>(null) }
    var isHost by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        myUserId = OnlineMultiplayerManager.getOrCreateUserId()
    }

    LaunchedEffect(currentRoomId) {
        if (currentRoomId.isNotEmpty()) {
            OnlineMultiplayerManager.observeRoom(currentRoomId).collect { room ->
                activeRoom = room
            }
        }
    }

    val mySymbol = if (isHost) "X" else "O"
    val isMyTurn = activeRoom?.currentTurn == mySymbol && activeRoom?.status == "PLAYING"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentRoomId.isEmpty()) {
            Text(
                text = "Online Multiplayer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isSearching = true
                    scope.launch {
                        OnlineMultiplayerManager.createRoom(
                            onSuccess = { code ->
                                isHost = true
                                currentRoomId = code
                                isSearching = false
                            },
                            onError = { err ->
                                isSearching = false
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                enabled = !isSearching
            ) {
                Text(
                    text = if (isSearching) "Creating..." else "Create Room",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.85f),
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = inputRoomId,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        inputRoomId = it
                    }
                },
                label = { Text("4-digit Room Code", color = Color(0xFF00E5FF)) },
                placeholder = { Text("e.g. 1234", color = Color(0xFF757575)) },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF666666),
                    cursorColor = Color(0xFF00E5FF),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (inputRoomId.length == 4) {
                        isSearching = true
                        scope.launch {
                            OnlineMultiplayerManager.joinRoom(
                                roomCode = inputRoomId,
                                onSuccess = {
                                    isHost = false
                                    currentRoomId = inputRoomId
                                    isSearching = false
                                },
                                onError = { err ->
                                    isSearching = false
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    } else {
                        Toast.makeText(context, "Enter a 4-digit code", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                enabled = !isSearching && inputRoomId.isNotEmpty()
            ) {
                Text(
                    text = if (isSearching) "Joining..." else "Join Room",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onBackToLocal) {
                Text("< Back to Local Game", color = Color(0xFF9E9E9E), fontSize = 15.sp)
            }
        } else {
            // Inside Active Room
            Text(
                text = "Room: $currentRoomId",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You are: $mySymbol",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (mySymbol == "X") Color(0xFF00E5FF) else Color(0xFFFF4081)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isRoundOver = activeRoom?.status == "FINISHED" || (activeRoom?.winner?.isNotEmpty() == true)

            val statusText = when {
                activeRoom?.status == "WAITING" -> "Waiting for opponent to join..."
                isRoundOver -> {
                    when (activeRoom?.winner) {
                        mySymbol -> "🎉 You Win!"
                        "DRAW" -> "It's a Draw!"
                        else -> "Opponent Won!"
                    }
                }
                isMyTurn -> "Your Turn ($mySymbol)"
                else -> "Opponent's Turn (${if (mySymbol == "X") "O" else "X"})..."
            }

            Text(
                text = statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                color = if (isMyTurn) Color(0xFF00E5FF) else Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val board = activeRoom?.board ?: List(9) { "" }

            Box(
                modifier = Modifier
                    .size(310.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val cellValue = board[index]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF282828), RoundedCornerShape(12.dp))
                                        .clickable(
                                            enabled = isMyTurn && cellValue.isEmpty() && !isRoundOver
                                        ) {
                                            OnlineMultiplayerManager.makeMove(
                                                roomCode = currentRoomId,
                                                index = index,
                                                symbol = mySymbol,
                                                currentBoard = board
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cellValue,
                                        fontSize = 40.sp,
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRoundOver) {
                    Button(
                        onClick = {
                            OnlineMultiplayerManager.resetMatch(currentRoomId)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Play Again 🔄", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        OnlineMultiplayerManager.leaveRoom(currentRoomId, isHost)
                        currentRoomId = ""
                        activeRoom = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Leave Room", color = Color(0xFFEEEEEE))
                }
            }
        }
    }
}
