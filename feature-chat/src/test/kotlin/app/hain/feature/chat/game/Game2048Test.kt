package app.hain.feature.chat.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Game2048Test {
    private fun board(vararg rows: String): Board {
        var id = 0
        val tiles =
            rows.flatMapIndexed { r, line ->
                line.split(" ").mapIndexedNotNull { c, v -> v.toIntOrNull()?.let { Tile(id++, it, r, c) } }
            }
        return Board(tiles, nextId = id)
    }

    private fun Board.row(r: Int): List<Int> = (0 until Game2048.SIZE).map { c -> at(r, c)?.value ?: 0 }

    private fun Board.col(c: Int): List<Int> = (0 until Game2048.SIZE).map { r -> at(r, c)?.value ?: 0 }

    @Test
    fun `tiles slide to the wall`() {
        val b = board(". 2 . 4", ". . . .", ". . . .", ". . . .")
        val result = Game2048.move(b, Direction.LEFT)
        assertTrue(result.moved)
        assertEquals(listOf(2, 4, 0, 0), result.board.row(0))
    }

    @Test
    fun `equal neighbours merge once per move`() {
        val b = board("2 2 2 2", "4 4 8 .", ". . . .", ". . . .")
        val result = Game2048.move(b, Direction.LEFT)
        assertEquals(listOf(4, 4, 0, 0), result.board.row(0))
        assertEquals(listOf(8, 8, 0, 0), result.board.row(1))
        assertEquals(4 + 4 + 8, result.gained)
        assertEquals(16, result.board.score)
        assertEquals(listOf(4, 4, 8), result.merges)
    }

    @Test
    fun `right, up and down work the same way`() {
        val b = board("2 . 2 4", ". . . .", "2 . . .", ". . . .")
        assertEquals(listOf(0, 0, 4, 4), Game2048.move(b, Direction.RIGHT).board.row(0))
        assertEquals(listOf(4, 0, 0, 0), Game2048.move(b, Direction.UP).board.col(0))
        assertEquals(listOf(0, 0, 0, 4), Game2048.move(b, Direction.DOWN).board.col(0))
    }

    @Test
    fun `a move that changes nothing is not a move and spawns nothing`() {
        val b = board("2 4 . .", ". . . .", ". . . .", ". . . .")
        val result = Game2048.play(b, Direction.LEFT, Random(1))
        assertFalse(result.moved)
        assertEquals(b, result.board)
    }

    @Test
    fun `a real move spawns exactly one 2 or 4 on a free cell`() {
        val b = board(". . . 2", ". . . .", ". . . .", ". . . .")
        val result = Game2048.play(b, Direction.LEFT, Random(3))
        assertEquals(2, result.board.tiles.size)
        val fresh = result.board.tiles.single { it.fresh }
        assertTrue(fresh.value == 2 || fresh.value == 4)
        assertEquals(
            2,
            result.board.tiles
                .map { it.row to it.col }
                .toSet()
                .size,
        )
    }

    @Test
    fun `a new game starts with two tiles`() {
        val b = Game2048.start(Random(7))
        assertEquals(2, b.tiles.size)
        assertEquals(0, b.score)
    }

    @Test
    fun `a full board without equal neighbours is game over`() {
        val b = board("2 4 2 4", "4 2 4 2", "2 4 2 4", "4 2 4 2")
        assertFalse(Game2048.canMove(b))
        val c = board("2 4 2 4", "4 2 4 2", "2 4 2 4", "4 2 4 4")
        assertTrue(Game2048.canMove(c))
    }

    @Test
    fun `2048 wins and ids stay unique`() {
        val b = board("1024 1024 . .", ". . . .", ". . . .", ". . . .")
        val result = Game2048.move(b, Direction.LEFT)
        assertTrue(Game2048.hasWon(result.board))
        val ids = result.board.tiles.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `tile colours go from light to full green`() {
        assertEquals(tileColors(2).first, tileColors(1).first)
        assertTrue(tileColors(2048).first != tileColors(2).first)
    }
}
