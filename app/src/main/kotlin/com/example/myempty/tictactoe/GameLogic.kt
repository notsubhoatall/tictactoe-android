package com.example.myempty.tictactoe

enum class GameMode {
    AI, TWO_PLAYER, NEARBY_P2P
}

class GameEngine {
    private val winningCombinations = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )

    fun checkWinner(board: List<String>): String? {
        for (combo in winningCombinations) {
            val (a, b, c) = combo
            if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c]) {
                return board[a]
            }
        }
        return if (board.all { it.isNotEmpty() }) "Draw" else null
    }

    // Minimax algorithm to find optimal AI move (AI is always 'O')
    fun getBestMove(board: List<String>): Int {
        var bestScore = Int.MIN_VALUE
        var move = -1

        for (i in board.indices) {
            if (board[i].isEmpty()) {
                val tempBoard = board.toMutableList()
                tempBoard[i] = "O"
                val score = minimax(tempBoard, 0, false)
                if (score > bestScore) {
                    bestScore = score
                    move = i
                }
            }
        }
        return move
    }

    private fun minimax(board: MutableList<String>, depth: Int, isMaximizing: Boolean): Int {
        val result = checkWinner(board)
        if (result == "O") return 10 - depth
        if (result == "X") return depth - 10
        if (result == "Draw") return 0

        return if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (i in board.indices) {
                if (board[i].isEmpty()) {
                    board[i] = "O"
                    val score = minimax(board, depth + 1, false)
                    board[i] = ""
                    bestScore = maxOf(score, bestScore)
                }
            }
            bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (i in board.indices) {
                if (board[i].isEmpty()) {
                    board[i] = "X"
                    val score = minimax(board, depth + 1, true)
                    board[i] = ""
                    bestScore = minOf(score, bestScore)
                }
            }
            bestScore
        }
    }
}
