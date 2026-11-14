package com.eduludo.Model.dice;

import java.util.Random;

/** The regular step die: 1 to 6, each with equal probability. */
public class NormalDice implements DiceStrategy {

    @Override
    public int roll(Random rng) {
        return rng.nextInt(faces()) + 1;
    }

    @Override
    public int faces() {
        return 6;
    }

    @Override
    public String name() {
        return "Normal";
    }
}

