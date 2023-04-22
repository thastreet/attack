import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.math.abs

private const val FRAMES_TO_SKIP = 8

fun runServer(analyze: (Response) -> Pair<Int, Int>?) {
    val datagramSocket = DatagramSocket(Constants.PORT)

    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)

    var queue: MutableList<String>? = null
    var framesToSkip = 0

    while (true) {
        datagramSocket.receive(packet)

        val responseJson = packet.data.decodeToString().take(packet.length)
        val response = Json.decodeFromString(Response.serializer(), responseJson)

        val command = when {
            framesToSkip > 0 -> {
                --framesToSkip
                Constants.COMMAND_NO_OP
            }

            response.board
                .flatten()
                .map { it.state }
                .any { it == Constants.BLOCK_STATE_BLOCK_ANIMATION_1 || it == Constants.BLOCK_STATE_BLOCK_ANIMATION_2 } || isAnyBlockFalling(response.board)
            -> {
                framesToSkip = FRAMES_TO_SKIP
                queue = null
                Constants.COMMAND_NO_OP
            }

            response.board
                .lastOrNull()
                ?.any { it.value != Constants.BLOCK_VALUE_EMPTY && it.value != Constants.BLOCK_VALUE_UNKNOWN } == true
            -> {
                framesToSkip = FRAMES_TO_SKIP

                if (!queue.isNullOrEmpty()) {
                    queue.removeFirst()
                } else {
                    analyze(response)?.let {
                        println("Moving to ${it.second}, ${it.first}")
                        val newQueue = mutableListOf<String>()

                        if (it.first > response.cursor.x) {
                            newQueue.addAll(List(abs(it.first - response.cursor.x)) { "right" })
                        } else if (it.first < response.cursor.x) {
                            newQueue.addAll(List(abs(response.cursor.x - it.first)) { "left" })
                        }

                        if (it.second > response.cursor.y) {
                            newQueue.addAll(List(abs(it.second - response.cursor.y)) { "down" })
                        } else if (it.second < response.cursor.y) {
                            newQueue.addAll(List(abs(response.cursor.y - it.second)) { "up" })
                        }

                        newQueue.add("A")

                        queue = newQueue
                        Constants.COMMAND_NO_OP
                    } ?: Constants.COMMAND_NO_OP
                }
            }

            else -> Constants.COMMAND_NO_OP
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

private fun isAnyBlockFalling(board: List<List<Response.Block>>): Boolean {
    for (j in board.size - 1 downTo 0) {
        for (i in board[j].indices) {
            if (board[j][i].value != 0) continue

            for (k in j - 1 downTo 0) {
                if (board[k][i].value != 0) {
                    return true
                }
            }
        }
    }

    return false
}