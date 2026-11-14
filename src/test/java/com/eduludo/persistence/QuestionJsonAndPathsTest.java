package com.eduludo.persistence;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.testing.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Question JSON conversion, the data folder layout, and history edge cases. */
public class QuestionJsonAndPathsTest {

    private static final List<String> ANSWERS = List.of("Model", "View", "Controller", "Router");

    @Test
    void questionsRoundTripThroughJson() {
        List<Question> qs = List.of(
                new Question(1, QuestionLevel.EASY, "MVC", "Which part shows data?", ANSWERS, 1),
                new Question(2, QuestionLevel.HARD, "", "\"Quoted\" text, with a comma?", ANSWERS, 3));
        assertEquals(qs, QuestionJson.fromJson(QuestionJson.toJson(qs)), "same questions after the round trip");
        assertEquals(List.of(), QuestionJson.fromJson(QuestionJson.toJson(List.of())), "empty list");
    }

    @Test
    void levelNamesAreCaseInsensitiveAndTopicIsOptional() {
        String json = "[{\"id\": 5, \"level\": \"medium\", \"text\": \"Q?\","
                + " \"options\": [\"a\", \"b\", \"c\", \"d\"], \"correct\": 2}]";
        Question q = QuestionJson.fromJson(json).get(0);
        assertEquals(QuestionLevel.MEDIUM, q.level(), "lower-case level accepted");
        assertEquals("", q.topic(), "missing topic becomes empty");
        assertEquals(2, q.correctIndex(), "correct index read");
    }

    @Test
    void theRootMustBeAnArrayOfObjects() {
        assertThrows(IllegalArgumentException.class, () -> QuestionJson.fromJson("{}"), "object root");
        assertThrows(IllegalArgumentException.class, () -> QuestionJson.fromJson("[1, 2]"), "array of numbers");
        assertThrows(IllegalArgumentException.class,
                () -> QuestionJson.fromJson("[{\"id\": 1, \"level\": \"EXTREME\", \"text\": \"Q\","
                        + " \"options\": [\"a\",\"b\",\"c\",\"d\"], \"correct\": 0}]"), "unknown level");
        assertThrows(IllegalArgumentException.class,
                () -> QuestionJson.fromJson("[{\"id\": 1, \"level\": \"EASY\", \"text\": \"Q\","
                        + " \"options\": [\"a\",\"b\",\"c\",\"d\"], \"correct\": 7}]"), "invalid question inside");
    }

    @Test
    void dataFilesLiveInsideTheBaseFolder() {
        Path base = Path.of("some", "folder");
        DataPaths paths = new DataPaths(base);
        assertEquals(base, paths.base(), "base folder");
        assertEquals(base.resolve("questions.dat"), paths.questions(), "questions file");
        assertEquals(base.resolve("history.csv"), paths.history(), "history file");
        assertEquals(base.resolve("admin.properties"), paths.adminCredentials(), "admin file");
    }

    @Test
    void theDataFolderCanBeOverriddenWithASystemProperty() {
        String old = System.getProperty("eduludo.data");
        try {
            System.setProperty("eduludo.data", "custom-dir");
            assertEquals(Path.of("custom-dir"), DataPaths.defaults().base(), "override used");
            System.setProperty("eduludo.data", "  ");
            assertEquals(Path.of(System.getProperty("user.home"), ".eduludo"), DataPaths.defaults().base(),
                    "a blank override falls back to the home folder");
        } finally {
            if (old == null) {
                System.clearProperty("eduludo.data");
            } else {
                System.setProperty("eduludo.data", old);
            }
        }
    }

    @Test
    void historyIsEmptyWhenNoGameWasPlayedYet() throws IOException {
        Path file = Files.createTempDirectory("eduludo-empty").resolve("history.csv");
        assertTrue(new CsvHistoryRepository(file).findAll().isEmpty(), "no file means no games");
        Files.writeString(file, CsvHistoryRepository.HEADER + "\n\n");
        assertTrue(new CsvHistoryRepository(file).findAll().isEmpty(), "header and blank lines only");
    }
}

