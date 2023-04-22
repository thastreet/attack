
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times

@Composable
fun GameView(response: Response?) {
    val requiredResponse = response ?: return

    Box {
        val blockSize = 32.dp

        Column {
            requiredResponse.board.forEach { row ->
                Row {
                    row.forEach { block ->
                        Box(Modifier.size(blockSize)) {
                            when (val value = block.value) {
                                in 1..7 -> NoFilterImage(
                                    "$value.png",
                                    modifier = Modifier.fillMaxSize()
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
                                Constants.BLOCK_STATE_CRITICAL -> Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.33f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("!", fontSize = 26.sp, color = Color.White)
                                }

                                Constants.BLOCK_STATE_CEMENT -> NoFilterImage(
                                    "block.png",
                                    modifier = Modifier.fillMaxSize()
                                )

                                Constants.BLOCK_STATE_BLOCK_ANIMATION_2 -> Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.33f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("2", fontSize = 26.sp, color = Color.White)
                                }

                                Constants.BLOCK_STATE_LAST_ROW -> Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                )

                                Constants.BLOCK_STATE_BREAKING_1, Constants.BLOCK_STATE_BREAKING_2 -> NoFilterImage(
                                    "block2.png",
                                    modifier = Modifier.fillMaxSize()
                                )

                                Constants.BLOCK_STATE_BLOCK_ANIMATION_1 -> Box(
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

        NoFilterImage(
            "cursor.png",
            modifier = Modifier
                .offset(response.cursor.x * blockSize - 4.dp, response.cursor.y * blockSize - 4.dp)
                .width(72.dp)
                .height(40.dp)
        )
    }
}