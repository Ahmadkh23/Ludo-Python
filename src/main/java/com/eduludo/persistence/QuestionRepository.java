package com.eduludo.persistence;

import com.eduludo.Model.Question;

import java.util.List;

/** DAO / Repository pattern: storage of the question bank, hidden behind an interface. */
public interface QuestionRepository {

    List<Question> loadAll();

    void saveAll(List<Question> questions);
}

