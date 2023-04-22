import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Response(
    val board: List<List<Block>>,
    @SerialName("cursor") private val cursorRaw: Cursor
) {
    @Serializable
    data class Block(
        val value: Int,
        val state: Int
    )

    @Serializable
    data class Cursor(
        val x: Int,
        val y: Int
    )

    @Transient
    val cursor: Cursor = Cursor(cursorRaw.x - Constants.CURSOR_X_OFFSET, cursorRaw.y - Constants.CURSOR_Y_OFFSET)
}
