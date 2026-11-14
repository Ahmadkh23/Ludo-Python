package com.eduludo.Model;

/**
 * Something that happened in the game. Sent to every {@link GameListener} (Observer pattern),
 * so the views can redraw and log without the model knowing anything about the UI.
 *
 * @param type    what happened
 * @param message a sentence describing it, for the game log
 */
public record GameEvent(Type type, String message) {

    public enum Type {
        GAME_STARTED,
        ROLLED,
        THIRD_SIX,
        NO_MOVES,
        ENTERED,
        MOVED,
        CAPTURED,
        QUESTION_ASKED,
        QUESTION_ANSWERED,
        SURPRISE,
        OPPONENT_MOVED,
        PIECE_FINISHED,
        PLAYER_FINISHED,
        EXTRA_TURN,
        BONUS_ROLL,
        TURN_SKIPPED,
        TURN_CHANGED,
        GAME_OVER
    }
}

