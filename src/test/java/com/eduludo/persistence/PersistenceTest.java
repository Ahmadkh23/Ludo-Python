package com.eduludo.persistence;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.GameRecord;
import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.security.CryptoBox;
import com.eduludo.testing.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** JSON, encrypted question storage, and the CSV history. */
public class PersistenceTest {

    @Test
    void jsonRoundTripsAllValueTypes() {
        String json = "{\"a\": [1, 2.5, true, false, null, \"x\\\"y\\n\\u00e9\"], \"b\": {}}";
        Object parsed = MiniJson.parse(json);
        Object again = MiniJson.parse(MiniJson.write(parsed));
        assertEquals(parsed, again, "write then parse gives the same value");
        List<?> a = (List<?>) ((Map<?, ?>) parsed).get("a");
        assertEquals("x\"y\né", a.get(5), "escapes decoded");
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\": }"), "invalid JSON rejected");
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("[1, 2] extra"), "trailing text rejected");
    }

    @Test
    void theDefaultQuestionBankIsValidAndLargeEnough() {
        List<Question> qs = QuestionJson.fromJson(EncryptedQuestionRepository.readDefaultResource());
        assertTrue(qs.size() >= 30, "at least 30 questions (found " + qs.size() + ")");
        for (QuestionLevel level : QuestionLevel.values()) {
            long n = qs.stream().filter(q -> q.level() == level).count();
            assertTrue(n >= 10, level + " has at least 10 questions");
        }
        assertEquals((long) qs.size(), qs.stream().map(Question::id).distinct().count(), "ids are unique");
        int[] correctCounts = new int[4];
        qs.forEach(q -> correctCounts[q.correctIndex()]++);
        for (int c : correctCounts) {
            assertTrue(c >= 5, "the correct answer is spread over all 4 positions");
        }
    }

    @Test
    void questionsAreStoredEncryptedAndCreatedFromDefaultsOnFirstRun() throws IOException {
        Path file = Files.createTempDirectory("eduludo-q").resolve("questions.dat");
        CryptoBox box = new CryptoBox("k".toCharArray());
        EncryptedQuestionRepository repo = new EncryptedQuestionRepository(file, box);
        List<Question> first = repo.loadAll();
        assertTrue(Files.exists(file), "file created on first run");
        String raw = Files.readString(file);
        assertFalse(raw.contains("Scrum"), "questions are not readable in the file");
        assertFalse(raw.contains("\"correct\""), "answers are not readable in the file");
        List<Question> second = new EncryptedQuestionRepository(file, box).loadAll();
        assertEquals(first, second, "same questions after reload");
    }

    @Test
    void historyIsSavedAndReadBackNewestFirst() throws IOException {
        Path file = Files.createTempDirectory("eduludo-h").resolve("history.csv");
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        GameRecord older = new GameRecord("1", LocalDateTime.of(2026, 1, 1, 10, 0), Difficulty.EASY, 300,
                List.of(new GameRecord.RankedPlayer(1, "Dana", null, false),
                        new GameRecord.RankedPlayer(2, "Bot", null, false)));
        GameRecord newer = new GameRecord("2", LocalDateTime.of(2026, 2, 1, 10, 0), Difficulty.HARD, 3725,
                List.of(new GameRecord.RankedPlayer(1, "Omar", null, false),
                        new GameRecord.RankedPlayer(2, "Lia", null, false),
                        new GameRecord.RankedPlayer(3, "Noa", null, false)));
        repo.save(older);
        repo.save(newer);
        List<GameRecord> all = new CsvHistoryRepository(file).findAll();
        assertEquals(2, all.size(), "two games");
        assertEquals(newer, all.get(0), "newest first");
        assertEquals(older, all.get(1), "course fields round-trip");
        assertEquals("01:02:05", all.get(0).formattedDuration(), "duration format");
        List<String> lines = Files.readAllLines(file);
        assertEquals(CsvHistoryRepository.HEADER, lines.get(0), "exact course template header");
        assertTrue(lines.get(1).startsWith("1,easy,01/01/2026;10:00:00,Dana,Bot,,,00:05:00"),
                "first row follows course schema and leaves unused places blank");
        assertTrue(lines.get(2).startsWith("2,hard,01/02/2026;10:00:00,Omar,Lia,Noa,,01:02:05"),
                "game IDs increase and duration uses HH:MM:SS");
    }

    @Test
    void aDamagedHistoryLineIsSkipped() throws IOException {
        Path file = Files.createTempDirectory("eduludo-h2").resolve("history.csv");
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        Files.writeString(file, CsvHistoryRepository.HEADER + System.lineSeparator()
                + "1,medium,01/01/2026;10:00:00,A,B,,,00:01:00" + System.lineSeparator());
        Files.writeString(file, "garbage,line\n", java.nio.file.StandardOpenOption.APPEND);
        assertEquals(1, repo.findAll().size(), "the good line is still read");
    }

    @Test
    void firstRunStartsTheHistoryFromTheBundledLecturerCsv() throws IOException {
        Path file = Files.createTempDirectory("eduludo-seed").resolve("history.csv");
        assertTrue(CsvHistoryRepository.seedFromCourseTemplate(file), "history file created on first run");
        assertEquals(Files.readAllLines(Path.of("docs/history-template.csv")), Files.readAllLines(file),
                "the new history file is the lecturer's CSV, unchanged");
        assertEquals(1, new CsvHistoryRepository(file).findAll().size(), "lecturer's example game is listed");
        Files.writeString(file, CsvHistoryRepository.HEADER + System.lineSeparator());
        assertFalse(CsvHistoryRepository.seedFromCourseTemplate(file), "an existing history file is never replaced");
        assertEquals(1, Files.readAllLines(file).size(), "existing file left unchanged");
    }

    @Test
    void attachedCourseTemplateSampleIsRead() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("docs/history-template.csv"));
        assertEquals(CsvHistoryRepository.HEADER, lines.get(0), "template header is copied exactly");
        Path file = Files.createTempDirectory("eduludo-course-history").resolve("history.csv");
        Files.write(file, lines);
        List<GameRecord> records = new CsvHistoryRepository(file).findAll();
        assertEquals(1, records.size(), "sample data row is recognized");
        GameRecord sample = records.get(0);
        assertEquals("1", sample.id(), "game ID");
        assertEquals(LocalDateTime.of(2026, 10, 19, 12, 1), sample.startedAt(), "local start timestamp");
        assertEquals(Difficulty.HARD, sample.difficulty(), "difficulty");
        assertEquals(3904L, sample.durationSeconds(), "duration");
        assertEquals(List.of("Mor", "Noy", "Safa", "Muhammed"),
                sample.ranking().stream().map(GameRecord.RankedPlayer::name).toList(), "places");
        new CsvHistoryRepository(file).save(new GameRecord("ignored", LocalDateTime.of(2026, 10, 20, 9, 0),
                Difficulty.EASY, 65, List.of(new GameRecord.RankedPlayer(1, "New winner", null, false),
                new GameRecord.RankedPlayer(2, "Runner up", null, false))));
        assertTrue(Files.readAllLines(file).get(2).startsWith("2,easy,20/10/2026;09:00:00"),
                "new records continue with the next sequential ID");
    }

    @Test
    void aMismatchedHistoryHeaderIsNotOverwritten() throws IOException {
        Path file = Files.createTempDirectory("eduludo-wrong-header").resolve("history.csv");
        String original = "id,finishedAt,difficulty" + System.lineSeparator() + "old,data";
        Files.writeString(file, original);
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        GameRecord record = new GameRecord("new", LocalDateTime.of(2026, 1, 1, 10, 0), Difficulty.EASY, 60,
                List.of(new GameRecord.RankedPlayer(1, "A", null, false),
                        new GameRecord.RankedPlayer(2, "B", null, false)));
        assertThrows(IllegalStateException.class, () -> repo.save(record), "mismatched schema is rejected");
        assertEquals(original, Files.readString(file), "the existing file remains unchanged");
    }

    @Test
    void courseHistoryNeedsTwoNamesAndCsvSafeNames() throws IOException {
        Path file = Files.createTempDirectory("eduludo-invalid-history").resolve("history.csv");
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        GameRecord onePlace = new GameRecord("one", LocalDateTime.of(2026, 1, 1, 10, 0), Difficulty.EASY, 60,
                List.of(new GameRecord.RankedPlayer(1, "A", null, false)));
        assertThrows(IllegalArgumentException.class, () -> repo.save(onePlace), "at least two places are required");
        GameRecord invalidName = new GameRecord("comma", LocalDateTime.of(2026, 1, 1, 10, 0), Difficulty.EASY, 60,
                List.of(new GameRecord.RankedPlayer(1, "A, B", null, false),
                        new GameRecord.RankedPlayer(2, "C", null, false)));
        assertThrows(IllegalArgumentException.class, () -> repo.save(invalidName), "CSV delimiters are rejected");
    }

    @Test
    void aHistoryFileSavedByExcelWithAByteOrderMarkIsStillUsable() throws IOException {
        Path file = Files.createTempDirectory("eduludo-bom").resolve("history.csv");
        // Excel writes a byte-order mark in front of the header when it saves a file as "CSV UTF-8".
        Files.writeString(file, "\uFEFF" + CsvHistoryRepository.HEADER + System.lineSeparator()
                + "1,easy,01/01/2026;10:00:00,A,B,,,00:01:00" + System.lineSeparator());
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        assertEquals(1, repo.findAll().size(), "the existing game is still read");
        repo.save(new GameRecord("next", LocalDateTime.of(2026, 1, 2, 11, 0), Difficulty.EASY, 120,
                List.of(new GameRecord.RankedPlayer(1, "C", null, false),
                        new GameRecord.RankedPlayer(2, "D", null, false))));
        assertEquals(2, repo.findAll().size(), "a new game can still be saved");
        assertTrue(Files.readAllLines(file).get(2).startsWith("2,easy,02/01/2026;11:00:00"),
                "the new row continues the sequence");
    }

    @Test
    void anEmptyHistoryFileGetsItsHeaderBackInsteadOfFailingForever() throws IOException {
        Path file = Files.createTempDirectory("eduludo-empty").resolve("history.csv");
        Files.writeString(file, "");  // an interrupted first write leaves a 0-byte file behind
        CsvHistoryRepository repo = new CsvHistoryRepository(file);
        assertTrue(repo.findAll().isEmpty(), "an empty history reads as no games, not as an error");
        repo.save(new GameRecord("first", LocalDateTime.of(2026, 1, 3, 12, 0), Difficulty.MEDIUM, 90,
                List.of(new GameRecord.RankedPlayer(1, "A", null, false),
                        new GameRecord.RankedPlayer(2, "B", null, false))));
        List<String> lines = Files.readAllLines(file);
        assertEquals(CsvHistoryRepository.HEADER, lines.get(0), "the header is written back");
        assertEquals(1, repo.findAll().size(), "the game is saved");
    }
}
