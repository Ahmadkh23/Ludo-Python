package com.eduludo.Controller;

import com.eduludo.Model.Game;
import com.eduludo.Model.GameEvent;
import com.eduludo.Model.GameListener;
import com.eduludo.Model.GameRecord;
import com.eduludo.Model.MoveOption;
import com.eduludo.Model.Phase;
import com.eduludo.Model.Piece;
import com.eduludo.Model.Player;
import com.eduludo.Model.Question;
import com.eduludo.Model.ai.ComputerPlayer;
import com.eduludo.persistence.HistoryRepository;
import com.eduludo.View.Dialogs;
import com.eduludo.View.GamePanel;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller of one game: turns user actions (Roll, click a piece, answer) into calls on the {@link Game} model,
 * plays the computer seats, and saves the result to the history when the game ends.
 * It observes the model ({@link GameListener}) to update the view.
 */
public final class GameController implements GameListener, GamePanel.Actions {

    /** Called when the game screen is left. */
    public interface Exit {
        /** @param playAgain true for a rematch with the same settings */
        void gameEnded(boolean playAgain);

        void gameQuit();
    }

    private static final int COMPUTER_ROLL_DELAY = 700;
    private static final int COMPUTER_MOVE_DELAY = 550;
    private static final int COMPUTER_ANSWER_DELAY = 1100;
    private static final int AFTER_ROLL_DELAY = 480;
    private static final int SINGLE_OPTION_DELAY = 450;

    private final Game game;
    private final GamePanel view;
    private final ComputerPlayer computer;
    private final HistoryRepository history;
    private final Exit exit;
    private final List<Timer> timers = new ArrayList<>();
    private boolean busy;
    private boolean stopped;
    private boolean saved;

    public GameController(Game game, GamePanel view, ComputerPlayer computer, HistoryRepository history, Exit exit) {
        this.game = game;
        this.view = view;
        this.computer = computer;
        this.history = history;
        this.exit = exit;
    }

    public void start() {
        game.addListener(this);
        view.bind(game, this);
        game.start();
        next();
    }

    // ------------------------------------------------------------------ model events (Observer)

    @Override
    public void onGameEvent(Game g, GameEvent event) {
        view.log(event);
        if (event.type() == GameEvent.Type.ROLLED) {
            view.showRoll(g.lastRoll());
        }
        view.refresh();
    }

    // ------------------------------------------------------------------ user actions

    @Override
    public void roll() {
        if (canHumanAct(Phase.WAITING_FOR_ROLL)) {
            doRoll();
        }
    }

    @Override
    public void pieceClicked(Piece piece) {
        if (canHumanAct(Phase.CHOOSING_MOVE) && piece.owner() == game.currentPlayer() && game.chooseMoveFor(piece)) {
            next();
        }
    }

    @Override
    public void quit() {
        if (Dialogs.confirm(view, "Quit game", "Quit this game? It will not be saved in the history.")) {
            stop();
            exit.gameQuit();
        }
    }

    @Override
    public void showRules() {
        Dialogs.rules(view);
    }

    private boolean canHumanAct(Phase phase) {
        return !stopped && !busy && game.phase() == phase && !game.currentPlayer().isComputer();
    }

    // ------------------------------------------------------------------ turn flow

    private void doRoll() {
        view.setRollAllowed(false);
        busy = true;
        game.roll();
        schedule(AFTER_ROLL_DELAY, () -> {
            busy = false;
            next();
        });
    }

    /** Decides what happens next from the current phase. */
    private void next() {
        if (stopped) {
            return;
        }
        if (view.isAnimating()) {
            schedule(60, this::next); // let the pieces finish moving first
            return;
        }
        Player player = game.currentPlayer();
        boolean cpu = player.isComputer();
        view.refresh();
        switch (game.phase()) {
            case WAITING_FOR_ROLL -> {
                view.setRollAllowed(!cpu);
                if (cpu) {
                    schedule(COMPUTER_ROLL_DELAY, this::doRoll);
                }
            }
            case CHOOSING_MOVE -> {
                view.setRollAllowed(false);
                if (cpu) {
                    schedule(COMPUTER_MOVE_DELAY, () -> {
                        game.choose(computer.chooseMove(game));
                        next();
                    });
                } else if (game.options().size() == 1) {
                    // Only one legal move: play it for the player (they can still click it first).
                    MoveOption only = game.options().get(0);
                    schedule(SINGLE_OPTION_DELAY, () -> {
                        if (game.phase() == Phase.CHOOSING_MOVE && game.options().contains(only)) {
                            game.choose(only);
                            next();
                        }
                    });
                }
            }
            case ANSWERING_QUESTION -> {
                view.setRollAllowed(false);
                Question q = game.pendingQuestion();
                if (cpu) {
                    schedule(COMPUTER_ANSWER_DELAY, () -> {
                        game.answer(computer.chooseAnswer(q));
                        next();
                    });
                } else {
                    int answer = view.askQuestion(q, player);
                    if (!stopped) {
                        game.answer(answer);
                        next();
                    }
                }
            }
            case CHOOSING_OPPONENT -> {
                view.setRollAllowed(false);
                List<Piece> candidates = game.opponentCandidates();
                if (cpu) {
                    schedule(COMPUTER_MOVE_DELAY, () -> {
                        game.chooseOpponentPiece(computer.chooseOpponentPiece(game));
                        next();
                    });
                } else {
                    int idx = view.chooseOpponentPiece(candidates);
                    if (!stopped) {
                        game.chooseOpponentPiece(candidates.get(idx));
                        next();
                    }
                }
            }
            case GAME_OVER -> finish();
        }
    }

    private void finish() {
        if (saved) {
            return;
        }
        saved = true;
        view.setRollAllowed(false);
        GameRecord record = game.toRecord();
        try {
            history.save(record);
        } catch (RuntimeException e) {
            Dialogs.error(view, "The game could not be saved to the history:\n" + e.getMessage());
        }
        boolean again = view.showGameOver(record);
        stop();
        exit.gameEnded(again);
    }

    private void schedule(int delayMs, Runnable action) {
        Timer t = new Timer(delayMs, null);
        t.setRepeats(false);
        t.addActionListener(e -> {
            timers.remove(t);
            if (!stopped) {
                action.run();
            }
        });
        timers.add(t);
        t.start();
    }

    /** Stops all timers and detaches from the model and the view. */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        for (Timer t : new ArrayList<>(timers)) {
            t.stop();
        }
        timers.clear();
        game.removeListener(this);
        view.unbind();
    }

    public boolean isRunning() {
        return !stopped;
    }
}

