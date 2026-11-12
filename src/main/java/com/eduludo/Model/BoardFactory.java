package com.eduludo.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Factory pattern: builds the EASY, MEDIUM or HARD board.
 * <p>
 * Geometry (row, col with (0,0) at the top-left):
 * <ul>
 *   <li>The classic 15x15 cross holds the 52-square loop and four 6-square home paths.</li>
 *   <li>MEDIUM wraps it in one ring of 64 squares (17x17 grid). HARD adds a second ring of 72 squares (19x19).</li>
 *   <li>Red's castle is the top-left corner. The other colours are the same layout rotated clockwise
 *       (Green top-right, Yellow bottom-right, Blue bottom-left), so the board is fair for everyone.</li>
 *   <li>START (#): EASY uses the classic start square on the loop. MEDIUM/HARD put it on the outermost ring,
 *       in the corner next to the player's castle.</li>
 *   <li>ARROWS: the last square of a player's lap on each track. From there the path continues onto the next
 *       inner track (ring to ring, ring to loop, loop to home path).</li>
 *   <li>SAFE, QUESTION and SURPRISE squares are placed at random on the shared tracks at the start of every game.</li>
 * </ul>
 */
public final class BoardFactory {

    /** Red's start square on the classic loop is index 0. */
    private static final List<Cell> LOOP = buildClassicLoop();

    private BoardFactory() {
    }

    /** Builds a board with random SAFE / QUESTION / SURPRISE squares, as required at the start of each game. */
    public static Board create(Difficulty difficulty, Random rng) {
        Board board = createLayout(difficulty);
        placeRandomSquares(board, difficulty, rng);
        return board;
    }

    /** Builds only the fixed layout (tracks, START, ARROW and HOME squares) with no random squares. */
    public static Board createLayout(Difficulty difficulty) {
        int n = difficulty.gridSize();
        int rings = difficulty.outerRings();

        // 1. Red's path, its arrows and its home path, in grid coordinates.
        List<Cell> redPath = new ArrayList<>();
        List<Cell> redArrows = new ArrayList<>();
        List<Cell> trackCells = new ArrayList<>();

        for (int k = 0; k < rings; k++) {
            List<Cell> ring = perimeter(k, n);
            trackCells.addAll(ring);
            if (k < rings - 1) {
                // Full lap of an outer ring; it ends next to the corner where the next ring starts.
                redPath.addAll(ring);
                redArrows.add(ring.get(ring.size() - 1));
            } else {
                // Innermost ring: walk until the square next to the loop entry.
                Cell arrow = new Cell(k + 7, k);
                int idx = ring.indexOf(arrow);
                redPath.addAll(ring.subList(0, idx + 1));
                redArrows.add(arrow);
            }
        }

        List<Cell> loop = LOOP.stream().map(c -> c.shift(rings)).toList();
        trackCells.addAll(loop);
        if (rings > 0) {
            redPath.add(loop.get(51)); // enter the loop just before red's classic start square
        }
        redPath.addAll(loop.subList(0, 51));
        redArrows.add(loop.get(50)); // turn into the home path

        List<Cell> redHome = new ArrayList<>();
        for (int c = 1; c <= 6; c++) {
            redHome.add(new Cell(7, c).shift(rings));
        }
        redPath.addAll(redHome);

        Board board = new Board(difficulty, n, trackCells);

        // 2. Every track cell starts as a regular square.
        for (Cell c : trackCells) {
            board.addSquare(new Square(c, SquareType.REGULAR, null));
        }

        // 3. Rotate red's layout for each colour.
        for (PlayerColor color : PlayerColor.values()) {
            int turns = color.quarterTurns();
            List<Cell> path = redPath.stream().map(c -> c.rotate(n, turns)).toList();
            board.setPath(color, path);

            board.square(path.get(0)).makeOwned(SquareType.START, color);
            for (Cell a : redArrows) {
                board.square(a.rotate(n, turns)).makeOwned(SquareType.ARROW, color);
            }
            for (Cell h : redHome) {
                Cell hc = h.rotate(n, turns);
                board.addSquare(new Square(hc, SquareType.HOME, color));
            }

            // Castle = the 6x6 corner block of the classic board; rotate its centre point.
            double r = rings + 3.0;
            double col = rings + 3.0;
            for (int t = 0; t < turns; t++) {
                double nr = col;
                double nc = n - r;
                r = nr;
                col = nc;
            }
            board.setCastleCenter(color, r, col);
        }
        return board;
    }

    /**
     * Randomly places SAFE, QUESTION (per level) and SURPRISE squares on regular squares of the shared tracks.
     * START and ARROW positions are fixed and never replaced.
     */
    static void placeRandomSquares(Board board, Difficulty difficulty, Random rng) {
        List<Cell> free = new ArrayList<>();
        for (Cell c : board.trackCells()) {
            if (board.square(c).type() == SquareType.REGULAR) {
                free.add(c);
            }
        }
        Collections.shuffle(free, rng);
        int i = 0;
        for (int s = 0; s < difficulty.safeSquares(); s++) {
            board.square(free.get(i++)).makeSafe();
        }
        for (QuestionLevel level : QuestionLevel.values()) {
            for (int q = 0; q < difficulty.questionsPerLevel(); q++) {
                board.square(free.get(i++)).makeQuestion(level);
            }
        }
        for (int s = 0; s < difficulty.surpriseSquares(); s++) {
            board.square(free.get(i++)).makeSurprise();
        }
    }

    /** Clockwise perimeter of the ring at offset k inside an n x n grid, starting at its top-left corner. */
    static List<Cell> perimeter(int k, int n) {
        int s = n - 2 * k;
        List<Cell> ring = new ArrayList<>(4 * s - 4);
        int last = k + s - 1;
        for (int c = k; c <= last; c++) {
            ring.add(new Cell(k, c));
        }
        for (int r = k + 1; r <= last; r++) {
            ring.add(new Cell(r, last));
        }
        for (int c = last - 1; c >= k; c--) {
            ring.add(new Cell(last, c));
        }
        for (int r = last - 1; r >= k + 1; r--) {
            ring.add(new Cell(r, k));
        }
        return ring;
    }

    /** The classic 52-square clockwise loop of a 15x15 Ludo board, starting at red's start square (6,1). */
    private static List<Cell> buildClassicLoop() {
        List<Cell> loop = new ArrayList<>(52);
        for (int c = 1; c <= 5; c++) loop.add(new Cell(6, c));
        for (int r = 5; r >= 0; r--) loop.add(new Cell(r, 6));
        loop.add(new Cell(0, 7));
        for (int r = 0; r <= 5; r++) loop.add(new Cell(r, 8));
        for (int c = 9; c <= 14; c++) loop.add(new Cell(6, c));
        loop.add(new Cell(7, 14));
        for (int c = 14; c >= 9; c--) loop.add(new Cell(8, c));
        for (int r = 9; r <= 14; r++) loop.add(new Cell(r, 8));
        loop.add(new Cell(14, 7));
        for (int r = 14; r >= 9; r--) loop.add(new Cell(r, 6));
        for (int c = 5; c >= 0; c--) loop.add(new Cell(8, c));
        loop.add(new Cell(7, 0));
        loop.add(new Cell(6, 0));
        Set<Cell> unique = new LinkedHashSet<>(loop);
        if (loop.size() != 52 || unique.size() != 52) {
            throw new IllegalStateException("Classic loop must have 52 unique squares");
        }
        return List.copyOf(loop);
    }
}

