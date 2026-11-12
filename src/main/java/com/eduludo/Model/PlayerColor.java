package com.eduludo.Model;

/**
 * The four player colours, in clockwise seating order.
 * The ordinal is the number of quarter-turns used to rotate the red layout to this colour's side.
 */
public enum PlayerColor {
    RED("Red"),
    GREEN("Green"),
    YELLOW("Yellow"),
    BLUE("Blue");

    private final String displayName;

    PlayerColor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Number of clockwise quarter-turns from the red side of the board. */
    public int quarterTurns() {
        return ordinal();
    }
}

