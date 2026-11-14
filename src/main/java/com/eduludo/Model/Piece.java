package com.eduludo.Model;

/**
 * One of a player's four pieces.
 * Its position is an index into the owner's path: -1 means "in the castle", the last index means "finished".
 */
public final class Piece {

    public static final int IN_CASTLE = -1;

    private final Player owner;
    private final int number;
    private final int finishIndex;
    private int position = IN_CASTLE;

    Piece(Player owner, int number, int finishIndex) {
        this.owner = owner;
        this.number = number;
        this.finishIndex = finishIndex;
    }

    public Player owner() {
        return owner;
    }

    /** 1 to 4, for display. */
    public int number() {
        return number;
    }

    public int position() {
        return position;
    }

    void setPosition(int position) {
        if (position < IN_CASTLE || position > finishIndex) {
            throw new IllegalArgumentException("Position out of range: " + position);
        }
        this.position = position;
    }

    public boolean isInCastle() {
        return position == IN_CASTLE;
    }

    public boolean isFinished() {
        return position == finishIndex;
    }

    /** On the board and still able to move (not in the castle, not finished). */
    public boolean isActive() {
        return !isInCastle() && !isFinished();
    }

    @Override
    public String toString() {
        return owner.name() + " #" + number;
    }
}

