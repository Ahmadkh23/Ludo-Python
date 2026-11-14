package com.eduludo.Model;

/** All square kinds defined by the specification, with their protection rule. */
public enum SquareType {
    /** Standard square: pieces can be captured here. */
    REGULAR(false),
    /** Star square: every piece standing here is protected from capture. */
    SAFE(true),
    /** "#" square where a player's pieces enter the track. Protected. */
    START(true),
    /** Arrow in a player's colour: that player's pieces turn onto the next inner track here. Not protected. */
    ARROW(false),
    /** Coloured home path leading to the finish. Protected. */
    HOME(true),
    /** Question square (easy / medium / hard). Not protected. */
    QUESTION(false),
    /** Surprise square: triggers a random effect. Protected. */
    SURPRISE(true);

    private final boolean protectedSquare;

    SquareType(boolean protectedSquare) {
        this.protectedSquare = protectedSquare;
    }

    public boolean isProtected() {
        return protectedSquare;
    }
}

