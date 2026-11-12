package com.eduludo.Model;

import com.eduludo.Model.dice.DiceRoll;
import com.eduludo.Model.dice.DiceStrategy;
import com.eduludo.Model.dice.NormalDice;
import com.eduludo.Model.dice.UltraBoostDice;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * The game engine (the "M" in MVC). Holds the complete game state and enforces every rule of the specification.
 * It has no UI code: views observe it through {@link GameListener} and controllers call its actions.
 *
 * <h2>Turn flow (State pattern, see {@link Phase})</h2>
 * <ol>
 *   <li>{@link #roll()} in WAITING_FOR_ROLL: rolls the normal die (plus the Boost/UltraBoost die on MEDIUM/HARD).</li>
 *   <li>{@link #choose(MoveOption)} in CHOOSING_MOVE: moves the chosen piece and resolves the square it lands on.</li>
 *   <li>{@link #answer(int)} in ANSWERING_QUESTION, {@link #chooseOpponentPiece(Piece)} in CHOOSING_OPPONENT.</li>
 * </ol>
 *
 * <h2>Rules implemented</h2>
 * <ul>
 *   <li>A piece leaves the castle only with a 6 on the normal die; the multiplier is then ignored.
 *       With a 6 the player may instead use the multiplied value on a piece that is already on the board.</li>
 *   <li>A 6 on the normal die gives an extra turn; the third 6 in a row is cancelled and the turn passes.</li>
 *   <li>The finish needs an exact roll; a move past it is illegal.</li>
 *   <li>Capture: landing on an unprotected square with a single opponent piece sends it back to its castle.</li>
 *   <li>Double block: two pieces of the same colour on a square stop opponents from landing on it or passing it.</li>
 *   <li>Question squares: correct answer +1/+2/+3 steps, wrong answer -1/-2/-3 by level.</li>
 *   <li>Surprise squares: see {@link Surprise}.</li>
 *   <li>The game ends when all players but one have finished; the finishing order is recorded.</li>
 * </ul>
 */
public final class Game {

    private final Board board;
    private final List<Player> players;
    private final DiceStrategy normalDie;
    private final DiceStrategy multiplierDie;
    private final DiceStrategy bonusDie;
    private final QuestionProvider questions;
    private final Random rng;
    private final LongSupplier clock;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    private int currentIndex;
    private Phase phase = Phase.WAITING_FOR_ROLL;
    private int consecutiveSixes;
    private boolean extraTurnOwed;
    private boolean bonusRollPending;
    private boolean bonusRollActive;
    private DiceRoll lastRoll;
    private List<MoveOption> options = List.of();
    private Question pendingQuestion;
    private Piece pendingPiece;
    private List<Piece> opponentCandidates = List.of();
    private Surprise lastSurprise;
    private final List<Player> finishOrder = new ArrayList<>();
    private final long startedAt;
    private long endedAt = -1;

    private Game(Builder b) {
        this.board = b.board != null ? b.board : BoardFactory.create(b.difficulty, b.rng);
        this.normalDie = b.normalDie != null ? b.normalDie : new NormalDice();
        this.multiplierDie = b.multiplierDieSet ? b.multiplierDie : board.difficulty().createMultiplierDie();
        this.bonusDie = b.bonusDie != null ? b.bonusDie : new UltraBoostDice();
        this.questions = b.questions;
        this.rng = b.rng;
        this.clock = b.clock;
        List<Player> list = new ArrayList<>();
        for (PlayerSpec spec : b.specs) {
            list.add(new Player(spec.name(), spec.color(), spec.computer(), board.finishIndex()));
        }
        this.players = Collections.unmodifiableList(list);
        this.startedAt = clock.getAsLong();
    }

    // ------------------------------------------------------------------ queries

    public Board board() {
        return board;
    }

    public Difficulty difficulty() {
        return board.difficulty();
    }

    public List<Player> players() {
        return players;
    }

    public Player currentPlayer() {
        return players.get(currentIndex);
    }

    public Phase phase() {
        return phase;
    }

    public DiceRoll lastRoll() {
        return lastRoll;
    }

    /** True while the current roll is the surprise bonus roll (UltraBoost). */
    public boolean isBonusRollActive() {
        return bonusRollActive;
    }

    /** Legal moves for the current roll (only meaningful in CHOOSING_MOVE). */
    public List<MoveOption> options() {
        return options;
    }

    public Question pendingQuestion() {
        return pendingQuestion;
    }

    public Piece pendingPiece() {
        return pendingPiece;
    }

    /** Opponent pieces that may be pushed back by the "move opponent" surprise. */
    public List<Piece> opponentCandidates() {
        return opponentCandidates;
    }

    public Surprise lastSurprise() {
        return lastSurprise;
    }

    public List<Player> finishOrder() {
        return Collections.unmodifiableList(finishOrder);
    }

    public int consecutiveSixes() {
        return consecutiveSixes;
    }

    /** The multiplier die used on this level, or null on EASY. */
    public DiceStrategy multiplierDie() {
        return multiplierDie;
    }

    public long elapsedSeconds() {
        long end = endedAt >= 0 ? endedAt : clock.getAsLong();
        return Math.max(0, (end - startedAt) / 1000);
    }

    public boolean isOver() {
        return phase == Phase.GAME_OVER;
    }

    /** The cell a piece stands on, or null while it is in its castle. */
    public Cell cellOf(Piece piece) {
        return piece.isInCastle() ? null : board.path(piece.owner().color()).get(piece.position());
    }

    public Square squareOf(Piece piece) {
        Cell c = cellOf(piece);
        return c == null ? null : board.square(c);
    }

    /** All pieces (of every player) currently standing on the cell. */
    public List<Piece> piecesAt(Cell cell) {
        List<Piece> result = new ArrayList<>();
        for (Player p : players) {
            for (Piece piece : p.pieces()) {
                if (!piece.isInCastle() && cell.equals(cellOf(piece))) {
                    result.add(piece);
                }
            }
        }
        return result;
    }

    /** True if some player other than {@code mover} has two or more pieces on the cell (a Double Block). */
    public boolean isBlockedFor(Cell cell, Player mover) {
        for (Player p : players) {
            if (p == mover) {
                continue;
            }
            int count = 0;
            for (Piece piece : p.pieces()) {
                if (!piece.isInCastle() && cell.equals(cellOf(piece))) {
                    count++;
                }
            }
            if (count >= 2) {
                return true;
            }
        }
        return false;
    }

    /** True if the move would capture at least one opponent piece (used by the computer player and the UI). */
    public boolean wouldCapture(MoveOption option) {
        Player mover = option.piece().owner();
        Cell target = board.path(mover.color()).get(option.target());
        Square sq = board.square(target);
        if (sq.isProtected()) {
            return false;
        }
        for (Player p : players) {
            if (p != mover && countAt(p, target) == 1) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ listeners

    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void fire(GameEvent.Type type, String message) {
        GameEvent event = new GameEvent(type, message);
        for (GameListener l : listeners) {
            l.onGameEvent(this, event);
        }
    }

    /** Announces the first player. Call once after listeners are attached. */
    public void start() {
        fire(GameEvent.Type.GAME_STARTED, "Game started on " + difficulty().displayName() + " level. "
                + currentPlayer().name() + " (" + currentPlayer().color().displayName() + ") rolls first.");
    }

    // ------------------------------------------------------------------ actions

    /** Rolls the dice for the current player. */
    public DiceRoll roll() {
        requirePhase(Phase.WAITING_FOR_ROLL);
        Player player = currentPlayer();
        boolean bonus = bonusRollActive;
        int normal = normalDie.roll(rng);
        DiceStrategy multDie = bonus ? bonusDie : multiplierDie;
        int face = multDie == null ? 0 : multDie.roll(rng);
        int steps = multDie == null ? normal : normal * (face - 1);
        lastRoll = new DiceRoll(normal, face, multDie == null ? null : multDie.name(), steps, bonus);
        fire(GameEvent.Type.ROLLED, player.name() + (bonus ? " (bonus roll)" : "") + " rolled " + lastRoll.describe() + ".");

        if (bonus && normal == 6) extraTurnOwed = true;
        if (!bonus) {
            if (normal == 6) {
                consecutiveSixes++;
                if (consecutiveSixes == 3) {
                    fire(GameEvent.Type.THIRD_SIX, "Third 6 in a row! The roll is cancelled and the turn passes.");
                    nextPlayer();
                    return lastRoll;
                }
                extraTurnOwed = true;
            } else {
                consecutiveSixes = 0;
                extraTurnOwed = false;
            }
        }

        options = computeOptions(player, lastRoll);
        if (options.isEmpty()) {
            fire(GameEvent.Type.NO_MOVES, player.name() + " has no legal move.");
            finishAction();
        } else {
            phase = Phase.CHOOSING_MOVE;
        }
        return lastRoll;
    }

    /** Performs one of {@link #options()}. */
    public void choose(MoveOption option) {
        requirePhase(Phase.CHOOSING_MOVE);
        if (!options.contains(option)) {
            throw new IllegalArgumentException("Not a legal move: " + option);
        }
        options = List.of();
        Piece piece = option.piece();
        if (option.kind() == MoveOption.Kind.ENTER) {
            piece.setPosition(0);
            fire(GameEvent.Type.ENTERED, piece + " enters the board.");
            land(piece, false);
        } else {
            int moved = option.target() - piece.position();
            piece.setPosition(option.target());
            fire(GameEvent.Type.MOVED, piece + " moves " + moved + " squares.");
            land(piece, true);
        }
    }

    /** Convenience for the UI: performs the legal move of this piece, if there is one. */
    public boolean chooseMoveFor(Piece piece) {
        for (MoveOption o : options) {
            if (o.piece() == piece) {
                choose(o);
                return true;
            }
        }
        return false;
    }

    /** Answers the pending question with answer index 0 to 3. Returns true if correct. */
    public boolean answer(int answerIndex) {
        requirePhase(Phase.ANSWERING_QUESTION);
        if (answerIndex < 0 || answerIndex >= Question.OPTION_COUNT) {
            throw new IllegalArgumentException("Answer must be between 0 and 3");
        }
        Question q = pendingQuestion;
        Piece piece = pendingPiece;
        pendingQuestion = null;
        pendingPiece = null;
        boolean correct = q.isCorrect(answerIndex);
        int k = q.level().steps();
        int moved = correct ? advanceFreely(piece, k) : moveBack(piece, k);
        fire(GameEvent.Type.QUESTION_ANSWERED, correct
                ? "Correct! " + piece + " moves " + moved + " forward."
                : "Wrong (the answer was: " + q.options().get(q.correctIndex()) + "). " + piece + " moves " + moved + " back.");
        if (moved > 0) land(piece, true);
        else finishAction();
        return correct;
    }

    /** Surprise "move an opponent": rolls the normal die and moves the chosen opponent piece back half of it. */
    public int chooseOpponentPiece(Piece target) {
        requirePhase(Phase.CHOOSING_OPPONENT);
        if (!opponentCandidates.contains(target)) {
            throw new IllegalArgumentException("Not a valid opponent piece: " + target);
        }
        opponentCandidates = List.of();
        int roll = normalDie.roll(rng);
        int half = (roll + 1) / 2;
        int moved = moveBack(target, half);
        fire(GameEvent.Type.OPPONENT_MOVED, "Rolled " + roll + ": " + target + " moves back " + moved + " (half of " + roll + ").");
        // The pushed piece can still capture, but its new square's question/surprise is not triggered:
        // it is not its owner's turn, so the current player must not answer for it or win its bonus roll.
        if (moved > 0) land(target, false);
        else finishAction();
        return moved;
    }

    // ------------------------------------------------------------------ rules

    private List<MoveOption> computeOptions(Player player, DiceRoll roll) {
        List<MoveOption> result = new ArrayList<>();
        List<MoveOption> partialMoves = new ArrayList<>();
        List<Cell> path = board.path(player.color());
        int finish = board.finishIndex();
        for (Piece piece : player.pieces()) {
            if (piece.isFinished()) {
                continue;
            }
            if (piece.isInCastle()) {
                // Leaving the castle uses the normal die only.
                if (roll.normal() == 6 && !isBlockedFor(path.get(0), player)) {
                    result.add(new MoveOption(piece, MoveOption.Kind.ENTER, 0));
                }
            } else {
                int steps = squareOf(piece).type() == SquareType.HOME ? roll.normal() : roll.steps();
                if (steps <= 0) continue;
                int target = piece.position() + steps;
                if (target <= finish) {
                    int reachable = piece.position();
                    while (reachable < target && !isBlockedFor(path.get(reachable + 1), player)) {
                        reachable++;
                    }
                    if (reachable == target) {
                        result.add(new MoveOption(piece, MoveOption.Kind.MOVE, target));
                    } else if (reachable > piece.position()) {
                        partialMoves.add(new MoveOption(piece, MoveOption.Kind.MOVE, reachable));
                    }
                }
            }
        }
        // A full move (including castle entry) must be used when available.
        // Only otherwise may a piece stop immediately before an opponent block.
        return List.copyOf(result.isEmpty() ? partialMoves : result);
    }

    private void land(Piece piece, boolean squareEffects) {
        while (true) {
            if (piece.isInCastle()) { finishAction(); return; }
            Square square = squareOf(piece);
            if (!square.isProtected()) captureAt(piece, square.cell());
            if (piece.isFinished()) { onPieceFinished(piece); finishAction(); return; }
            if (squareEffects && square.type() == SquareType.QUESTION) {
                if (questions == null) throw new IllegalStateException("No question provider configured");
                pendingQuestion = questions.draw(square.questionLevel());
                pendingPiece = piece;
                phase = Phase.ANSWERING_QUESTION;
                fire(GameEvent.Type.QUESTION_ASKED, piece + " landed on a "
                        + square.questionLevel().displayName().toLowerCase(Locale.ROOT) + " question square.");
                return;
            }
            if (squareEffects && square.type() == SquareType.SURPRISE) {
                lastSurprise = Surprise.draw(rng);
                fire(GameEvent.Type.SURPRISE, "Surprise for " + piece + ": " + lastSurprise.description());
                int before = piece.position();
                if (lastSurprise.apply(this, piece)) return;
                if (piece.position() != before) continue;
            }
            finishAction();
            return;
        }
    }

    private void captureAt(Piece mover, Cell cell) {
        for (Player p : players) {
            if (p == mover.owner()) {
                continue;
            }
            if (countAt(p, cell) == 1) {
                for (Piece victim : p.pieces()) {
                    if (!victim.isInCastle() && cell.equals(cellOf(victim))) {
                        victim.setPosition(Piece.IN_CASTLE);
                        fire(GameEvent.Type.CAPTURED, mover + " captured " + victim + "! It goes back to its castle.");
                    }
                }
            }
        }
    }

    private int countAt(Player p, Cell cell) {
        int count = 0;
        for (Piece piece : p.pieces()) {
            if (!piece.isInCastle() && cell.equals(cellOf(piece))) {
                count++;
            }
        }
        return count;
    }

    /** Moves forward up to k squares, stopping before an opponent block or at the finish. Returns squares moved. */
    private int advanceFreely(Piece piece, int k) {
        if (piece.isInCastle() || piece.isFinished()) {
            return 0;
        }
        List<Cell> path = board.path(piece.owner().color());
        int moved = 0;
        while (moved < k && piece.position() < board.finishIndex()
                && !isBlockedFor(path.get(piece.position() + 1), piece.owner())) {
            piece.setPosition(piece.position() + 1);
            moved++;
        }
        return moved;
    }

    /** Moves back up to k squares, never behind the START square and never onto an opponent block. */
    private int moveBack(Piece piece, int k) {
        if (piece.isInCastle() || piece.isFinished()) {
            return 0;
        }
        List<Cell> path = board.path(piece.owner().color());
        int moved = 0;
        while (moved < k && piece.position() > 0 && !isBlockedFor(path.get(piece.position() - 1), piece.owner())) {
            piece.setPosition(piece.position() - 1);
            moved++;
        }
        return moved;
    }

    private void onPieceFinished(Piece piece) {
        fire(GameEvent.Type.PIECE_FINISHED, piece + " reached the finish!");
        Player owner = piece.owner();
        if (owner.hasFinished() && !finishOrder.contains(owner)) {
            finishOrder.add(owner);
            fire(GameEvent.Type.PLAYER_FINISHED, owner.name() + " finished in place " + finishOrder.size() + "!");
        }
    }

    /** Ends the current action: game over, bonus roll, extra turn for a 6, or the next player. */
    private void finishAction() {
        options = List.of();
        if (checkGameOver()) {
            return;
        }
        Player player = currentPlayer();
        if (player.hasFinished()) {
            nextPlayer();
            return;
        }
        if (bonusRollPending) {
            bonusRollPending = false;
            bonusRollActive = true;
            phase = Phase.WAITING_FOR_ROLL;
            fire(GameEvent.Type.BONUS_ROLL, player.name() + " gets a bonus roll with the UltraBoost die.");
            return;
        }
        bonusRollActive = false;
        if (extraTurnOwed) {
            extraTurnOwed = false;
            phase = Phase.WAITING_FOR_ROLL;
            fire(GameEvent.Type.EXTRA_TURN, player.name() + " rolled a 6 and rolls again.");
            return;
        }
        nextPlayer();
    }

    private boolean checkGameOver() {
        long remaining = players.stream().filter(p -> !p.hasFinished()).count();
        if (remaining > 1) {
            return false;
        }
        for (Player p : players) {
            if (!finishOrder.contains(p)) {
                finishOrder.add(p);
            }
        }
        phase = Phase.GAME_OVER;
        endedAt = clock.getAsLong();
        fire(GameEvent.Type.GAME_OVER, "Game over! Winner: " + finishOrder.get(0).name() + ".");
        return true;
    }

    private void nextPlayer() {
        consecutiveSixes = 0;
        extraTurnOwed = false;
        bonusRollPending = false;
        bonusRollActive = false;
        options = List.of();
        int n = players.size();
        int idx = currentIndex;
        for (int i = 0; i < 2 * n; i++) {
            idx = (idx + 1) % n;
            Player candidate = players.get(idx);
            if (candidate.hasFinished()) {
                continue;
            }
            if (candidate.isSkipNextTurn()) {
                candidate.setSkipNextTurn(false);
                fire(GameEvent.Type.TURN_SKIPPED, candidate.name() + " loses this turn.");
                continue;
            }
            currentIndex = idx;
            phase = Phase.WAITING_FOR_ROLL;
            fire(GameEvent.Type.TURN_CHANGED, "It's " + candidate.name() + "'s turn (" + candidate.color().displayName() + ").");
            return;
        }
        throw new IllegalStateException("No player can take a turn");
    }

    private void requirePhase(Phase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Action not allowed in phase " + phase + " (expected " + expected + ")");
        }
    }

    // ------------------------------------------------------------------ surprise hooks (called by Surprise)

    void surpriseSendToFinish(Piece piece) {
        piece.setPosition(board.finishIndex());
    }

    void surpriseSendToCastle(Piece piece) {
        piece.setPosition(Piece.IN_CASTLE);
    }

    void surpriseMoveBack(Piece piece, int squares) {
        moveBack(piece, squares);
    }

    void surpriseLoseNextTurn(Player player) {
        player.setSkipNextTurn(true);
    }

    void surpriseGrantBonusRoll() {
        bonusRollPending = true;
    }

    /** Returns true if the player must now choose an opponent piece. */
    boolean surpriseRequestOpponent(Piece piece) {
        List<Piece> candidates = new ArrayList<>();
        for (Player p : players) {
            if (p != piece.owner()) {
                for (Piece op : p.pieces()) {
                    if (op.isActive() && op.position() > 0) {
                        candidates.add(op);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            fire(GameEvent.Type.OPPONENT_MOVED, "No opponent piece can be moved back. Nothing happens.");
            return false;
        }
        opponentCandidates = List.copyOf(candidates);
        phase = Phase.CHOOSING_OPPONENT;
        return true;
    }

    // ------------------------------------------------------------------ history

    /** Builds the history entry for a finished game. */
    public GameRecord toRecord() {
        if (!isOver()) {
            throw new IllegalStateException("The game is not over yet");
        }
        List<GameRecord.RankedPlayer> ranking = new ArrayList<>();
        for (int i = 0; i < finishOrder.size(); i++) {
            Player p = finishOrder.get(i);
            ranking.add(new GameRecord.RankedPlayer(i + 1, p.name(), p.color(), p.isComputer()));
        }
        return new GameRecord(UUID.randomUUID().toString().substring(0, 8), Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDateTime(), difficulty(),
                elapsedSeconds(), ranking);
    }

    // ------------------------------------------------------------------ builder

    /** A seat at the table before the game starts. */
    public record PlayerSpec(String name, PlayerColor color, boolean computer) {
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder pattern: validates the setup (2-4 players, unique colours and names) and creates the game. */
    public static final class Builder {
        public static final int MIN_PLAYERS = 2;
        public static final int MAX_PLAYERS = 4;
        public static final int MAX_NAME_LENGTH = 20;

        private Difficulty difficulty = Difficulty.EASY;
        private final List<PlayerSpec> specs = new ArrayList<>();
        private QuestionProvider questions;
        private Random rng = new Random();
        private LongSupplier clock = System::currentTimeMillis;
        private Board board;
        private DiceStrategy normalDie;
        private DiceStrategy multiplierDie;
        private boolean multiplierDieSet;
        private DiceStrategy bonusDie;

        public Builder difficulty(Difficulty difficulty) {
            this.difficulty = Objects.requireNonNull(difficulty);
            return this;
        }

        public Builder addPlayer(String name, PlayerColor color, boolean computer) {
            specs.add(new PlayerSpec(name == null ? "" : name.strip(), color, computer));
            return this;
        }

        public Builder questions(QuestionProvider questions) {
            this.questions = questions;
            return this;
        }

        public Builder random(Random rng) {
            this.rng = Objects.requireNonNull(rng);
            return this;
        }

        public Builder clock(LongSupplier clock) {
            this.clock = Objects.requireNonNull(clock);
            return this;
        }

        /** Use a prepared board (tests). Its difficulty overrides {@link #difficulty}. */
        public Builder board(Board board) {
            this.board = board;
            return this;
        }

        public Builder normalDie(DiceStrategy die) {
            this.normalDie = die;
            return this;
        }

        public Builder multiplierDie(DiceStrategy die) {
            this.multiplierDie = die;
            this.multiplierDieSet = true;
            return this;
        }

        public Builder bonusDie(DiceStrategy die) {
            this.bonusDie = die;
            return this;
        }

        public Game build() {
            if (specs.size() < MIN_PLAYERS || specs.size() > MAX_PLAYERS) {
                throw new IllegalArgumentException("A game needs 2 to 4 players");
            }
            Set<PlayerColor> colors = new HashSet<>();
            Set<String> names = new HashSet<>();
            for (PlayerSpec s : specs) {
                if (s.color() == null) {
                    throw new IllegalArgumentException("Every player needs a colour");
                }
                if (!colors.add(s.color())) {
                    throw new IllegalArgumentException("Each player must have a different colour");
                }
                if (s.name().isEmpty()) {
                    throw new IllegalArgumentException("Every player needs a name");
                }
                if (s.name().length() > MAX_NAME_LENGTH) {
                    throw new IllegalArgumentException("Names can have at most " + MAX_NAME_LENGTH + " characters");
                }
                if (s.name().indexOf(',') >= 0 || s.name().indexOf('"') >= 0
                        || s.name().indexOf('\n') >= 0 || s.name().indexOf('\r') >= 0) {
                    throw new IllegalArgumentException("Names cannot contain commas, quotation marks or line breaks");
                }
                if (!names.add(s.name().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Each player must have a different name");
                }
            }
            Difficulty level = board != null ? board.difficulty() : difficulty;
            if (level != Difficulty.EASY && questions == null) {
                throw new IllegalArgumentException("Medium and Hard games need a question bank");
            }
            // Turn order follows the clockwise seating order of the colours.
            specs.sort((a, b) -> Integer.compare(a.color().ordinal(), b.color().ordinal()));
            return new Game(this);
        }
    }
}
