package com.eduludo.service;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.persistence.QuestionRepository;
import com.eduludo.security.AdminSession;
import com.eduludo.security.AuthService;
import com.eduludo.testing.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Question access rules: players draw random questions, only a logged-in admin can see or change the bank. */
public class QuestionServiceTest {

    private static final class MemoryRepository implements QuestionRepository {
        List<Question> stored = new ArrayList<>();
        int saves;

        @Override
        public List<Question> loadAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public void saveAll(List<Question> questions) {
            stored = new ArrayList<>(questions);
            saves++;
        }
    }

    private MemoryRepository repo;
    private AuthService auth;
    private AdminSession session;

    private QuestionService service(int perLevel) throws IOException {
        return service(perLevel, perLevel, perLevel);
    }

    private QuestionService service(int easy, int medium, int hard) throws IOException {
        repo = new MemoryRepository();
        int id = 1;
        int[] counts = {easy, medium, hard};
        for (QuestionLevel level : QuestionLevel.values()) {
            for (int i = 0; i < counts[level.ordinal()]; i++) {
                repo.stored.add(q(id++, level));
            }
        }
        auth = new AuthService(Files.createTempDirectory("eduludo-s").resolve("admin.properties"), Clock.systemUTC(), 1000);
        auth.createAdmin("secret123".toCharArray());
        session = auth.login("secret123".toCharArray());
        return new QuestionService(repo, auth, new Random(3));
    }

    private static Question q(int id, QuestionLevel level) {
        return new Question(id, level, "Topic", "Question " + id, List.of("a" + id, "b", "c", "d"), 0);
    }

    @Test
    void drawReturnsTheRequestedLevelWithoutRepeatsUntilAllAreUsed() throws IOException {
        QuestionService s = service(10);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Question q = s.draw(QuestionLevel.MEDIUM);
            assertEquals(QuestionLevel.MEDIUM, q.level(), "level");
            assertTrue(seen.add(q.id()), "no repeat inside one round");
        }
        assertEquals(QuestionLevel.MEDIUM, s.draw(QuestionLevel.MEDIUM).level(), "refills after all were used");
    }

    @Test
    void adminOperationsNeedAValidSession() throws IOException {
        QuestionService s = service(10);
        assertThrows(SecurityException.class, () -> s.listAll(null), "list needs login");
        assertThrows(SecurityException.class, () -> s.add(null, q(0, QuestionLevel.EASY)), "add needs login");
        assertThrows(SecurityException.class, () -> s.delete(null, 1), "delete needs login");
        auth.logout(session);
        assertThrows(SecurityException.class, () -> s.listAll(session), "logged-out session refused");
    }

    @Test
    void adminCanAddEditAndDelete() throws IOException {
        QuestionService s = service(11);
        Question added = s.add(session, q(0, QuestionLevel.HARD));
        assertEquals(34, added.id(), "new id after the highest");
        assertEquals(34, s.listAll(session).size(), "added");
        Question edited = new Question(added.id(), QuestionLevel.EASY, "QA", "Edited?", List.of("w", "x", "y", "z"), 3);
        s.update(session, edited);
        assertTrue(s.listAll(session).contains(edited), "edited");
        s.delete(session, added.id());
        assertEquals(33, s.listAll(session).size(), "deleted");
        assertEquals(3, repo.saves, "every change is saved");
    }

    @Test
    void theBankKeepsAtLeastThirtyQuestionsAndEveryLevel() throws IOException {
        QuestionService s = service(10);
        assertThrows(IllegalStateException.class, () -> s.delete(session, 1), "can't go below 30");

        QuestionService oneHard = service(15, 15, 1); // 31 questions, only id 31 is HARD
        assertThrows(IllegalStateException.class, () -> oneHard.delete(session, 31), "last hard question can't be deleted");
        assertThrows(IllegalStateException.class, () -> oneHard.update(session, q(31, QuestionLevel.EASY)),
                "last hard question can't change level");
        assertThrows(IllegalArgumentException.class, () -> oneHard.update(session, q(999, QuestionLevel.EASY)), "unknown id");
        assertEquals(31, oneHard.listAll(session).size(), "nothing changed");
    }

    @Test
    void invalidQuestionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", " ", List.of("a", "b", "c", "d"), 0), "empty text");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q", List.of("a", "b", "c"), 0), "3 answers");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q", List.of("a", "b", "c", "A"), 0), "duplicate answers");
        assertThrows(IllegalArgumentException.class,
                () -> new Question(1, QuestionLevel.EASY, "t", "Q", List.of("a", "b", "c", "d"), 4), "bad correct index");
    }
}

