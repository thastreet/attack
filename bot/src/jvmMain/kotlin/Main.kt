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

private const val MOCK_ENABLED = true

fun main() {
    application {
        val coroutineScope = remember { CoroutineScope(Dispatchers.IO) }

        var response by remember {
            mutableStateOf<Response?>(null)
        }

        var debug by remember {
            mutableStateOf<Array<Array<Response.Block>>?>(null)
        }

        coroutineScope.launch {
            runServer {
                response = it
                debug = analyze(it)
            }
        }

        Window(onCloseRequest = ::exitApplication) {
            RootView(response, debug)
        }
    }
}

fun analyze(response: Response): Array<Array<Response.Block>> {
    val board = response.board
        .dropLast(1)
        .map { it.toTypedArray() }
        .toTypedArray()

    for (j in board.indices) {
        for (i in 0 until board[j].size - 1) {
            if (board[j][i].value == board[j][i + 1].value) continue
            simulateFlip(board, i, j)
        }
    }

    return board
}

fun simulateFlip(board: Array<Array<Response.Block>>, i: Int, j: Int) {
    println("simulating flip ($i, $j)")

    val temp = board[j][i]
    board[j][i] = board[j][i + 1]
    board[j][i + 1] = temp

    applyGravity(board)
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

fun runServer(onResponse: (Response) -> Unit) {
    if (MOCK_ENABLED) {
        onResponse(Json.decodeFromString(Response.serializer(), Mock.response))
        return
    }

    val datagramSocket = DatagramSocket(9876)

    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)

    while (true) {
        datagramSocket.receive(packet)

        val responseJson = packet.data.decodeToString().take(packet.length)
        val response = Json.decodeFromString(Response.serializer(), responseJson)

        onResponse(response)

        val command = "no-op"
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