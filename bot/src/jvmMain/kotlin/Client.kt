import Response.Block
import Response.Cursor
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

suspend fun runClient() {
    val datagramSocket = DatagramSocket()

    while (true) {
        val command = Json.encodeToString(
            Response.serializer(), Response(
                List(Constants.ROW_COUNT - 4) {
                    List(Constants.COLUMN_COUNT) { Block(0, Constants.BLOCK_STATE_NORMAL) }
                } + listOf(
                    listOf(Block(0, 0), Block(0, 0), Block(1, 0), Block(0, 0), Block(0, 0), Block(0, 0))
                ) + listOf(
                    listOf(Block(0, 0), Block(0, 0), Block(1, 0), Block(0, 0), Block(0, 0), Block(0, 0))
                ) + listOf(
                    listOf(Block(0, 0), Block(0, 0), Block(2, 0), Block(1, 0), Block(0, 0), Block(0, 0))
                ) + listOf(
                    List(Constants.COLUMN_COUNT) { Block(1, Constants.BLOCK_STATE_LAST_ROW) }
                ),
                Cursor(Constants.CURSOR_X_OFFSET, Constants.CURSOR_Y_OFFSET)
            )
        )
        datagramSocket.send(
            DatagramPacket(
                command.encodeToByteArray(),
                command.length,
                InetAddress.getLocalHost(),
                Constants.PORT
            )
        )

        delay(16)
    }
}