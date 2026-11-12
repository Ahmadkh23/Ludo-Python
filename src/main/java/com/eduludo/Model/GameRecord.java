package com.eduludo.Model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A finished game, as stored in the history: when it started, the difficulty, how long it took,
 * and the players in finishing order (winner first).
 *
 * @param id              unique id
 * @param startedAt       local date and time the game started
 * @param difficulty      game level
 * @param durationSeconds play time in seconds
 * @param ranking         players in finishing order
 */
public record GameRecord(String id, LocalDateTime startedAt, Difficulty difficulty, long durationSeconds,
                         List<RankedPlayer> ranking) {

    public GameRecord {
        ranking = List.copyOf(ranking);
    }

    /**
     * One line of the ranking. The course CSV does not store color or computer-seat information, so
     * these may be null/false for records loaded from that file.
     */
    public record RankedPlayer(int place, String name, PlayerColor color, boolean computer) {
    }

    public String winnerName() {
        return ranking.isEmpty() ? "" : ranking.get(0).name();
    }

    /** Duration formatted as HH:MM:SS, the same format the history CSV uses. */
    public String formattedDuration() {
        return formatSeconds(durationSeconds);
    }

    public static String formatSeconds(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
