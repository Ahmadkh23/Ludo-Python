package com.eduludo.service;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.Model.QuestionProvider;
import com.eduludo.persistence.QuestionRepository;
import com.eduludo.security.AdminSession;
import com.eduludo.security.AuthService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The only way to reach the question bank.
 * <ul>
 *   <li>Players (the game) get one random question of a level through {@link #draw(QuestionLevel)}. Questions
 *       don't repeat until every question of that level has been used.</li>
 *   <li>Viewing the full bank (with the answers) and adding, editing or deleting questions need a valid
 *       {@link AdminSession}. The check is done here in the service layer, so the UI can't skip it.</li>
 *   <li>The bank must keep at least {@value #MIN_TOTAL} questions and at least one question per level.</li>
 * </ul>
 */
public final class QuestionService implements QuestionProvider {

    public static final int MIN_TOTAL = 30;

    private final QuestionRepository repository;
    private final AuthService auth;
    private final Random rng;
    private List<Question> questions;
    private final Map<QuestionLevel, List<Question>> bags = new EnumMap<>(QuestionLevel.class);

    public QuestionService(QuestionRepository repository, AuthService auth, Random rng) {
        this.repository = repository;
        this.auth = auth;
        this.rng = rng;
        this.questions = new ArrayList<>(repository.loadAll());
    }

    // ------------------------------------------------------------ player access

    @Override
    public synchronized Question draw(QuestionLevel level) {
        List<Question> bag = bags.computeIfAbsent(level, l -> new ArrayList<>());
        if (bag.isEmpty()) {
            for (Question q : questions) {
                if (q.level() == level) {
                    bag.add(q);
                }
            }
            if (bag.isEmpty()) {
                throw new IllegalStateException("The question bank has no " + level.displayName() + " questions");
            }
            Collections.shuffle(bag, rng);
        }
        return bag.remove(bag.size() - 1);
    }

    /** Number of questions per level (no answers revealed), shown on the start screen. */
    public synchronized Map<QuestionLevel, Integer> countByLevel() {
        Map<QuestionLevel, Integer> counts = new EnumMap<>(QuestionLevel.class);
        for (QuestionLevel l : QuestionLevel.values()) {
            counts.put(l, 0);
        }
        for (Question q : questions) {
            counts.merge(q.level(), 1, Integer::sum);
        }
        return counts;
    }

    // ------------------------------------------------------------ admin access

    public synchronized List<Question> listAll(AdminSession session) {
        auth.requireValid(session);
        return List.copyOf(questions);
    }

    /** Adds a question (its id is assigned automatically) and returns the stored copy. */
    public synchronized Question add(AdminSession session, Question question) {
        auth.requireValid(session);
        int nextId = questions.stream().mapToInt(Question::id).max().orElse(0) + 1;
        Question stored = question.withId(nextId);
        List<Question> updated = new ArrayList<>(questions);
        updated.add(stored);
        commit(updated);
        return stored;
    }

    public synchronized void update(AdminSession session, Question question) {
        auth.requireValid(session);
        List<Question> updated = new ArrayList<>(questions);
        int idx = indexOf(question.id());
        Question old = updated.get(idx);
        updated.set(idx, question);
        if (old.level() != question.level()) {
            checkLevelsStillCovered(updated);
        }
        commit(updated);
    }

    public synchronized void delete(AdminSession session, int questionId) {
        auth.requireValid(session);
        if (questions.size() <= MIN_TOTAL) {
            throw new IllegalStateException("The bank must keep at least " + MIN_TOTAL
                    + " questions. Add a new question before deleting this one, or edit it instead.");
        }
        List<Question> updated = new ArrayList<>(questions);
        updated.remove(indexOf(questionId));
        checkLevelsStillCovered(updated);
        commit(updated);
    }

    private void checkLevelsStillCovered(List<Question> list) {
        for (QuestionLevel l : QuestionLevel.values()) {
            if (list.stream().noneMatch(q -> q.level() == l)) {
                throw new IllegalStateException("At least one " + l.displayName() + " question must remain.");
            }
        }
    }

    private int indexOf(int id) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).id() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("No question with id " + id);
    }

    private void commit(List<Question> updated) {
        repository.saveAll(updated);
        questions = updated;
        bags.clear();
    }
}

