package com.eduludo.Model;

import com.eduludo.testing.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertTrue;

/** Board geometry and the square counts required for each difficulty level. */
public class BoardFactoryTest {

    @Test
    void pathLengthsMatchTheSpecification() {
        // EASY: 51 loop squares + 6 home squares. MEDIUM adds 58+1 (64-ring lap + loop entry), HARD adds 72 more.
        assertEquals(57, BoardFactory.createLayout(Difficulty.EASY).path(PlayerColor.RED).size(), "easy path");
        assertEquals(116, BoardFactory.createLayout(Difficulty.MEDIUM).path(PlayerColor.RED).size(), "medium path");
        assertEquals(188, BoardFactory.createLayout(Difficulty.HARD).path(PlayerColor.RED).size(), "hard path");
    }

    @Test
    void trackSizesAre52Then64Then72() {
        assertEquals(52, BoardFactory.createLayout(Difficulty.EASY).trackCells().size(), "easy track");
        assertEquals(52 + 64, BoardFactory.createLayout(Difficulty.MEDIUM).trackCells().size(), "medium track");
        assertEquals(52 + 64 + 72, BoardFactory.createLayout(Difficulty.HARD).trackCells().size(), "hard track");
        assertEquals(64, BoardFactory.perimeter(0, 17).size(), "64 ring");
        assertEquals(72, BoardFactory.perimeter(0, 19).size(), "72 ring");
    }

    @Test
    void everyPathIsContinuousUniqueAndInsideTheGrid() {
        for (Difficulty d : Difficulty.values()) {
            Board b = BoardFactory.createLayout(d);
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = b.path(color);
                assertEquals(b.path(PlayerColor.RED).size(), path.size(), d + " " + color + " same length as red");
                assertEquals(path.size(), new HashSet<>(path).size(), d + " " + color + " path has no repeated square");
                for (int i = 0; i < path.size(); i++) {
                    Cell c = path.get(i);
                    assertTrue(c.row() >= 0 && c.col() >= 0 && c.row() < b.size() && c.col() < b.size(), "inside grid");
                    assertTrue(b.square(c) != null, d + " " + color + " square exists at " + c);
                    if (i > 0) {
                        assertTrue(path.get(i - 1).touches(c), d + " " + color + " step " + i + " is a neighbour move");
                    }
                }
            }
        }
    }

    @Test
    void fixedSquareCountsPerLevel() {
        for (Difficulty d : Difficulty.values()) {
            Board b = BoardFactory.create(d, new Random(7));
            assertEquals(4L, b.count(SquareType.START), d + " start squares");
            assertEquals((long) d.arrowSquares(), b.count(SquareType.ARROW), d + " arrow squares");
            assertEquals((long) d.safeSquares(), b.count(SquareType.SAFE), d + " safe squares");
            assertEquals((long) d.surpriseSquares(), b.count(SquareType.SURPRISE), d + " surprise squares");
            for (QuestionLevel level : QuestionLevel.values()) {
                assertEquals((long) d.questionsPerLevel(), b.countQuestions(level), d + " " + level + " question squares");
            }
            assertEquals(24L, b.count(SquareType.HOME), d + " home squares (6 per colour)");
        }
        assertEquals(4, Difficulty.EASY.arrowSquares(), "easy arrows");
        assertEquals(8, Difficulty.MEDIUM.arrowSquares(), "medium arrows");
        assertEquals(12, Difficulty.HARD.arrowSquares(), "hard arrows");
    }

    @Test
    void startSquaresAreOwnedAndInTheCornerOnMediumAndHard() {
        Board easy = BoardFactory.createLayout(Difficulty.EASY);
        assertEquals(new Cell(6, 1), easy.path(PlayerColor.RED).get(0), "easy red start");
        for (Difficulty d : List.of(Difficulty.MEDIUM, Difficulty.HARD)) {
            Board b = BoardFactory.createLayout(d);
            int n = b.size();
            Set<Cell> corners = Set.of(new Cell(0, 0), new Cell(0, n - 1), new Cell(n - 1, n - 1), new Cell(n - 1, 0));
            for (PlayerColor color : PlayerColor.values()) {
                Cell start = b.path(color).get(0);
                assertTrue(corners.contains(start), d + " " + color + " starts in a corner");
                assertEquals(SquareType.START, b.square(start).type(), "start type");
                assertEquals(color, b.square(start).owner(), "start owner");
            }
        }
    }

    @Test
    void eachColourHasItsOwnArrowsOnItsPathAndItsOwnFinish() {
        for (Difficulty d : Difficulty.values()) {
            Board b = BoardFactory.createLayout(d);
            Set<Cell> finishes = new HashSet<>();
            for (PlayerColor color : PlayerColor.values()) {
                List<Cell> path = b.path(color);
                long ownArrowsOnPath = path.stream()
                        .filter(c -> b.square(c).type() == SquareType.ARROW && b.square(c).owner() == color)
                        .count();
                assertEquals((long) d.outerRings() + 1, ownArrowsOnPath, d + " " + color + " arrows on own path");
                Cell finish = path.get(path.size() - 1);
                assertEquals(SquareType.HOME, b.square(finish).type(), "finish is a home square");
                assertEquals(color, b.square(finish).owner(), "finish owner");
                // The 6 last squares are the coloured home path.
                for (int i = path.size() - 6; i < path.size(); i++) {
                    assertEquals(color, b.square(path.get(i)).owner(), d + " " + color + " home path square " + i);
                }
                assertTrue(finishes.add(finish), "finish squares are different per colour");
            }
        }
    }

    @Test
    void randomSquaresChangeBetweenGamesButNeverReplaceFixedSquares() {
        Board a = BoardFactory.create(Difficulty.HARD, new Random(1));
        Board b = BoardFactory.create(Difficulty.HARD, new Random(2));
        boolean different = false;
        for (Square s : a.squares()) {
            if (s.type() != b.square(s.cell()).type()) {
                different = true;
            }
        }
        assertTrue(different, "two games have different random square positions");
        Board layout = BoardFactory.createLayout(Difficulty.HARD);
        for (Square s : layout.squares()) {
            if (s.type() == SquareType.START || s.type() == SquareType.ARROW || s.type() == SquareType.HOME) {
                assertEquals(s.type(), a.square(s.cell()).type(), "fixed square kept at " + s.cell());
            }
        }
    }

    @Test
    void protectionRulesFollowTheSpecification() {
        assertTrue(SquareType.SAFE.isProtected(), "safe");
        assertTrue(SquareType.START.isProtected(), "start");
        assertTrue(SquareType.HOME.isProtected(), "home");
        assertTrue(SquareType.SURPRISE.isProtected(), "surprise");
        assertFalse(SquareType.REGULAR.isProtected(), "regular");
        assertFalse(SquareType.QUESTION.isProtected(), "question");
        assertFalse(SquareType.ARROW.isProtected(), "arrow");
    }
}

