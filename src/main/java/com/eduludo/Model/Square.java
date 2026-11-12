package com.eduludo.Model;

/**
 * One square of the board. Its type decides protection and what happens when a piece lands on it.
 * START, ARROW and HOME squares belong to one colour; QUESTION squares carry a question level.
 */
public final class Square {

    private final Cell cell;
    private SquareType type;
    private PlayerColor owner;
    private QuestionLevel questionLevel;

    Square(Cell cell, SquareType type, PlayerColor owner) {
        this.cell = cell;
        this.type = type;
        this.owner = owner;
    }

    public Cell cell() {
        return cell;
    }

    public SquareType type() {
        return type;
    }

    /** Colour that owns a START / ARROW / HOME square, otherwise {@code null}. */
    public PlayerColor owner() {
        return owner;
    }

    /** Level of a QUESTION square, otherwise {@code null}. */
    public QuestionLevel questionLevel() {
        return questionLevel;
    }

    /** True if pieces standing here cannot be captured. */
    public boolean isProtected() {
        return type.isProtected();
    }

    void makeRegular() {
        this.type = SquareType.REGULAR;
        this.owner = null;
        this.questionLevel = null;
    }

    void makeOwned(SquareType type, PlayerColor owner) {
        this.type = type;
        this.owner = owner;
        this.questionLevel = null;
    }

    void makeSafe() {
        this.type = SquareType.SAFE;
        this.owner = null;
        this.questionLevel = null;
    }

    void makeQuestion(QuestionLevel level) {
        this.type = SquareType.QUESTION;
        this.owner = null;
        this.questionLevel = level;
    }

    void makeSurprise() {
        this.type = SquareType.SURPRISE;
        this.owner = null;
        this.questionLevel = null;
    }

    @Override
    public String toString() {
        return type + (owner != null ? "(" + owner + ")" : "") + (questionLevel != null ? "(" + questionLevel + ")" : "")
                + "@" + cell.row() + "," + cell.col();
    }
}

