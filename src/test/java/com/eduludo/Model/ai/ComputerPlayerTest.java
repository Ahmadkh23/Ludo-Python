package com.eduludo.Model.ai;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.Game;
import com.eduludo.Model.Phase;
import com.eduludo.Model.PlayerColor;
import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.testing.Test;

import java.util.List;
import java.util.Random;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertTrue;

/** The computer player always makes legal decisions, and full games between computers always finish. */
public class ComputerPlayerTest {

    @Test
    void computerOnlyGamesAlwaysFinishOnEveryLevel() {
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 5; seed++) {
                Random rng = new Random(seed);
                Game g = Game.builder().difficulty(d).random(rng)
                        .questions(level -> new Question(1, level, "t", "q", List.of("a", "b", "c", "d"), 0))
                        .addPlayer("A", PlayerColor.RED, true).addPlayer("B", PlayerColor.GREEN, true)
                        .addPlayer("C", PlayerColor.YELLOW, true).addPlayer("D", PlayerColor.BLUE, true).build();
                ComputerPlayer ai = new ComputerPlayer(rng);
                int actions = 0;
                while (!g.isOver()) {
                    switch (g.phase()) {
                        case WAITING_FOR_ROLL -> g.roll();
                        case CHOOSING_MOVE -> g.choose(ai.chooseMove(g));
                        case ANSWERING_QUESTION -> g.answer(ai.chooseAnswer(g.pendingQuestion()));
                        case CHOOSING_OPPONENT -> g.chooseOpponentPiece(ai.chooseOpponentPiece(g));
                        case GAME_OVER -> { }
                    }
                    actions++;
                    assertTrue(actions < 200_000, d + " seed " + seed + " game must end");
                }
                assertEquals(Phase.GAME_OVER, g.phase(), "over");
                assertEquals(4, g.finishOrder().size(), "all 4 players ranked");
            }
        }
    }

    @Test
    void computerAnswersMatchTheConfiguredAccuracy() {
        ComputerPlayer ai = new ComputerPlayer(new Random(9));
        Question easy = new Question(1, QuestionLevel.EASY, "t", "q", List.of("a", "b", "c", "d"), 2);
        int correct = 0;
        for (int i = 0; i < 10_000; i++) {
            int a = ai.chooseAnswer(easy);
            assertTrue(a >= 0 && a < 4, "valid answer index");
            if (a == 2) {
                correct++;
            }
        }
        assertTrue(Math.abs(correct / 100.0 - 80) < 2, "about 80% correct on easy questions");
    }
}

