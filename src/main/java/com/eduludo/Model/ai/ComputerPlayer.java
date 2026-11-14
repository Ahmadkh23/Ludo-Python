package com.eduludo.Model.ai;

import com.eduludo.Model.Cell;
import com.eduludo.Model.Game;
import com.eduludo.Model.MoveOption;
import com.eduludo.Model.Piece;
import com.eduludo.Model.Player;
import com.eduludo.Model.Question;
import com.eduludo.Model.Square;
import com.eduludo.Model.SquareType;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Bonus feature: decisions for a computer-controlled seat. It only reads the public game state and returns
 * a decision; it never changes the game itself (the controller performs the action).
 * <p>
 * Move scoring (highest wins): finishing a piece, capturing, entering a new piece, landing on a protected
 * square, forming a double block, then plain progress. Squares where an opponent could capture next turn
 * are penalised.
 */
public final class ComputerPlayer {

    private final Random rng;

    public ComputerPlayer(Random rng) {
        this.rng = rng;
    }

    public MoveOption chooseMove(Game game) {
        List<MoveOption> options = game.options();
        return options.stream().max(Comparator.comparingInt(o -> score(game, o))).orElseThrow();
    }

    int score(Game game, MoveOption option) {
        Piece piece = option.piece();
        Player me = piece.owner();
        int finish = game.board().finishIndex();
        if (option.target() == finish) {
            return 10_000;
        }
        int score = 0;
        if (game.wouldCapture(option)) {
            score += 5_000;
        }
        if (option.kind() == MoveOption.Kind.ENTER) {
            score += 3_000;
        }
        Cell target = game.board().path(me.color()).get(option.target());
        Square square = game.board().square(target);
        if (square.isProtected()) {
            score += 1_500;
        } else if (isThreatened(game, me, target)) {
            score -= 2_000;
        }
        if (square.type() == SquareType.QUESTION) {
            score -= 200; // a wrong answer could move it back
        }
        long ownHere = game.piecesAt(target).stream().filter(p -> p.owner() == me).count();
        if (ownHere == 1) {
            score += 800; // forms a double block
        }
        if (piece.isActive() && !game.board().square(game.cellOf(piece)).isProtected()
                && isThreatened(game, me, game.cellOf(piece))) {
            score += 1_000; // escape from danger
        }
        score += option.target(); // prefer progress
        return score;
    }

    /** True if an opponent piece is 1 to 6 squares behind the cell on the opponent's own path. */
    private boolean isThreatened(Game game, Player me, Cell cell) {
        for (Player p : game.players()) {
            if (p == me) {
                continue;
            }
            List<Cell> path = game.board().path(p.color());
            int idx = path.indexOf(cell);
            if (idx < 0) {
                continue;
            }
            for (Piece op : p.pieces()) {
                if (op.isActive() && op.position() < idx && idx - op.position() <= 6) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Simulated knowledge: 80% / 60% / 40% chance to answer an easy / medium / hard question correctly. */
    public int chooseAnswer(Question question) {
        double chance = switch (question.level()) {
            case EASY -> 0.8;
            case MEDIUM -> 0.6;
            case HARD -> 0.4;
        };
        if (rng.nextDouble() < chance) {
            return question.correctIndex();
        }
        int wrong = rng.nextInt(Question.OPTION_COUNT - 1);
        return wrong >= question.correctIndex() ? wrong + 1 : wrong;
    }

    /** Pushes back the opponent piece that is furthest ahead. */
    public Piece chooseOpponentPiece(Game game) {
        return game.opponentCandidates().stream().max(Comparator.comparingInt(Piece::position)).orElseThrow();
    }
}

