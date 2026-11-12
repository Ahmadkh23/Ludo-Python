package com.eduludo.Model;

/** Question difficulty. A correct answer moves the piece forward {@link #steps()} squares, a wrong one backward. */
public enum QuestionLevel {
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3);

    private final String displayName;
    private final int steps;

    QuestionLevel(String displayName, int steps) {
        this.displayName = displayName;
        this.steps = steps;
    }

    public String displayName() {
        return displayName;
    }

    public int steps() {
        return steps;
    }
}

