package com.eduludo.persistence;

import java.nio.file.Path;

/**
 * Where the game keeps its files: {@code <user home>/.eduludo/} by default. The user's home folder is always
 * writable, even when the game is installed in "Program Files". Override with {@code -Deduludo.data=<folder>}.
 */
public final class DataPaths {

    private final Path base;

    public DataPaths(Path base) {
        this.base = base;
    }

    public static DataPaths defaults() {
        String override = System.getProperty("eduludo.data");
        if (override != null && !override.isBlank()) {
            return new DataPaths(Path.of(override));
        }
        return new DataPaths(Path.of(System.getProperty("user.home"), ".eduludo"));
    }

    public Path base() {
        return base;
    }

    public Path questions() {
        return base.resolve("questions.dat");
    }

    public Path history() {
        return base.resolve("history.csv");
    }

    public Path adminCredentials() {
        return base.resolve("admin.properties");
    }
}

