package com.eduludo.Model;

/**
 * Supplies a random question of a given level to the game.
 * The game only ever sees this narrow interface, never the full (protected) question bank.
 */
@FunctionalInterface
public interface QuestionProvider {
    Question draw(QuestionLevel level);
}

