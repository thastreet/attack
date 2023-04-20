import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.math.abs

private const val MOCK_ENABLED = false

fun main() {
    application {
        var response by remember {
            mutableStateOf<Response?>(null)
        }

        var debug by remember {
            mutableStateOf<Array<Array<Response.Block>>?>(null)
        }

        Window(onCloseRequest = ::exitApplication) {
            RootView(response, debug)
        }

        LaunchedEffect(true) {
            CoroutineScope(Dispatchers.IO).launch {
                runServer {
                    analyze(it)
                }
            }
        }
    }
}

fun analyze(response: Response): Pair<Int, Combo>? {
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

fun simulateFlip(board: Array<Array<Response.Block>>, i: Int, j: Int): Pair<Int, Combo>? {
    println("simulating flip ($i, $j)")

    val temp = board[j][i]
    board[j][i] = board[j][i + 1]
    board[j][i + 1] = temp

    applyGravity(board)

    for (k in board.indices) {
        val combos = getConsecutive(board[k].map { it.value }, i, j, k).filter { it.count > 2 }
        combos.firstOrNull()?.let {
            println("Combo ${it.count} at row index $k col ${it.startIndex}")
            return Pair(j, it)
        }
    }

    return null
}

data class Combo(
    val value: Int,
    val count: Int,
    val startIndex: Int
)

// TODO: Fix issue with XOXX moving to XXOX
fun getConsecutive(row: Collection<Int>, i: Int, j: Int, k: Int): List<Combo> =
    row.foldIndexed(mutableListOf()) { index, acc, value ->
        if (value == 0) return@foldIndexed acc

        if (acc.isEmpty()) {
            acc.add(Combo(value, 1, index))
        } else {
            val last = acc.last()
            if (last.value == value) {
                acc[acc.size - 1] = Combo(last.value, last.count + 1, if (j == k && index == i) index else last.startIndex)
            } else {
                acc.add(Combo(value, 1, index))
            }
        }

        acc
    }

fun applyGravity(board: Array<Array<Response.Block>>) {
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

fun runServer(analyze: (Response) -> Pair<Int, Combo>?) {
    if (MOCK_ENABLED) {
        analyze(Json.decodeFromString(Response.serializer(), Mock.response))
        return
    }

    val datagramSocket = DatagramSocket(9876)

    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)

    var queue: MutableList<String>? = null
    var skipNextFrame = false

    while (true) {
        datagramSocket.receive(packet)

        val responseJson = packet.data.decodeToString().take(packet.length)
        val response = Json.decodeFromString(Response.serializer(), responseJson)

        val animating = response.board.flatten().map { it.state }.any { it == 16 || it == 64 }
        val command = if (!animating && response.board.last().any { it.value != 0 && it.value != 255 }) {
            if (queue == null) {
                analyze(response)?.let {
                    println("Moving to ${it.second.startIndex}, ${it.first}")
                    val newQueue = mutableListOf<String>()

                    val cursorX = response.cursor.x - 1
                    val cursorY = response.cursor.y - 4

                    if (it.second.startIndex > cursorX) {
                        newQueue.addAll(List(abs(it.second.startIndex - cursorX)) { "right" })
                    } else if (it.second.startIndex < cursorX) {
                        newQueue.addAll(List(abs(cursorX - it.second.startIndex)) { "left" })
                    }

                    if (it.first > cursorY) {
                        newQueue.addAll(List(abs(it.first - cursorY)) { "down" })
                    } else if (it.first < cursorY) {
                        newQueue.addAll(List(abs(cursorY - it.first)) { "up" })
                    }

                    newQueue.add("A")

                    queue = newQueue
                }
            }

            if (response.board.last().any { it.value != 0 && it.value != 255 } && !queue.isNullOrEmpty() && !skipNextFrame) {
                skipNextFrame = true
                queue?.removeFirst() ?: "no-op"
            } else {
                skipNextFrame = false
                "no-op"
            }
        } else {
            if (animating) {
                queue = null
            }

            "no-op"
        }

        datagramSocket.send(
            DatagramPacket(
                command.encodeToByteArray(),
                command.length,
                packet.address,
                packet.port
            )
        )
    }
}