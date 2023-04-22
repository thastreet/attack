object Analyzer {
    fun analyze(response: Response): Pair<Int, Int>? {
        val boardWithoutLastRow = response.board.dropLast(1)

        for (j in boardWithoutLastRow.indices) {
            for (i in 0 until boardWithoutLastRow[j].size - 1) {
                val board = boardWithoutLastRow.map { it.toTypedArray() }.toTypedArray()

                if (board[j][i].value == board[j][i + 1].value) continue
                simulateFlip(board, i, j)?.let {
                    return it
                }
            }
        }

        return null
    }

    private fun simulateFlip(board: Array<Array<Response.Block>>, i: Int, j: Int): Pair<Int, Int>? {
        println("Simulating flip ($i, $j)")

        val temp = board[j][i]
        board[j][i] = board[j][i + 1]
        board[j][i + 1] = temp

        applyGravity(board)

        for (k in board.indices) {
            val combos = getConsecutive(board[k].map { it.value }).filter { it.count > 2 && it.value != 0 }
            combos.firstOrNull()?.let {
                println("Combo ${it.count} at row from bottom ${board.size - k - 1} col ${it.startIndex}")
                return Pair(i, j)
            }
        }

        return null
    }

    private data class Combo(
        val value: Int,
        val count: Int,
        val startIndex: Int
    )

    private fun getConsecutive(row: Collection<Int>): List<Combo> =
        row.foldIndexed(mutableListOf()) { index, acc, value ->
            if (acc.isEmpty()) {
                acc.add(Combo(value, 1, index))
            } else {
                val last = acc.last()
                if (last.value == value) {
                    acc[acc.size - 1] = Combo(last.value, last.count + 1, last.startIndex)
                } else {
                    acc.add(Combo(value, 1, index))
                }
            }

            acc
        }

    private fun applyGravity(board: Array<Array<Response.Block>>) {
        for (j in board.size - 1 downTo 0) {
            for (i in board[j].indices) {
                if (board[j][i].value != 0) continue

                for (k in j - 1 downTo 0) {
                    if (board[k][i].value != 0) {
                        val temp = board[k][i]
                        board[j][i] = temp
                        board[k][i] = Response.Block(0, 0)
                        break
                    }
                }
            }
        }
    }
}