package com.eduludo.Model.dice;

import com.eduludo.testing.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertTrue;

/** The three die strategies and the {@link DiceRoll} value they produce. */
public class DiceStrategyTest {

    @Test
    void normalDiceHasSixFacesAllReachable() {
        NormalDice dice = new NormalDice();
        assertEquals(6, dice.faces(), "normal faces");
        assertEquals("Normal", dice.name(), "normal name");
        assertRollsCoverExactly(dice, 1, 6);
    }

    @Test
    void boostDiceHasThreeFacesAllReachable() {
        BoostDice dice = new BoostDice();
        assertEquals(3, dice.faces(), "boost faces");
        assertEquals("Boost", dice.name(), "boost name");
        assertRollsCoverExactly(dice, 1, 3);
    }

    @Test
    void ultraBoostDiceHasSixFacesAllReachable() {
        UltraBoostDice dice = new UltraBoostDice();
        assertEquals(6, dice.faces(), "ultra boost faces");
        assertEquals("UltraBoost", dice.name(), "ultra boost name");
        assertRollsCoverExactly(dice, 1, 6);
    }

    @Test
    void rollNeverExceedsTheDiesFaceCount() {
        NormalDice normal = new NormalDice();
        BoostDice boost = new BoostDice();
        UltraBoostDice ultra = new UltraBoostDice();
        Random rng = new Random(7);
        for (int i = 0; i < 500; i++) {
            assertTrue(normal.roll(rng) >= 1 && normal.roll(rng) <= 6, "normal in range");
            assertTrue(boost.roll(rng) >= 1 && boost.roll(rng) <= 3, "boost in range");
            assertTrue(ultra.roll(rng) >= 1 && ultra.roll(rng) <= 6, "ultra in range");
        }
    }

    @Test
    void diceRollWithoutAMultiplierReportsNoMultiplierAndStepsEqualNormal() {
        DiceRoll roll = new DiceRoll(4, 0, null, 4, false);
        assertFalse(roll.hasMultiplier(), "no multiplier die was thrown");
        assertEquals(1, roll.multiplier(), "multiplier defaults to x1 without a multiplier die");
        assertFalse(roll.isSix(), "4 is not a six");
        assertEquals("4 steps", roll.describe(), "plain roll description");
    }

    @Test
    void diceRollWithAMultiplierComputesTheRightStepsAndDescription() {
        DiceRoll roll = new DiceRoll(5, 3, "Boost", 10, false);
        assertTrue(roll.hasMultiplier(), "a multiplier die was thrown");
        assertEquals(2, roll.multiplier(), "face 3 -> x2 multiplier");
        assertEquals(10, roll.steps(), "5 x 2 = 10 steps");
        assertEquals("5 x Boost(3 -> x2) = 10 steps", roll.describe(), "multiplier roll description");
    }

    @Test
    void diceRollSixIsDetectedOnlyFromTheNormalFace() {
        assertTrue(new DiceRoll(6, 0, null, 6, false).isSix(), "normal face of 6 is a six");
        assertFalse(new DiceRoll(5, 6, "UltraBoost", 25, true).isSix(), "a multiplier face of 6 is not itself a six");
    }

    @Test
    void oneStepIsDescribedInSingular() {
        assertEquals("1 step", new DiceRoll(1, 0, null, 1, false).describe(), "singular step wording");
    }

    /** Rolls the die many times and checks every face from min to max actually comes up. */
    private static void assertRollsCoverExactly(DiceStrategy dice, int min, int max) {
        Random rng = new Random(42);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            int face = dice.roll(rng);
            assertTrue(face >= min && face <= max, dice.name() + " face in range: " + face);
            seen.add(face);
        }
        for (int face = min; face <= max; face++) {
            assertTrue(seen.contains(face), dice.name() + " should eventually roll " + face);
        }
        assertEquals(max - min + 1, seen.size(), dice.name() + " should not roll outside its face count");
    }
}

