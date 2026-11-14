package com.eduludo.Model.dice;

import java.util.Random;

/**
 * Strategy pattern: every die type implements the same interface, so the game engine
 * never needs to know which die it is rolling.
 */
public interface DiceStrategy {

    /** Rolls the die and returns a face value between 1 and {@link #faces()} (inclusive), all equally likely. */
    int roll(Random rng);

    /** Number of faces on this die. */
    int faces();

    /** Name shown to the player. */
    String name();
}

