package com.eduludo.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A participant: name, unique colour, four pieces, and whether the computer plays for them. */
public final class Player {

    public static final int PIECES_PER_PLAYER = 4;

    private final String name;
    private final PlayerColor color;
    private final boolean computer;
    private final List<Piece> pieces = new ArrayList<>(PIECES_PER_PLAYER);
    private boolean skipNextTurn;

    Player(String name, PlayerColor color, boolean computer, int finishIndex) {
        this.name = name;
        this.color = color;
        this.computer = computer;
        for (int i = 1; i <= PIECES_PER_PLAYER; i++) {
            pieces.add(new Piece(this, i, finishIndex));
        }
    }

    public String name() {
        return name;
    }

    public PlayerColor color() {
        return color;
    }

    /** True if the computer plays this seat (bonus feature). */
    public boolean isComputer() {
        return computer;
    }

    public List<Piece> pieces() {
        return Collections.unmodifiableList(pieces);
    }

    public Piece piece(int number) {
        return pieces.get(number - 1);
    }

    /** All four pieces have reached the finish. */
    public boolean hasFinished() {
        return pieces.stream().allMatch(Piece::isFinished);
    }

    public long finishedPieces() {
        return pieces.stream().filter(Piece::isFinished).count();
    }

    public boolean hasActivePieces() {
        return pieces.stream().anyMatch(Piece::isActive);
    }

    public boolean isSkipNextTurn() {
        return skipNextTurn;
    }

    void setSkipNextTurn(boolean skipNextTurn) {
        this.skipNextTurn = skipNextTurn;
    }

    @Override
    public String toString() {
        return name + " (" + color.displayName() + ")";
    }
}

