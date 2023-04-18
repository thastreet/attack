import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RootView(response: Response?, debug: Array<Array<Response.Block>>?) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(100.dp)
    ) {
        response?.let { GameView(it) }
        debug?.let {
            GameView(
                Response(
                    it.map { it.toList() }.toList(),
                    Response.Cursor(0, 0)
                )
            )
        }
    }
}