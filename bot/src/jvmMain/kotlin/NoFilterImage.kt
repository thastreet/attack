import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource

@Composable
fun NoFilterImage(resourcePath: String, modifier: Modifier = Modifier) {
    Image(
        bitmap = useResource(resourcePath) { loadImageBitmap(it) },
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None
    )
}