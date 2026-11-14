package com.eduludo.Model;

/**
 * A legal move the current player may choose after a roll.
 *
 * @param piece  the piece to move
 * @param kind   ENTER (castle to START square, needs a 6 on the normal die) or MOVE (advance on the path)
 * @param target the path index the piece will land on
 */
public record MoveOption(Piece piece, Kind kind, int target) {

    public enum Kind { ENTER, MOVE }
}

