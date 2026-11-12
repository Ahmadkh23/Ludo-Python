package com.eduludo.Model;

import com.eduludo.Model.dice.BoostDice;
import com.eduludo.Model.dice.UltraBoostDice;
import com.eduludo.testing.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Question validation, level settings, colours and the game record. */
public class ModelValuesTest {

    private static final List<String> ANSWERS = List.of("Scrum", "Kanban", "XP", "Waterfall");

    @Test
    void aValidQuestionIsTrimmedAndKnowsItsCorrectAnswer() {
        Question q = new Question(1, QuestionLevel.MEDIUM, "  Agile ", "  Which is iterative? ",
                List.of(" Scrum ", "Kanban", "XP", "Waterfall"), 0);
        assertEquals("Agile", q.topic(), "topic trimmed");
        assertEquals("Which is iterative?", q.text(), "text trimmed");
        assertEquals("Scrum", q.options().get(0), "answers trimmed");
        assertTrue(q.isCorrect(0), "index 0 is correct");
        assertFalse(q.isCorrect(3), "index 3 is wrong");
        assertEquals("", new Question(1, QuestionLevel.EASY, null, "Q?", ANSWERS, 1).topic(), "null topic becomes empty");
    }

    @Test
    void invalidQuestionsAreRejectedByTheRecord() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "   ", ANSWERS, 0), "empty text");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q?", List.of("a", "b", "c"), 0), "only three answers");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q?", List.of("a", "b", "c", " "), 0), "blank answer");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q?", List.of("a", "b", "c", "A"), 0), "duplicate answers ignoring case");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q?", ANSWERS, 4), "correct index too big");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q?", ANSWERS, -1), "negative correct index");
        assertThrows(NullPointerException.class,
                () -> new Question(1, null, "t", "Q?", ANSWERS, 0), "level is required");
    }

    @Test
    void questionAnswersCannotBeChangedAfterCreation() {
        List<String> answers = new ArrayList<>(ANSWERS);
        Question q = new Question(1, QuestionLevel.EASY, "t", "Q?", answers, 0);
        answers.set(0, "Changed");
        assertEquals("Scrum", q.options().get(0), "later changes to the input list do not leak in");
        assertThrows(UnsupportedOperationException.class, () -> q.options().add("x"), "answers list is read-only");
    }

    @Test
    void withIdKeepsEverythingElse() {
        Question q = new Question(1, QuestionLevel.HARD, "QA", "Q?", ANSWERS, 2);
        Question copy = q.withId(99);
        assertEquals(99, copy.id(), "new id");
        assertEquals(q.withId(1), q, "only the id differs");
    }

    @Test
    void questionLevelsMoveOneTwoOrThreeSteps() {
        assertEquals(1, QuestionLevel.EASY.steps(), "easy");
        assertEquals(2, QuestionLevel.MEDIUM.steps(), "medium");
        assertEquals(3, QuestionLevel.HARD.steps(), "hard");
    }

    @Test
    void difficultySettingsMatchTheSpecification() {
        assertEquals(15, Difficulty.EASY.gridSize(), "easy grid");
        assertEquals(17, Difficulty.MEDIUM.gridSize(), "medium grid");
        assertEquals(19, Difficulty.HARD.gridSize(), "hard grid");
        assertEquals(4, Difficulty.EASY.arrowSquares(), "easy arrows");
        assertEquals(8, Difficulty.MEDIUM.arrowSquares(), "medium arrows");
        assertEquals(12, Difficulty.HARD.arrowSquares(), "hard arrows");
        assertEquals(0, Difficulty.EASY.questionsPerLevel() + Difficulty.EASY.surpriseSquares(), "easy has no questions or surprises");
    }

    @Test
    void eachDifficultyUsesTheRightMultiplierDie() {
        assertEquals(null, Difficulty.EASY.createMultiplierDie(), "easy has no multiplier die");
        assertTrue(Difficulty.MEDIUM.createMultiplierDie() instanceof BoostDice, "medium uses Boost");
        assertTrue(Difficulty.HARD.createMultiplierDie() instanceof UltraBoostDice, "hard uses UltraBoost");
        assertFalse(Difficulty.MEDIUM.createMultiplierDie() == Difficulty.MEDIUM.createMultiplierDie(), "a new die each time");
    }

    @Test
    void coloursAreSeatedClockwiseFromRed() {
        assertEquals(0, PlayerColor.RED.quarterTurns(), "red");
        assertEquals(1, PlayerColor.GREEN.quarterTurns(), "green");
        assertEquals(2, PlayerColor.YELLOW.quarterTurns(), "yellow");
        assertEquals(3, PlayerColor.BLUE.quarterTurns(), "blue");
    }

    @Test
    void durationsAreFormattedAsMinutesOrHours() {
        assertEquals("00:00:00", GameRecord.formatSeconds(0), "zero");
        assertEquals("00:00:59", GameRecord.formatSeconds(59), "under a minute");
        assertEquals("00:59:59", GameRecord.formatSeconds(3599), "just under an hour");
        assertEquals("01:00:00", GameRecord.formatSeconds(3600), "exactly one hour");
        assertEquals("10:05:07", GameRecord.formatSeconds(36307), "ten hours");
    }

    @Test
    void theGameRecordNamesTheWinnerAndCopiesTheRanking() {
        List<GameRecord.RankedPlayer> ranking = new ArrayList<>(List.of(
                new GameRecord.RankedPlayer(1, "Lia", PlayerColor.GREEN, false),
                new GameRecord.RankedPlayer(2, "Bot", PlayerColor.RED, true)));
        GameRecord record = new GameRecord("g1", LocalDateTime.of(2026, 11, 1, 12, 0), Difficulty.MEDIUM, 125, ranking);
        ranking.clear();
        assertEquals(2, record.ranking().size(), "ranking copied on creation");
        assertEquals("Lia", record.winnerName(), "winner is first place");
        assertEquals("00:02:05", record.formattedDuration(), "duration");
        assertEquals("", new GameRecord("g2", LocalDateTime.now(), Difficulty.EASY, 0, List.of()).winnerName(),
                "no winner for an empty ranking");
    }
}

