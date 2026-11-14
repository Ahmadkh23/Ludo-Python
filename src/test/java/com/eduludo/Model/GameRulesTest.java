package com.eduludo.Model;

import com.eduludo.testing.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Rules of play and edge cases, driven by scripted dice so every case is reproducible. */
public class GameRulesTest {

    private ScriptedDice normal;
    private ScriptedDice multiplier;
    private Board board;

    /** A fixed "random" source: nextInt(bound) always returns the same value (used to pick a surprise). */
    private static final class FixedRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }
    }

    private static final QuestionProvider ONE_QUESTION = level -> new Question(1, level, "Test", "2 + 2 = ?",
            List.of("4", "3", "5", "22"), 0);

    private Game game(Difficulty d, Random rng, PlayerColor... colors) {
        normal = new ScriptedDice(6, "Normal");
        multiplier = d == Difficulty.EASY ? null : new ScriptedDice(d == Difficulty.MEDIUM ? 3 : 6, "Boost");
        board = BoardFactory.createLayout(d);
        Game.Builder b = Game.builder().board(board).normalDie(normal).multiplierDie(multiplier)
                .bonusDie(new ScriptedDice(6, "UltraBoost").then(3, 3, 3)).questions(ONE_QUESTION).random(rng)
                .clock(() -> 0L);
        for (PlayerColor c : colors) {
            b.addPlayer(c.displayName(), c, false);
        }
        return b.build();
    }

    private Game easyGame() {
        return game(Difficulty.EASY, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
    }

    private Player red(Game g) {
        return g.players().get(0);
    }

    private Player green(Game g) {
        return g.players().get(1);
    }

    /** Index on green's path of the cell that red reaches at red index i. */
    private int greenIndexOfRed(Game g, int redIndex) {
        Cell cell = board.path(PlayerColor.RED).get(redIndex);
        return board.path(PlayerColor.GREEN).indexOf(cell);
    }

    // ---------------------------------------------------------------- entering and the 6-rule

    @Test
    void aPieceLeavesTheCastleOnlyWithASix() {
        Game g = easyGame();
        normal.then(5);
        g.roll();
        assertTrue(g.options().isEmpty(), "no move with 5 and all pieces in the castle");
        assertEquals(green(g), g.currentPlayer(), "turn passes immediately");
        normal.then(6);
        g.roll();
        assertEquals(4, g.options().size(), "a 6 lets any of the 4 pieces enter");
        assertTrue(g.options().stream().allMatch(o -> o.kind() == MoveOption.Kind.ENTER), "all options are ENTER");
    }

    @Test
    void enteringPutsThePieceOnTheStartSquareAndASixGivesAnotherRoll() {
        Game g = easyGame();
        normal.then(6);
        g.roll();
        g.choose(g.options().get(0));
        Piece p = red(g).piece(1);
        assertEquals(0, p.position(), "piece on start square");
        assertEquals(SquareType.START, g.squareOf(p).type(), "start square");
        assertEquals(red(g), g.currentPlayer(), "same player again after a 6");
        assertEquals(Phase.WAITING_FOR_ROLL, g.phase(), "waiting for the extra roll");
    }

    @Test
    void theThirdSixInARowIsCancelledAndTheTurnPasses() {
        Game g = easyGame();
        normal.then(6, 6, 6);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        g.roll();
        g.chooseMoveFor(red(g).piece(2));
        g.roll();
        assertEquals(green(g), g.currentPlayer(), "third 6 passes the turn");
        assertEquals(Piece.IN_CASTLE, red(g).piece(3).position(), "third 6 was not used");
        assertEquals(0, red(g).piece(1).position(), "earlier moves stay");
    }

    @Test
    void aSixCanMoveAPieceAlreadyOnTheBoard() {
        Game g = easyGame();
        red(g).piece(1).setPosition(10);
        normal.then(6);
        g.roll();
        boolean hasMove = g.options().stream().anyMatch(o -> o.kind() == MoveOption.Kind.MOVE && o.target() == 16);
        boolean hasEnter = g.options().stream().anyMatch(o -> o.kind() == MoveOption.Kind.ENTER);
        assertTrue(hasMove && hasEnter, "player may enter a new piece or move 6 with an existing one");
    }

    // ---------------------------------------------------------------- finishing

    @Test
    void theFinishNeedsAnExactRoll() {
        Game g = easyGame();
        int finish = board.finishIndex();
        red(g).piece(1).setPosition(finish - 2);
        normal.then(3);
        g.roll();
        assertTrue(g.options().isEmpty(), "3 is too much when 2 squares are left");
        assertEquals(green(g), g.currentPlayer(), "no other piece can move: turn passes");
        normal.then(1);
        g.roll(); // green: no pieces out
        normal.then(2);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        assertTrue(red(g).piece(1).isFinished(), "exact roll reaches the finish");
    }

    @Test
    void tooBigARollMustBeUsedByAnotherPiece() {
        Game g = easyGame();
        int finish = board.finishIndex();
        red(g).piece(1).setPosition(finish - 1);
        red(g).piece(2).setPosition(5);
        normal.then(4);
        g.roll();
        assertEquals(1, g.options().size(), "only the other piece can move");
        assertEquals(red(g).piece(2), g.options().get(0).piece(), "piece 2 moves");
    }

    @Test
    void theGameEndsWhenAllPlayersButOneHaveFinished() {
        Game g = game(Difficulty.EASY, new Random(1), PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW);
        int finish = board.finishIndex();
        Player red = g.players().get(0);
        Player green = g.players().get(1);
        for (int i = 1; i <= 4; i++) {
            green.piece(i).setPosition(finish);
        }
        for (int i = 1; i <= 3; i++) {
            red.piece(i).setPosition(finish);
        }
        red.piece(4).setPosition(finish - 1);
        normal.then(1);
        g.roll();
        g.chooseMoveFor(red.piece(4));
        assertEquals(Phase.GAME_OVER, g.phase(), "game over when only one player is left");
        assertEquals(red, g.finishOrder().get(0), "red finished first in this game");
        assertEquals(3, g.finishOrder().size(), "every player is ranked");
        GameRecord rec = g.toRecord();
        assertEquals(Difficulty.EASY, rec.difficulty(), "record difficulty");
        assertEquals(3, rec.ranking().size(), "record ranking");
    }

    // ---------------------------------------------------------------- capture and protection

    @Test
    void landingOnASingleOpponentPieceCapturesIt() {
        Game g = easyGame();
        red(g).piece(1).setPosition(3);
        green(g).piece(1).setPosition(greenIndexOfRed(g, 7));
        normal.then(4);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        assertTrue(green(g).piece(1).isInCastle(), "captured piece returns to its castle");
        assertEquals(7, red(g).piece(1).position(), "capturing piece stays on the square");
    }

    @Test
    void piecesOnASafeSquareCannotBeCaptured() {
        Game g = easyGame();
        board.square(board.path(PlayerColor.RED).get(7)).makeSafe();
        red(g).piece(1).setPosition(3);
        green(g).piece(1).setPosition(greenIndexOfRed(g, 7));
        normal.then(4);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        assertFalse(green(g).piece(1).isInCastle(), "no capture on a safe square");
    }

    @Test
    void piecesOnAStartSquareCannotBeCaptured() {
        Game g = easyGame();
        Cell greenStart = board.path(PlayerColor.GREEN).get(0);
        int redIdx = board.path(PlayerColor.RED).indexOf(greenStart);
        green(g).piece(1).setPosition(0);
        red(g).piece(1).setPosition(redIdx - 2);
        normal.then(2);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        assertEquals(0, green(g).piece(1).position(), "start square is protected");
    }

    private Game gameWithGreenBlockAtRedIndex(int redIndex) {
        Game g = easyGame();
        int gi = greenIndexOfRed(g, redIndex);
        green(g).piece(1).setPosition(gi);
        green(g).piece(2).setPosition(gi);
        return g;
    }

    @Test
    void aDoubleBlockCannotBePassed() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        normal.then(4);
        g.roll();
        assertEquals(1, g.options().size(), "offer the partial move when no full move exists");
        assertEquals(7, g.options().get(0).target(), "stop before the block at 8");
        g.choose(g.options().get(0));
        assertEquals(7, red(g).piece(1).position(), "execute the partial move");
        assertEquals(green(g), g.currentPlayer(), "advance the turn after the partial move");
    }

    @Test
    void aDoubleBlockCannotBeLandedOn() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        normal.then(3);
        g.roll();
        assertEquals(1, g.options().size(), "offer a partial move instead of landing on the block");
        assertEquals(7, g.options().get(0).target(), "never land on the block at 8");
    }

    @Test
    void aFullMoveTakesPriorityOverAPartialBlockedMove() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        red(g).piece(2).setPosition(1);
        normal.then(4);
        g.roll();
        assertEquals(1, g.options().size(), "only the full move is offered");
        assertEquals(red(g).piece(2), g.options().get(0).piece(), "use the unblocked piece");
        assertEquals(5, g.options().get(0).target(), "use all four steps");
    }

    @Test
    void enteringFromCastleTakesPriorityOverAPartialBlockedMove() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        normal.then(6);
        g.roll();
        assertEquals(3, g.options().size(), "the three castle pieces can enter");
        assertTrue(g.options().stream().allMatch(o -> o.kind() == MoveOption.Kind.ENTER),
                "do not offer partial movement when entry is possible");
    }

    @Test
    void anAdjacentBlockDoesNotOfferAZeroLengthMove() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(7);
        normal.then(4);
        g.roll();
        assertTrue(g.options().isEmpty(), "no movement is possible before the adjacent block");
        assertEquals(green(g), g.currentPlayer(), "pass the turn");
    }

    @Test
    void allPartialChoicesAreAvailableWhenNoFullMoveExists() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        red(g).piece(2).setPosition(6);
        normal.then(4);
        g.roll();
        assertEquals(2, g.options().size(), "both blocked pieces can stop before the block");
        assertTrue(g.options().stream().allMatch(o -> o.target() == 7), "both stop at 7");
    }

    @Test
    void movingInFrontOfADoubleBlockIsAllowed() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        red(g).piece(1).setPosition(5);
        normal.then(2);
        g.roll();
        assertEquals(1, g.options().size(), "5 -> 7 stops before the block");
    }

    @Test
    void aDoubleBlockIsBrokenWhenOnePieceMoves() {
        Game g = gameWithGreenBlockAtRedIndex(8);
        Cell blockCell = board.path(PlayerColor.RED).get(8);
        assertTrue(g.isBlockedFor(blockCell, red(g)), "blocked while both pieces stand there");
        green(g).piece(2).setPosition(green(g).piece(2).position() + 1);
        assertFalse(g.isBlockedFor(blockCell, red(g)), "a single piece is not a block");
    }

    @Test
    void aDoubleBlockOnTheStartSquareStopsEntering() {
        Game g = easyGame();
        Cell redStart = board.path(PlayerColor.RED).get(0);
        int gi = board.path(PlayerColor.GREEN).indexOf(redStart);
        green(g).piece(1).setPosition(gi);
        green(g).piece(2).setPosition(gi);
        normal.then(6);
        g.roll();
        assertTrue(g.options().isEmpty(), "start square is blocked by an opponent double block");
    }

    @Test
    void ownPiecesFormABlockAndCanShareASquare() {
        Game g = easyGame();
        red(g).piece(1).setPosition(4);
        red(g).piece(2).setPosition(2);
        normal.then(2);
        g.roll();
        g.chooseMoveFor(red(g).piece(2));
        assertEquals(2, g.piecesAt(board.path(PlayerColor.RED).get(4)).size(), "two red pieces on one square");
        assertTrue(g.isBlockedFor(board.path(PlayerColor.RED).get(4), green(g)), "it blocks green");
        assertFalse(g.isBlockedFor(board.path(PlayerColor.RED).get(4), red(g)), "it does not block red");
    }

    // ---------------------------------------------------------------- Boost / UltraBoost dice

    @Test
    void boostMultipliesTheNormalDieByNMinusOne() {
        Game g = game(Difficulty.MEDIUM, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
        red(g).piece(1).setPosition(10);
        normal.then(4);
        multiplier.then(3);
        g.roll();
        assertEquals(8, g.lastRoll().steps(), "4 x (3-1) = 8");
        assertEquals(18, g.options().get(0).target(), "moves 8");
    }

    @Test
    void boostOfOneGivesZeroStepsAndNoMove() {
        Game g = game(Difficulty.MEDIUM, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
        red(g).piece(1).setPosition(10);
        normal.then(5);
        multiplier.then(1);
        g.roll();
        assertEquals(0, g.lastRoll().steps(), "5 x 0 = 0");
        assertTrue(g.options().isEmpty(), "zero steps: nothing to move");
        assertEquals(green(g), g.currentPlayer(), "turn passes");
    }

    @Test
    void aSixWithBoostZeroStillEntersAPiece() {
        Game g = game(Difficulty.MEDIUM, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
        normal.then(6);
        multiplier.then(1);
        g.roll();
        assertEquals(4, g.options().size(), "entering uses only the normal die, even with multiplier x0");
    }

    @Test
    void aSixWithBoostOffersEnterOrTheMultipliedMove() {
        Game g = game(Difficulty.MEDIUM, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
        red(g).piece(1).setPosition(10);
        normal.then(6);
        multiplier.then(3);
        g.roll();
        assertTrue(g.options().stream().anyMatch(o -> o.kind() == MoveOption.Kind.MOVE && o.target() == 22),
                "move 6 x 2 = 12");
        assertTrue(g.options().stream().anyMatch(o -> o.kind() == MoveOption.Kind.ENTER), "or enter a piece");
    }

    @Test
    void hardLevelUsesUltraBoostWithMultipliersUpToFive() {
        Game g = game(Difficulty.HARD, new Random(1), PlayerColor.RED, PlayerColor.GREEN);
        red(g).piece(1).setPosition(10);
        normal.then(3);
        multiplier.then(6);
        g.roll();
        assertEquals(15, g.lastRoll().steps(), "3 x (6-1) = 15");
    }

    // ---------------------------------------------------------------- questions

    private Game gameWithQuestionAt(int redIndex, QuestionLevel level) {
        Game g = easyGame();
        board.square(board.path(PlayerColor.RED).get(redIndex)).makeQuestion(level);
        return g;
    }

    @Test
    void aCorrectAnswerMovesForwardByTheQuestionLevel() {
        for (QuestionLevel level : QuestionLevel.values()) {
            Game g = gameWithQuestionAt(10, level);
            red(g).piece(1).setPosition(6);
            normal.then(4);
            g.roll();
            g.chooseMoveFor(red(g).piece(1));
            assertEquals(Phase.ANSWERING_QUESTION, g.phase(), "question asked");
            assertTrue(g.answer(0), "answer 0 is correct");
            assertEquals(10 + level.steps(), red(g).piece(1).position(), level + " correct moves forward");
            assertEquals(green(g), g.currentPlayer(), "turn passes after the question");
        }
    }

    @Test
    void aWrongAnswerMovesBackByTheQuestionLevel() {
        for (QuestionLevel level : QuestionLevel.values()) {
            Game g = gameWithQuestionAt(10, level);
            red(g).piece(1).setPosition(6);
            normal.then(4);
            g.roll();
            g.chooseMoveFor(red(g).piece(1));
            assertFalse(g.answer(2), "answer 2 is wrong");
            assertEquals(10 - level.steps(), red(g).piece(1).position(), level + " wrong moves back");
        }
    }

    @Test
    void aWrongAnswerNeverMovesBehindTheStartSquare() {
        Game g = gameWithQuestionAt(1, QuestionLevel.HARD);
        red(g).piece(1).setPosition(0);
        normal.then(1);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        g.answer(3);
        assertEquals(0, red(g).piece(1).position(), "stops at the start square");
    }

    @Test
    void aCorrectAnswerStopsAtTheFinish() {
        int finish = BoardFactory.createLayout(Difficulty.EASY).finishIndex();
        Game g = gameWithQuestionAt(finish - 2, QuestionLevel.HARD);
        red(g).piece(1).setPosition(finish - 3);
        normal.then(1);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        g.answer(0);
        assertEquals(finish, red(g).piece(1).position(), "+3 from 2 squares away stops on the finish");
        assertTrue(red(g).piece(1).isFinished(), "piece finished");
        assertThrows(IllegalStateException.class, () -> g.answer(0), "can't answer twice");
    }

    @Test
    void aCorrectAnswerStopsBeforeAnOpponentBlock() {
        Game g = gameWithQuestionAt(15, QuestionLevel.HARD);
        int gi = greenIndexOfRed(g, 17);
        assertTrue(gi > 0, "square 17 is on green's path too");
        green(g).piece(1).setPosition(gi);
        green(g).piece(2).setPosition(gi);
        red(g).piece(1).setPosition(11);
        normal.then(4);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
        g.answer(0);
        assertEquals(16, red(g).piece(1).position(), "+3 stops in front of the block at 17");
    }

    @Test
    void actionsAreRejectedInTheWrongPhase() {
        Game g = easyGame();
        assertThrows(IllegalStateException.class, () -> g.answer(0), "no question pending");
        normal.then(6);
        g.roll();
        assertThrows(IllegalStateException.class, g::roll, "must choose a move before rolling again");
        MoveOption fake = new MoveOption(green(g).piece(1), MoveOption.Kind.ENTER, 0);
        assertThrows(IllegalArgumentException.class, () -> g.choose(fake), "can't move an opponent piece");
    }

    // ---------------------------------------------------------------- surprises

    private Game gameWithSurpriseAt(int redIndex, int surpriseRoll) {
        Game g = game(Difficulty.EASY, new FixedRandom(surpriseRoll), PlayerColor.RED, PlayerColor.GREEN);
        board.square(board.path(PlayerColor.RED).get(redIndex)).makeSurprise();
        return g;
    }

    private void landOnSurprise(Game g) {
        red(g).piece(1).setPosition(16);
        normal.then(4);
        g.roll();
        g.chooseMoveFor(red(g).piece(1));
    }

    @Test
    void surpriseRocketSendsThePieceToTheFinish() {
        Game g = gameWithSurpriseAt(20, 5);
        landOnSurprise(g);
        assertEquals(Surprise.ROCKET_TO_FINISH, g.lastSurprise(), "rocket");
        assertTrue(red(g).piece(1).isFinished(), "piece finished");
    }

    @Test
    void surpriseBackToCastle() {
        Game g = gameWithSurpriseAt(20, 25);
        landOnSurprise(g);
        assertEquals(Surprise.BACK_TO_CASTLE, g.lastSurprise(), "castle");
        assertTrue(red(g).piece(1).isInCastle(), "piece back in castle");
    }

    @Test
    void surpriseBackThreeSquares() {
        Game g = gameWithSurpriseAt(20, 45);
        landOnSurprise(g);
        assertEquals(Surprise.BACK_THREE, g.lastSurprise(), "back 3");
        assertEquals(17, red(g).piece(1).position(), "20 - 3");
    }

    @Test
    void surpriseLoseNextTurn() {
        Game g = gameWithSurpriseAt(20, 75);
        landOnSurprise(g);
        assertEquals(Surprise.LOSE_NEXT_TURN, g.lastSurprise(), "lose turn");
        assertEquals(green(g), g.currentPlayer(), "green plays");
        normal.then(2);
        g.roll(); // green has no pieces out -> turn passes, red is skipped -> back to green
        assertEquals(green(g), g.currentPlayer(), "red's turn was skipped");
        assertFalse(red(g).isSkipNextTurn(), "skip is used up");
    }

    @Test
    void surpriseUltraBoostBonusRollDoesNotCountTowardThreeSixes() {
        Game g = gameWithSurpriseAt(20, 95);
        landOnSurprise(g);
        assertEquals(Surprise.ULTRA_BONUS_ROLL, g.lastSurprise(), "bonus");
        assertEquals(red(g), g.currentPlayer(), "red rolls again");
        assertTrue(g.isBonusRollActive(), "bonus roll active");
        normal.then(6);
        g.roll();
        assertTrue(g.lastRoll().bonus(), "roll flagged as bonus");
        assertEquals(12, g.lastRoll().steps(), "6 x UltraBoost(3 -> x2)");
        assertEquals(0, g.consecutiveSixes(), "bonus six is not counted");
    }

    @Test
    void surpriseMoveOpponentPushesTheChosenPieceBackHalfARoll() {
        Game g = gameWithSurpriseAt(20, 15);
        green(g).piece(1).setPosition(30);
        landOnSurprise(g);
        assertEquals(Surprise.MOVE_OPPONENT, g.lastSurprise(), "sabotage");
        assertEquals(Phase.CHOOSING_OPPONENT, g.phase(), "player chooses the opponent piece");
        normal.then(5);
        int moved = g.chooseOpponentPiece(green(g).piece(1));
        assertEquals(3, moved, "half of 5 rounds up to 3");
        assertEquals(27, green(g).piece(1).position(), "moved back");
        assertEquals(green(g), g.currentPlayer(), "turn passes");
    }

    @Test
    void aPieceMovedBackBySabotageDoesNotTriggerItsNewSquare() {
        Game g = gameWithSurpriseAt(20, 15);
        green(g).piece(1).setPosition(30);
        board.square(board.path(PlayerColor.GREEN).get(27)).makeQuestion(QuestionLevel.EASY);
        landOnSurprise(g);
        assertEquals(Phase.CHOOSING_OPPONENT, g.phase(), "player chooses the opponent piece");
        normal.then(5);
        g.chooseOpponentPiece(green(g).piece(1));
        assertEquals(27, green(g).piece(1).position(), "pushed onto the question square");
        assertEquals(null, g.pendingQuestion(), "no question for the saboteur to answer");
        assertEquals(green(g), g.currentPlayer(), "turn passes normally");
        assertEquals(Phase.WAITING_FOR_ROLL, g.phase(), "next player rolls");
    }

    @Test
    void aPieceMovedBackBySabotageStillCaptures() {
        Game g = gameWithSurpriseAt(20, 15);
        green(g).piece(1).setPosition(30);
        Cell target = board.path(PlayerColor.GREEN).get(27);
        assertFalse(board.square(target).isProtected(), "test needs an unprotected square");
        Piece victim = red(g).piece(2);
        victim.setPosition(board.path(PlayerColor.RED).indexOf(target));
        landOnSurprise(g);
        assertEquals(Phase.CHOOSING_OPPONENT, g.phase(), "player chooses the opponent piece");
        normal.then(5);
        g.chooseOpponentPiece(green(g).piece(1));
        assertEquals(27, green(g).piece(1).position(), "pushed back 3 squares");
        assertTrue(victim.isInCastle(), "the single red piece on that square is captured");
    }

    @Test
    void surpriseWeightsMatchTheSpecification() {
        Map<Surprise, Integer> counts = new EnumMap<>(Surprise.class);
        Random rng = new Random(42);
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            counts.merge(Surprise.draw(rng), 1, Integer::sum);
        }
        int total = 0;
        for (Surprise s : Surprise.values()) {
            total += s.weightPercent();
            double pct = 100.0 * counts.getOrDefault(s, 0) / n;
            assertTrue(Math.abs(pct - s.weightPercent()) < 0.6, s + " observed " + pct + "% vs " + s.weightPercent() + "%");
        }
        assertEquals(100, total, "weights sum to 100");
    }

    // ---------------------------------------------------------------- setup validation

    @Test
    void setupRulesAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> Game.builder().addPlayer("A", PlayerColor.RED, false).build(),
                "at least 2 players");
        assertThrows(IllegalArgumentException.class, () -> Game.builder().addPlayer("A", PlayerColor.RED, false)
                .addPlayer("B", PlayerColor.RED, false).build(), "colours must be unique");
        assertThrows(IllegalArgumentException.class, () -> Game.builder().addPlayer("A", PlayerColor.RED, false)
                .addPlayer("a", PlayerColor.BLUE, false).build(), "names must be unique");
        assertThrows(IllegalArgumentException.class, () -> Game.builder().addPlayer(" ", PlayerColor.RED, false)
                .addPlayer("B", PlayerColor.BLUE, false).build(), "names must not be empty");
        assertThrows(IllegalArgumentException.class, () -> Game.builder().difficulty(Difficulty.MEDIUM)
                .addPlayer("A", PlayerColor.RED, false).addPlayer("B", PlayerColor.BLUE, false).build(),
                "medium needs a question bank");
        Game ok = Game.builder().addPlayer("Yo", PlayerColor.YELLOW, false).addPlayer("Re", PlayerColor.RED, false).build();
        assertEquals(PlayerColor.RED, ok.players().get(0).color(), "turn order follows seating (red first)");
    }

    @Test
    void listenersReceiveEvents() {
        Game g = easyGame();
        StringBuilder log = new StringBuilder();
        g.addListener((game, e) -> log.append(e.type()).append(' '));
        normal.then(5);
        g.roll();
        assertTrue(log.toString().contains("ROLLED"), "rolled event");
        assertTrue(log.toString().contains("TURN_CHANGED"), "turn changed event");
    }
}
