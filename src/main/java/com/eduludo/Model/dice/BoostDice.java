package com.eduludo.Model.dice;

import java.util.Random;

/**
 * Boost die (MEDIUM level): faces 1 to 3, equal probability.
 * Rolling N multiplies the normal die by (N - 1), so the multiplier is 0, 1 or 2.
 */
public class BoostDice implements DiceStrategy {

    @Override
    public int roll(Random rng) {
        return rng.nextInt(faces()) + 1;
    }

    @Override
    public int faces() {
        return 3;
    }

    @Override
    public String name() {
        return "Boost";
    }
}

