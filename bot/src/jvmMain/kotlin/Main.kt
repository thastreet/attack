
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
private const val FRAMES_TO_SKIP = 15

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

fun simulateFlip(board: Array<Array<Response.Block>>, i: Int, j: Int): Pair<Int, Int>? {
    println("simulating flip ($i, $j)")

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

data class Combo(
    val value: Int,
    val count: Int,
    val startIndex: Int
)

fun getConsecutive(row: Collection<Int>): List<Combo> =
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

fun runServer(analyze: (Response) -> Pair<Int, Int>?) {
    if (MOCK_ENABLED) {
        analyze(Json.decodeFromString(Response.serializer(), Mock.response))
        return
    }

    val datagramSocket = DatagramSocket(9876)

    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)

    var queue: MutableList<String>? = null
    var framesToSkip = 0

    while (true) {
        datagramSocket.receive(packet)

        val responseJson = packet.data.decodeToString().take(packet.length)
        val response = Json.decodeFromString(Response.serializer(), responseJson)

        // TODO: wait for any 0 block under a non 0 before analyzing, which means the blocks are not yet finished from falling
        val command = if (framesToSkip > 0) {
            --framesToSkip
            "no-op"
        } else if (response.board.flatten().map { it.state }.any { it == 16 || it == 64 }) {
            framesToSkip = FRAMES_TO_SKIP
            queue = null
            "no-op"
        } else if (response.board.last().any { it.value != 0 && it.value != 255 }) {
            framesToSkip = FRAMES_TO_SKIP

            if (!queue.isNullOrEmpty()) {
                queue.removeFirst()
            } else {
                analyze(response)?.let {
                    println("Moving to ${it.second}, ${it.first}")
                    val newQueue = mutableListOf<String>()

                    val cursorX = response.cursor.x - 1
                    val cursorY = response.cursor.y - 4

                    if (it.first > cursorX) {
                        newQueue.addAll(List(abs(it.first - cursorX)) { "right" })
                    } else if (it.first < cursorX) {
                        newQueue.addAll(List(abs(cursorX - it.first)) { "left" })
                    }

                    if (it.second > cursorY) {
                        newQueue.addAll(List(abs(it.second - cursorY)) { "down" })
                    } else if (it.second < cursorY) {
                        newQueue.addAll(List(abs(cursorY - it.second)) { "up" })
                    }

                    newQueue.add("A")

                    queue = newQueue
                    "no-op"
                } ?: "no-op"
            }
        } else {
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