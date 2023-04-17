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

private const val MOCK_ENABLED = false

fun main() {
    application {
        val coroutineScope = remember { CoroutineScope(Dispatchers.IO) }

        var response by remember {
            mutableStateOf<Response?>(null)
        }

        coroutineScope.launch {
            runServer { response = it }
        }

        Window(onCloseRequest = ::exitApplication) {
            DebugView(response)
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