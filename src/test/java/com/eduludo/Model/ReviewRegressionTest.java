package com.eduludo.Model;

import com.eduludo.testing.Test;

import java.util.List;
import java.util.Random;

import static com.eduludo.testing.Assert.assertEquals;

/** Regression tests for defects found in independent review. */
public class ReviewRegressionTest {

    private Game game(Board b, ScriptedDice normal, ScriptedDice mult, Random random) {
        return Game.builder()
                .board(b)
                .normalDie(normal)
                .multiplierDie(mult)
                .random(random)
                .questions(level -> new Question(999, level, "Review", "Test?", List.of("A", "B", "C", "D"), 0))
                .addPlayer("Alice", PlayerColor.RED, false)
                .addPlayer("Bob", PlayerColor.GREEN, false)
                .build();
    }

    @Test
    public void blockedOnlyMoveMustStopBeforeBlock() {
        Board b = BoardFactory.createLayout(Difficulty.EASY);
        Game g = game(b, new ScriptedDice(6, "Normal").then(4), null, new Random(1));
        Piece p = g.players().get(0).piece(1);
        p.setPosition(7);
        int other = b.path(PlayerColor.GREEN).indexOf(b.path(PlayerColor.RED).get(10));
        g.players().get(1).piece(1).setPosition(other);
        g.players().get(1).piece(2).setPosition(other);

        g.roll();

        assertEquals(1, g.options().size(), "only legal partial move must be offered");
        assertEquals(9, g.options().get(0).target(), "stop before block");
    }

    @Test
    public void homeLaneUsesNormalWhenMultiplierZero() {
        Board b = BoardFactory.createLayout(Difficulty.MEDIUM);
        Game g = game(b, new ScriptedDice(6, "Normal").then(2), new ScriptedDice(3, "Boost").then(1), new Random(1));
        g.players().get(0).piece(1).setPosition(b.finishIndex() - 3);

        g.roll();

        assertEquals(1, g.options().size(), "normal die home move must remain possible");
        assertEquals(b.finishIndex() - 1, g.options().get(0).target(), "two steps");
    }

    @Test
    public void oddOpponentSurpriseRoundsUp() {
        Board b = BoardFactory.createLayout(Difficulty.EASY);
        b.square(b.path(PlayerColor.RED).get(10)).makeSurprise();
        Random random = new Random(1) {
            private static final long serialVersionUID = 1L;

            @Override
            public int nextInt(int bound) {
                return Math.min(15, bound - 1);
            }
        };
        Game g = game(b, new ScriptedDice(6, "Normal").then(1, 5), null, random);
        g.players().get(0).piece(1).setPosition(9);
        Piece other = g.players().get(1).piece(1);
        other.setPosition(20);

        g.roll();
        g.chooseMoveFor(g.players().get(0).piece(1));

        assertEquals(Phase.CHOOSING_OPPONENT, g.phase(), "surprise selection");
        assertEquals(3, g.chooseOpponentPiece(other), "ceil(5/2)");
    }
}
