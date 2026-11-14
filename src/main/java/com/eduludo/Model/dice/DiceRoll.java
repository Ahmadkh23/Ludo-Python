package com.eduludo.Model.dice;

/**
 * Result of one throw.
 *
 * @param normal         value of the normal step die (1 to 6)
 * @param multiplierFace face of the Boost/UltraBoost die, or 0 when no multiplier die was thrown
 * @param multiplierName name of the multiplier die, or {@code null}
 * @param steps          final number of steps: normal x (face - 1), or just normal without a multiplier die
 * @param bonus          true for the surprise bonus roll (it does not count toward the "three sixes" rule)
 */
public record DiceRoll(int normal, int multiplierFace, String multiplierName, int steps, boolean bonus) {

    public boolean hasMultiplier() {
        return multiplierFace > 0;
    }

    public int multiplier() {
        return hasMultiplier() ? multiplierFace - 1 : 1;
    }

    public boolean isSix() {
        return normal == 6;
    }

    /** Human-readable description, e.g. "5 x Boost(3 -> x2) = 10 steps". */
    public String describe() {
        if (!hasMultiplier()) {
            return normal + (normal == 1 ? " step" : " steps");
        }
        return normal + " x " + multiplierName + "(" + multiplierFace + " -> x" + multiplier() + ") = " + steps
                + (steps == 1 ? " step" : " steps");
    }
}

