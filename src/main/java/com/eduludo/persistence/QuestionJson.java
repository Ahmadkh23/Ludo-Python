package com.eduludo.persistence;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts questions to and from JSON:
 * <pre>[{"id":1,"level":"EASY","topic":"Agile","text":"...","options":["a","b","c","d"],"correct":0}]</pre>
 */
public final class QuestionJson {

    private QuestionJson() {
    }

    public static String toJson(List<Question> questions) {
        List<Object> list = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", (long) q.id());
            m.put("level", q.level().name());
            m.put("topic", q.topic());
            m.put("text", q.text());
            m.put("options", new ArrayList<Object>(q.options()));
            m.put("correct", (long) q.correctIndex());
            list.add(m);
        }
        return MiniJson.write(list);
    }

    public static List<Question> fromJson(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof List<?> list)) {
            throw new IllegalArgumentException("The question file must contain a JSON array");
        }
        List<Question> result = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) {
                throw new IllegalArgumentException("Each question must be a JSON object");
            }
            int id = ((Number) m.get("id")).intValue();
            QuestionLevel level = QuestionLevel.valueOf(String.valueOf(m.get("level")).toUpperCase(Locale.ROOT));
            String topic = m.get("topic") == null ? "" : String.valueOf(m.get("topic"));
            String text = String.valueOf(m.get("text"));
            List<String> options = new ArrayList<>();
            for (Object opt : (List<?>) m.get("options")) {
                options.add(String.valueOf(opt));
            }
            int correct = ((Number) m.get("correct")).intValue();
            result.add(new Question(id, level, topic, text, options, correct));
        }
        return result;
    }
}

