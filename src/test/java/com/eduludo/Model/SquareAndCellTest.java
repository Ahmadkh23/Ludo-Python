package com.eduludo.Model;

import com.eduludo.testing.Test;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertTrue;

/** Square types and their protection, grid coordinates, and the surprise draw. */
public class SquareAndCellTest {

    @Test
    void onlySafeStartHomeAndSurpriseSquaresAreProtected() {
        Set<SquareType> expected = EnumSet.of(SquareType.SAFE, SquareType.START, SquareType.HOME, SquareType.SURPRISE);
        for (SquareType type : SquareType.values()) {
            assertEquals(expected.contains(type), type.isProtected(), type + " protection");
        }
    }

    @Test
    void changingASquareClearsItsOldOwnerAndLevel() {
        Square sq = new Square(new Cell(3, 4), SquareType.REGULAR, null);
        sq.makeQuestion(QuestionLevel.HARD);
        assertEquals(SquareType.QUESTION, sq.type(), "question square");
        assertEquals(QuestionLevel.HARD, sq.questionLevel(), "question level");
        assertFalse(sq.isProtected(), "question squares are not protected");

        sq.makeOwned(SquareType.START, PlayerColor.GREEN);
        assertEquals(PlayerColor.GREEN, sq.owner(), "owned by green");
        assertEquals(null, sq.questionLevel(), "level cleared");
        assertTrue(sq.isProtected(), "start squares are protected");

        sq.makeSafe();
        assertEquals(null, sq.owner(), "owner cleared by safe");
        sq.makeSurprise();
        assertEquals(SquareType.SURPRISE, sq.type(), "surprise square");
        sq.makeRegular();
        assertEquals(SquareType.REGULAR, sq.type(), "back to regular");
        assertEquals(null, sq.owner(), "regular has no owner");
        assertEquals(new Cell(3, 4), sq.cell(), "the cell never changes");
    }

    @Test
    void squareDescriptionShowsTypeOwnerLevelAndPosition() {
        Square sq = new Square(new Cell(1, 2), SquareType.ARROW, PlayerColor.RED);
        assertEquals("ARROW(RED)@1,2", sq.toString(), "owned square");
        sq.makeQuestion(QuestionLevel.EASY);
        assertEquals("QUESTION(EASY)@1,2", sq.toString(), "question square");
    }

    @Test
    void fourQuarterTurnsBringACellBackToWhereItStarted() {
        Cell c = new Cell(2, 5);
        assertEquals(new Cell(5, 12), c.rotate(15), "one clockwise quarter-turn on a 15x15 grid");
        assertEquals(c, c.rotate(15, 4), "four turns is a full circle");
        assertEquals(c, c.rotate(15, 0), "zero turns changes nothing");
        assertEquals(c.rotate(15).rotate(15), c.rotate(15, 2), "two single turns equal one double turn");
    }

    @Test
    void shiftMovesDiagonallyAndTouchesFindsTheEightNeighbours() {
        assertEquals(new Cell(4, 6), new Cell(2, 4).shift(2), "shift by 2");
        Cell centre = new Cell(5, 5);
        int neighbours = 0;
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                if (centre.touches(new Cell(5 + dr, 5 + dc))) {
                    neighbours++;
                }
            }
        }
        assertEquals(8, neighbours, "exactly the 8 surrounding cells touch");
        assertFalse(centre.touches(centre), "a cell does not touch itself");
    }

    /** A Random whose nextInt always returns the given value, to hit exact boundaries. */
    private static Random fixed(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    @Test
    void surpriseDrawFollowsTheWeightBoundariesExactly() {
        assertEquals(Surprise.ROCKET_TO_FINISH, Surprise.draw(fixed(0)), "0 -> rocket");
        assertEquals(Surprise.ROCKET_TO_FINISH, Surprise.draw(fixed(9)), "9 -> rocket");
        assertEquals(Surprise.MOVE_OPPONENT, Surprise.draw(fixed(10)), "10 -> move opponent");
        assertEquals(Surprise.BACK_TO_CASTLE, Surprise.draw(fixed(20)), "20 -> back to castle");
        assertEquals(Surprise.BACK_THREE, Surprise.draw(fixed(30)), "30 -> back three");
        assertEquals(Surprise.BACK_THREE, Surprise.draw(fixed(59)), "59 -> back three");
        assertEquals(Surprise.LOSE_NEXT_TURN, Surprise.draw(fixed(60)), "60 -> lose next turn");
        assertEquals(Surprise.ULTRA_BONUS_ROLL, Surprise.draw(fixed(90)), "90 -> bonus roll");
        assertEquals(Surprise.ULTRA_BONUS_ROLL, Surprise.draw(fixed(99)), "99 -> bonus roll");
    }

    @Test
    void everySurpriseHasADescription() {
        for (Surprise s : Surprise.values()) {
            assertFalse(s.description().isBlank(), s + " has a description");
        }
    }
}

