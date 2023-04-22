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

private const val USE_LOCAL_CLIENT = true

fun main() {
    application {
        var response by remember {
            mutableStateOf<Response?>(null)
        }

        Window(onCloseRequest = ::exitApplication) {
            GameView(response)
        }

        LaunchedEffect(true) {
            CoroutineScope(Dispatchers.IO).launch {
                runServer {
                    response = it
                    Analyzer.analyze(it)
                }
            }

            if (USE_LOCAL_CLIENT) {
                CoroutineScope(Dispatchers.IO).launch {
                    runClient()
                }
            }
        }
    }
}