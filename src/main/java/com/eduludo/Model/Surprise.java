package com.eduludo.Model;

import java.util.Random;

/**
 * Strategy pattern: each surprise is a separate effect with its own probability (weights sum to 100),
 * exactly as listed in the specification. {@link #apply} returns true when the game must wait for the
 * player's input before the turn can continue.
 */
public enum Surprise {

    /** 10% - the piece jumps straight to the finish. */
    ROCKET_TO_FINISH(10, "Rocket! The piece jumps straight to the finish.") {
        @Override
        boolean apply(Game game, Piece piece) {
            game.surpriseSendToFinish(piece);
            return false;
        }
    },
    /** 10% - choose an opponent's piece, roll the normal die, move that piece back half the result. */
    MOVE_OPPONENT(10, "Sabotage! Choose an opponent's piece. It moves back half of a normal die roll.") {
        @Override
        boolean apply(Game game, Piece piece) {
            return game.surpriseRequestOpponent(piece);
        }
    },
    /** 10% - the piece that landed returns to its castle. */
    BACK_TO_CASTLE(10, "Oops! The piece goes back to its castle.") {
        @Override
        boolean apply(Game game, Piece piece) {
            game.surpriseSendToCastle(piece);
            return false;
        }
    },
    /** 30% - the piece moves back 3 squares. */
    BACK_THREE(30, "Slip! The piece moves back 3 squares.") {
        @Override
        boolean apply(Game game, Piece piece) {
            game.surpriseMoveBack(piece, 3);
            return false;
        }
    },
    /** 30% - the player loses their next turn. */
    LOSE_NEXT_TURN(30, "Sleepy! You lose your next turn.") {
        @Override
        boolean apply(Game game, Piece piece) {
            game.surpriseLoseNextTurn(piece.owner());
            return false;
        }
    },
    /** 10% - a bonus roll with the UltraBoost die + normal die (does not count toward the three-sixes rule). */
    ULTRA_BONUS_ROLL(10, "Jackpot! You get a bonus roll with the UltraBoost die.") {
        @Override
        boolean apply(Game game, Piece piece) {
            game.surpriseGrantBonusRoll();
            return false;
        }
    };

    private final int weightPercent;
    private final String description;

    Surprise(int weightPercent, String description) {
        this.weightPercent = weightPercent;
        this.description = description;
    }

    public int weightPercent() {
        return weightPercent;
    }

    public String description() {
        return description;
    }

    /** Applies the effect to the piece that landed on the surprise square. Returns true if input is needed. */
    abstract boolean apply(Game game, Piece piece);

    /** Draws a surprise according to the weights (10/10/10/30/30/10). */
    public static Surprise draw(Random rng) {
        int roll = rng.nextInt(100);
        int acc = 0;
        for (Surprise s : values()) {
            acc += s.weightPercent;
            if (roll < acc) {
                return s;
            }
        }
        throw new IllegalStateException("Surprise weights must sum to 100");
    }
}

