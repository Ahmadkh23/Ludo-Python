package com.eduludo.persistence;

import com.eduludo.Model.GameRecord;

import java.util.List;

/**
 * DAO / Repository pattern: storage of finished games.
 * <p>
 * The course will publish a fixed schema for the history. When it arrives, write a new class that
 * implements this interface with that exact schema and change one line in {@code App}. No other code changes.
 */
public interface HistoryRepository {

    void save(GameRecord record);

    /** All games, newest first. */
    List<GameRecord> findAll();
}

