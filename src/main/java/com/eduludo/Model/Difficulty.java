package com.eduludo.Model;

import com.eduludo.Model.dice.BoostDice;
import com.eduludo.Model.dice.DiceStrategy;
import com.eduludo.Model.dice.UltraBoostDice;

/**
 * Game difficulty as defined in the specification.
 * <ul>
 *   <li>EASY   - 52-square track + home paths. 4 safe, 4 arrows. Normal die only.</li>
 *   <li>MEDIUM - adds a 64-square outer ring. 8 safe, 8 arrows, 6 questions, 2 surprises. Normal + Boost die.</li>
 *   <li>HARD   - adds another 72-square ring. 12 safe, 12 arrows, 9 questions, 3 surprises. Normal + UltraBoost die.</li>
 * </ul>
 */
public enum Difficulty {
    EASY("Easy", 0, 4, 0, 0),
    MEDIUM("Medium", 1, 8, 2, 2),
    HARD("Hard", 2, 12, 3, 3);

    private final String displayName;
    private final int outerRings;
    private final int safeSquares;
    private final int questionsPerLevel;
    private final int surpriseSquares;

    Difficulty(String displayName, int outerRings, int safeSquares, int questionsPerLevel, int surpriseSquares) {
        this.displayName = displayName;
        this.outerRings = outerRings;
        this.safeSquares = safeSquares;
        this.questionsPerLevel = questionsPerLevel;
        this.surpriseSquares = surpriseSquares;
    }

    public String displayName() {
        return displayName;
    }

    /** Number of extra rings around the classic 15x15 board. */
    public int outerRings() {
        return outerRings;
    }

    /** Side length of the square grid. */
    public int gridSize() {
        return 15 + 2 * outerRings;
    }

    public int safeSquares() {
        return safeSquares;
    }

    public int questionsPerLevel() {
        return questionsPerLevel;
    }

    public int surpriseSquares() {
        return surpriseSquares;
    }

    /** One arrow per player per track transition (ring to ring, ring to track, track to home path). */
    public int arrowSquares() {
        return 4 * (outerRings + 1);
    }

    /** The extra multiplier die used at this level, or {@code null} on EASY. */
    public DiceStrategy createMultiplierDie() {
        return switch (this) {
            case EASY -> null;
            case MEDIUM -> new BoostDice();
            case HARD -> new UltraBoostDice();
        };
    }
}

