package com.eduludo.Model;

import com.eduludo.persistence.CsvHistoryRepository;
import com.eduludo.testing.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Checks that span the model and the course CSV history. */
public class CombinedValidationTest {

    @Test
    public void csvSafeNameBoundary() {
        Game.builder()
                .addPlayer("abcdefghijklmnopqrst", PlayerColor.RED, false)
                .addPlayer("Bob", PlayerColor.GREEN, false)
                .build();
        for (String name : new String[] {"abcdefghijklmnopqrstu", "A,B", "A\"B", "A\nB", "A\rB"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> Game.builder()
                            .addPlayer(name, PlayerColor.RED, false)
                            .addPlayer("Bob", PlayerColor.GREEN, false)
                            .build(),
                    "CSV-safe name");
        }
    }

    @Test
    public void completedMatchSavesAndReloadsCourseCsv() throws Exception {
        long[] time = {1000};
        Game g = Game.builder()
                .board(BoardFactory.createLayout(Difficulty.EASY))
                .clock(() -> time[0])
                .normalDie(new ScriptedDice(6, "Normal").then(1))
                .addPlayer("Alice", PlayerColor.RED, false)
                .addPlayer("Bob", PlayerColor.GREEN, false)
                .build();
        for (int n = 1; n <= 3; n++) {
            g.players().get(0).piece(n).setPosition(g.board().finishIndex());
        }
        g.players().get(0).piece(4).setPosition(g.board().finishIndex() - 1);

        time[0] = 6000;
        g.roll();
        g.chooseMoveFor(g.players().get(0).piece(4));
        assertTrue(g.isOver(), "match finished");

        Path dir = Files.createTempDirectory("eduludo-combined-review-");
        Path file = dir.resolve("history.csv");
        try {
            CsvHistoryRepository repo = new CsvHistoryRepository(file);
            repo.save(g.toRecord());
            GameRecord saved = repo.findAll().get(0);
            assertEquals("Alice", saved.winnerName(), "winner");
            assertEquals(5L, saved.durationSeconds(), "duration");
            assertEquals(Instant.ofEpochMilli(1000).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    saved.startedAt(), "start time");
            assertEquals(8, Files.readAllLines(file).get(1).split(",", -1).length, "eight columns");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }
}
