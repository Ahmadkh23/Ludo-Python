package com.eduludo.Model;

import com.eduludo.testing.Test;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** A player's four pieces and the states a piece can be in. */
public class PlayerAndPieceTest {

    private static final int FINISH = 56;

    private static Player newPlayer() {
        return new Player("Omar", PlayerColor.RED, false, FINISH);
    }

    @Test
    void aNewPlayerHasFourNumberedPiecesAllInTheCastle() {
        Player p = newPlayer();
        assertEquals(Player.PIECES_PER_PLAYER, p.pieces().size(), "four pieces");
        for (int i = 1; i <= 4; i++) {
            Piece piece = p.piece(i);
            assertEquals(i, piece.number(), "piece numbered " + i);
            assertTrue(piece.owner() == p, "piece owned by the player");
            assertTrue(piece.isInCastle(), "piece starts in the castle");
            assertFalse(piece.isActive(), "a castle piece is not active");
        }
        assertFalse(p.hasActivePieces(), "no active pieces at the start");
        assertEquals(0L, p.finishedPieces(), "nothing finished at the start");
        assertFalse(p.hasFinished(), "player not finished at the start");
    }

    @Test
    void thePieceListCannotBeChangedFromOutside() {
        Player p = newPlayer();
        assertThrows(UnsupportedOperationException.class, () -> p.pieces().remove(0), "pieces list is read-only");
    }

    @Test
    void aPieceOnTheBoardIsActiveUntilItReachesTheFinish() {
        Player p = newPlayer();
        Piece piece = p.piece(1);
        piece.setPosition(0);
        assertTrue(piece.isActive(), "on the start square it is active");
        assertTrue(p.hasActivePieces(), "player has an active piece");
        piece.setPosition(FINISH);
        assertTrue(piece.isFinished(), "at the last index it is finished");
        assertFalse(piece.isActive(), "a finished piece is no longer active");
        assertEquals(1L, p.finishedPieces(), "one finished piece");
    }

    @Test
    void aPieceCanBeSentBackToTheCastle() {
        Piece piece = newPlayer().piece(2);
        piece.setPosition(10);
        piece.setPosition(Piece.IN_CASTLE);
        assertTrue(piece.isInCastle(), "captured piece is back in the castle");
    }

    @Test
    void positionsOutsideThePathAreRejected() {
        Piece piece = newPlayer().piece(1);
        assertThrows(IllegalArgumentException.class, () -> piece.setPosition(-2), "below the castle");
        assertThrows(IllegalArgumentException.class, () -> piece.setPosition(FINISH + 1), "past the finish");
        assertTrue(piece.isInCastle(), "a rejected move leaves the piece where it was");
    }

    @Test
    void thePlayerHasFinishedOnlyWhenAllFourPiecesHave() {
        Player p = newPlayer();
        for (int i = 1; i <= 3; i++) {
            p.piece(i).setPosition(FINISH);
        }
        assertFalse(p.hasFinished(), "three of four is not finished");
        p.piece(4).setPosition(FINISH);
        assertTrue(p.hasFinished(), "all four finished");
        assertEquals(4L, p.finishedPieces(), "four finished pieces");
    }

    @Test
    void skipNextTurnFlagAndDisplayText() {
        Player p = new Player("Bot", PlayerColor.BLUE, true, FINISH);
        assertTrue(p.isComputer(), "computer seat");
        assertFalse(p.isSkipNextTurn(), "no skip by default");
        p.setSkipNextTurn(true);
        assertTrue(p.isSkipNextTurn(), "skip flag set");
        assertEquals("Bot (Blue)", p.toString(), "player display text");
        assertEquals("Bot #3", p.piece(3).toString(), "piece display text");
    }
}

