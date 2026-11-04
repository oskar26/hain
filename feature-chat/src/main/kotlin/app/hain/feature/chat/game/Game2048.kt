package app.hain.feature.chat.game

import kotlin.random.Random

/** One tile on the 2048 board. [id] stays the same while it slides, so the UI can animate it. */
data class Tile(
    val id: Int,
    val value: Int,
    val row: Int,
    val col: Int,
    /** Just formed by a merge (pops in the UI). */
    val merged: Boolean = false,
    /** Just spawned (grows in the UI). */
    val fresh: Boolean = false,
)

/** The whole game state (pure, immutable). */
data class Board(
    val tiles: List<Tile>,
    val score: Int = 0,
    val nextId: Int = 0,
) {
    fun at(
        row: Int,
        col: Int,
    ): Tile? = tiles.firstOrNull { it.row == row && it.col == col }

    val maxTile: Int get() = tiles.maxOfOrNull { it.value } ?: 0
}

enum class Direction { LEFT, RIGHT, UP, DOWN }

/** What a move did: the new board, whether anything moved, and the values that merged. */
data class MoveResult(
    val board: Board,
    val moved: Boolean,
    val gained: Int,
    val merges: List<Int>,
)

/**
 * Hain's own 2048 (T-098, the easter egg behind Pim): 4×4 board, slide, merge equal neighbours once per
 * move, spawn a 2 (90 %) or a 4 (10 %) after every real move. Pure and unit-tested.
 */
object Game2048 {
    const val SIZE = 4
    const val GOAL = 2048
    private const val FOUR_CHANCE = 0.1f
    private const val TWO = 2
    private const val FOUR = 4

    fun empty(): Board = Board(emptyList())

    fun start(random: Random = Random): Board = spawn(spawn(empty(), random), random)

    /** Puts a new tile on a random free cell (or returns [board] when it is full). */
    fun spawn(
        board: Board,
        random: Random = Random,
    ): Board {
        val cells = (0 until SIZE).flatMap { r -> (0 until SIZE).map { c -> r to c } }
        val free = cells.filter { (r, c) -> board.at(r, c) == null }
        if (free.isEmpty()) return board
        val (r, c) = free[random.nextInt(free.size)]
        val value = if (random.nextFloat() < FOUR_CHANCE) FOUR else TWO
        return board.copy(
            tiles = board.tiles + Tile(board.nextId, value, r, c, fresh = true),
            nextId = board.nextId + 1,
        )
    }

    /** Slides every tile towards [dir]; equal neighbours merge once. Does not spawn. */
    fun move(
        board: Board,
        dir: Direction,
    ): MoveResult {
        val calm = board.tiles.map { it.copy(merged = false, fresh = false) }
        val out = mutableListOf<Tile>()
        val merges = mutableListOf<Int>()
        var gained = 0
        repeat(SIZE) { line ->
            // tiles of this line, the one nearest to the wall first
            val inLine =
                calm
                    .filter { if (dir == Direction.LEFT || dir == Direction.RIGHT) it.row == line else it.col == line }
                    .sortedBy { t ->
                        when (dir) {
                            Direction.LEFT -> t.col
                            Direction.RIGHT -> -t.col
                            Direction.UP -> t.row
                            Direction.DOWN -> -t.row
                        }
                    }
            var slot = 0
            var i = 0
            while (i < inLine.size) {
                val a = inLine[i]
                val b = inLine.getOrNull(i + 1)
                val (r, c) = cell(dir, line, slot)
                if (b != null && b.value == a.value) {
                    val value = a.value * 2
                    // the leading tile keeps its id: it slides into place and pops, its partner melts in
                    out += Tile(a.id, value, r, c, merged = true)
                    merges += value
                    gained += value
                    i += 2
                } else {
                    out += a.copy(row = r, col = c)
                    i += 1
                }
                slot++
            }
        }
        val moved =
            merges.isNotEmpty() ||
                out.any { t -> calm.none { it.id == t.id && it.row == t.row && it.col == t.col } }
        val next = Board(out, board.score + gained, board.nextId)
        return MoveResult(if (moved) next else board, moved, gained, merges)
    }

    /** A real move followed by a spawn; [MoveResult.moved] false leaves the board as it was. */
    fun play(
        board: Board,
        dir: Direction,
        random: Random = Random,
    ): MoveResult {
        val result = move(board, dir)
        return if (result.moved) result.copy(board = spawn(result.board, random)) else result
    }

    fun canMove(board: Board): Boolean = Direction.entries.any { move(board, it).moved }

    fun hasWon(board: Board): Boolean = board.maxTile >= GOAL

    private fun cell(
        dir: Direction,
        line: Int,
        slot: Int,
    ): Pair<Int, Int> =
        when (dir) {
            Direction.LEFT -> line to slot
            Direction.RIGHT -> line to SIZE - 1 - slot
            Direction.UP -> slot to line
            Direction.DOWN -> SIZE - 1 - slot to line
        }
}
