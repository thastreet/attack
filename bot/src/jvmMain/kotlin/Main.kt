import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket

@Serializable
data class Response(
    val board: List<List<Block>>
) {
    @Serializable
    data class Block(
        val value: Int,
        val state: Int
    )
}

@Composable
@Preview
fun App(response: Response?) {
    val board = response?.board ?: return

    Column {
        board.forEach { row ->
            Row {
                row.forEach { block ->
                    Box(Modifier.size(32.dp)) {
                        when (val value = block.value) {
                            in 1..7 -> Image(
                                bitmap = useResource("$value.png") { loadImageBitmap(it) },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.None
                            )

                            0 -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray)
                            )

                            else -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = value.toString())
                            }
                        }

                        when (block.state) {
                            2 -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.33f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("!", fontSize = 26.sp, color = Color.White)
                            }

                            4 -> Image(
                                bitmap = useResource("block.png") { loadImageBitmap(it) },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.None
                            )

                            16 -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.33f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", fontSize = 26.sp, color = Color.White)
                            }

                            20, 68 -> Image(
                                bitmap = useResource("block2.png") { loadImageBitmap(it) },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.None
                            )

                            64 -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.33f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", fontSize = 26.sp, color = Color.White)
                            }

                        }
                    }
                }
            }
        }
    }
}

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
            App(response)
        }
    }
}

fun runServer(onResponse: (Response) -> Unit) {
    val datagramSocket = DatagramSocket(9876)

    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)

    while (true) {
        datagramSocket.receive(packet)
        onResponse(Json.decodeFromString(Response.serializer(), packet.data.decodeToString().take(packet.length)))
    }
}