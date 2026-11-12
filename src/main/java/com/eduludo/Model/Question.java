package com.eduludo.Model;

import java.util.List;
import java.util.Objects;

/**
 * A multiple-choice course question with exactly four answers, one of them correct.
 *
 * @param id           unique id
 * @param level        difficulty (decides how many steps a right or wrong answer moves)
 * @param topic        course topic, e.g. "Agile", "MVC", "Design Patterns", "QA"
 * @param text         the question text
 * @param options      exactly 4 answers
 * @param correctIndex index (0-3) of the correct answer
 */
public record Question(int id, QuestionLevel level, String topic, String text, List<String> options, int correctIndex) {

    public static final int OPTION_COUNT = 4;

    public Question {
        Objects.requireNonNull(level, "level");
        topic = topic == null ? "" : topic.strip();
        text = Objects.requireNonNull(text, "text").strip();
        options = List.copyOf(Objects.requireNonNull(options, "options")).stream().map(String::strip).toList();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Question text must not be empty");
        }
        if (options.size() != OPTION_COUNT) {
            throw new IllegalArgumentException("A question needs exactly " + OPTION_COUNT + " answers");
        }
        if (options.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("Answers must not be empty");
        }
        if (options.stream().map(String::toLowerCase).distinct().count() != OPTION_COUNT) {
            throw new IllegalArgumentException("The 4 answers must be different from each other");
        }
        if (correctIndex < 0 || correctIndex >= OPTION_COUNT) {
            throw new IllegalArgumentException("Correct answer index must be between 0 and 3");
        }
    }

    public boolean isCorrect(int answerIndex) {
        return answerIndex == correctIndex;
    }

    public Question withId(int newId) {
        return new Question(newId, level, topic, text, options, correctIndex);
    }
}

