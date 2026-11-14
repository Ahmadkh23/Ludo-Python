package com.eduludo.Model;

/**
 * State pattern (enum-based state machine): the phase of the current turn.
 * Each public action of {@link Game} is only allowed in one phase.
 */
public enum Phase {
    /** The current player must roll the dice. */
    WAITING_FOR_ROLL,
    /** The dice were rolled; the player must pick one of {@link Game#options()}. */
    CHOOSING_MOVE,
    /** A piece landed on a question square; the player must answer {@link Game#pendingQuestion()}. */
    ANSWERING_QUESTION,
    /** Surprise "move an opponent": the player must pick an opponent from {@link Game#opponentCandidates()}. */
    CHOOSING_OPPONENT,
    /** All players except one have finished. */
    GAME_OVER
}

