import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val board: List<List<Block>>,
    val cursor: Cursor
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
}