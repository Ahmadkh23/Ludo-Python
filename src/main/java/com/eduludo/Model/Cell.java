package com.eduludo.Model;

/** An immutable (row, column) coordinate on the square board grid. */
public record Cell(int row, int col) {

    /** Rotates this cell 90 degrees clockwise inside an n x n grid. */
    public Cell rotate(int n) {
        return new Cell(col, n - 1 - row);
    }

    /** Rotates this cell clockwise {@code times} quarter-turns inside an n x n grid. */
    public Cell rotate(int n, int times) {
        Cell c = this;
        for (int i = 0; i < times; i++) {
            c = c.rotate(n);
        }
        return c;
    }

    /** Moves this cell diagonally by {@code d} (used to nest a smaller board inside outer rings). */
    public Cell shift(int d) {
        return new Cell(row + d, col + d);
    }

    /** True if the other cell is one of the 8 neighbours of this cell. */
    public boolean touches(Cell other) {
        return Math.max(Math.abs(row - other.row), Math.abs(col - other.col)) == 1;
    }
}

