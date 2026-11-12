package com.eduludo.Model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The game board: a square grid holding every track square, plus the path that each colour follows.
 * <p>
 * A colour's path is the ordered list of cells its pieces walk: index 0 is the START (#) square and the
 * last index is the finish square at the centre. Arrows are handled by the path itself: when a path
 * reaches its own arrow, the next cell is on the next inner track.
 * <p>
 * Instances are built only by {@link BoardFactory} (Factory pattern).
 */
public final class Board {

    private final Difficulty difficulty;
    private final int size;
    private final Map<Cell, Square> squares = new LinkedHashMap<>();
    private final Map<PlayerColor, List<Cell>> paths = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, double[]> castleCenters = new EnumMap<>(PlayerColor.class);
    private final List<Cell> trackCells;

    Board(Difficulty difficulty, int size, List<Cell> trackCells) {
        this.difficulty = difficulty;
        this.size = size;
        this.trackCells = List.copyOf(trackCells);
    }

    void addSquare(Square square) {
        squares.put(square.cell(), square);
    }

    void setPath(PlayerColor color, List<Cell> path) {
        paths.put(color, List.copyOf(path));
    }

    void setCastleCenter(PlayerColor color, double row, double col) {
        castleCenters.put(color, new double[] {row, col});
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Side length of the grid (15, 17 or 19). */
    public int size() {
        return size;
    }

    public Square square(Cell cell) {
        return squares.get(cell);
    }

    public Collection<Square> squares() {
        return Collections.unmodifiableCollection(squares.values());
    }

    /** The shared tracks (outer rings + the 52-square loop), without the coloured home paths. */
    public List<Cell> trackCells() {
        return trackCells;
    }

    public List<Cell> path(PlayerColor color) {
        return paths.get(color);
    }

    /** Index of the finish square in every path. */
    public int finishIndex() {
        return paths.get(PlayerColor.RED).size() - 1;
    }

    /** Centre of a colour's castle in continuous grid coordinates {row, col} (for drawing). */
    public double[] castleCenter(PlayerColor color) {
        return castleCenters.get(color).clone();
    }

    /** Number of squares of the given type. */
    public long count(SquareType type) {
        return squares.values().stream().filter(s -> s.type() == type).count();
    }

    /** Number of question squares of the given level. */
    public long countQuestions(QuestionLevel level) {
        return squares.values().stream()
                .filter(s -> s.type() == SquareType.QUESTION && s.questionLevel() == level)
                .count();
    }
}

