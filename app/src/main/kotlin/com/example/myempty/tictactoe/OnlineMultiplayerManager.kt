package com.example.myempty.tictactoe

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class OnlineRoom(
    val roomId: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val board: List<String> = List(9) { "" },
    val currentTurn: String = "X",
    val winner: String = "",
    val status: String = "WAITING"
)

object OnlineMultiplayerManager {
    private val auth by lazy { Firebase.auth }
    private val db by lazy { Firebase.firestore }

    suspend fun getOrCreateUserId(): String {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            currentUser.uid
        } else {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: ""
        }
    }

    suspend fun createRoom(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            val uid = getOrCreateUserId()
            val roomCode = (1000..9999).random().toString()
            val newRoom = OnlineRoom(
                roomId = roomCode,
                hostId = uid,
                status = "WAITING"
            )
            db.collection("rooms").document(roomCode).set(newRoom).await()
            onSuccess(roomCode)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to create room")
        }
    }

    suspend fun joinRoom(roomCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val uid = getOrCreateUserId()
            val docRef = db.collection("rooms").document(roomCode)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                onError("Room not found!")
                return
            }

            val room = snapshot.toObject(OnlineRoom::class.java)
            if (room?.status == "WAITING") {
                docRef.update(
                    mapOf(
                        "guestId" to uid,
                        "status" to "PLAYING"
                    )
                ).await()
                onSuccess()
            } else {
                onError("Room is full or already started!")
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to join room")
        }
    }

    fun observeRoom(roomCode: String): Flow<OnlineRoom?> = callbackFlow {
        val docRef = db.collection("rooms").document(roomCode)
        val registration: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val room = snapshot?.toObject(OnlineRoom::class.java)
            trySend(room)
        }
        awaitClose { registration.remove() }
    }

    fun makeMove(roomCode: String, index: Int, symbol: String, currentBoard: List<String>) {
        val updatedBoard = currentBoard.toMutableList()
        updatedBoard[index] = symbol
        val nextTurn = if (symbol == "X") "O" else "X"
        val winner = checkWinner(updatedBoard)
        val status = if (winner.isNotEmpty()) "FINISHED" else "PLAYING"

        db.collection("rooms").document(roomCode).update(
            mapOf(
                "board" to updatedBoard,
                "currentTurn" to nextTurn,
                "winner" to winner,
                "status" to status
            )
        )
    }

    fun resetMatch(roomCode: String) {
        db.collection("rooms").document(roomCode).update(
            mapOf(
                "board" to List(9) { "" },
                "currentTurn" to "X",
                "winner" to "",
                "status" to "PLAYING"
            )
        )
    }

    fun leaveRoom(roomCode: String, isHost: Boolean) {
        if (roomCode.isEmpty()) return
        val docRef = db.collection("rooms").document(roomCode)
        if (isHost) {
            docRef.delete()
        } else {
            docRef.update(
                mapOf(
                    "guestId" to "",
                    "status" to "WAITING"
                )
            )
        }
    }

    private fun checkWinner(board: List<String>): String {
        val winPositions = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (pos in winPositions) {
            val (a, b, c) = pos
            if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c]) {
                return board[a]
            }
        }
        return if (board.none { it.isEmpty() }) "DRAW" else ""
    }
}
