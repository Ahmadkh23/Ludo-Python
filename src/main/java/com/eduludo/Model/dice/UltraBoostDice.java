package com.eduludo.Model.dice;

import java.util.Random;

/**
 * UltraBoost die (HARD level and the surprise bonus roll): faces 1 to 6, equal probability.
 * Rolling N multiplies the normal die by (N - 1), so the multiplier is 0 to 5.
 */
public class UltraBoostDice implements DiceStrategy {

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
        return "UltraBoost";
    }
}

