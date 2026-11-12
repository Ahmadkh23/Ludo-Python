package com.eduludo.Controller;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.Game;
import com.eduludo.Model.QuestionLevel;
import com.eduludo.Model.ai.ComputerPlayer;
import com.eduludo.persistence.HistoryRepository;
import com.eduludo.security.AuthService;
import com.eduludo.service.QuestionService;
import com.eduludo.View.AdminPanel;
import com.eduludo.View.Dialogs;
import com.eduludo.View.GamePanel;
import com.eduludo.View.HistoryPanel;
import com.eduludo.View.MainFrame;
import com.eduludo.View.SetupPanel;
import com.eduludo.View.StartPanel;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Main controller: owns the window and the screens, and moves between them
 * (start, setup, game, history, question manager).
 */
public final class AppController implements StartPanel.Actions, SetupPanel.Actions, GameController.Exit {

    private final QuestionService questions;
    private final HistoryRepository history;
    private final MainFrame frame;
    private final StartPanel startPanel;
    private final SetupPanel setupPanel;
    private final GamePanel gamePanel = new GamePanel();
    private final HistoryPanel historyPanel;
    private final AdminController adminController;
    private final Random random = new SecureRandom();
    private GameController current;
    private Difficulty lastDifficulty;
    private List<Game.PlayerSpec> lastPlayers;

    public AppController(QuestionService questions, AuthService auth, HistoryRepository history) {
        this.questions = questions;
        this.history = history;
        this.frame = new MainFrame(this::exit);
        this.startPanel = new StartPanel(this);
        this.setupPanel = new SetupPanel(this);
        this.historyPanel = new HistoryPanel(this::showStart);
        AdminPanel adminPanel = new AdminPanel();
        this.adminController = new AdminController(auth, questions, adminPanel, frame, this::showStart);
        frame.addScreen(MainFrame.START, startPanel);
        frame.addScreen(MainFrame.SETUP, setupPanel);
        frame.addScreen(MainFrame.GAME, gamePanel);
        frame.addScreen(MainFrame.HISTORY, historyPanel);
        frame.addScreen(MainFrame.ADMIN, adminPanel);
    }

    public void show() {
        showStart();
        frame.setVisible(true);
    }

    public void showStart() {
        Map<QuestionLevel, Integer> counts = questions.countByLevel();
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        startPanel.setBankInfo("Question bank: " + total + " questions (easy " + counts.get(QuestionLevel.EASY)
                + ", medium " + counts.get(QuestionLevel.MEDIUM) + ", hard " + counts.get(QuestionLevel.HARD) + ")");
        frame.show(MainFrame.START);
    }

    // ------------------------------------------------------------------ start screen

    @Override
    public void newGame() {
        frame.show(MainFrame.SETUP);
    }

    @Override
    public void history() {
        try {
            historyPanel.setRecords(history.findAll());
        } catch (RuntimeException e) {
            Dialogs.error(frame, "The game history could not be read:\n" + e.getMessage());
        }
        frame.show(MainFrame.HISTORY);
    }

    @Override
    public void questionManager() {
        adminController.open();
    }

    @Override
    public void rules() {
        Dialogs.rules(frame);
    }

    @Override
    public void exit() {
        if (current != null && current.isRunning()
                && !Dialogs.confirm(frame, "Exit", "A game is in progress. Exit anyway? It will not be saved.")) {
            return;
        }
        if (current != null) {
            current.stop();
        }
        frame.dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ setup screen

    @Override
    public void startGame(Difficulty difficulty, List<Game.PlayerSpec> players) {
        Game game;
        try {
            Game.Builder builder = Game.builder().difficulty(difficulty).questions(questions).random(random);
            for (Game.PlayerSpec p : players) {
                builder.addPlayer(p.name(), p.color(), p.computer());
            }
            game = builder.build();
        } catch (IllegalArgumentException e) {
            Dialogs.error(frame, e.getMessage());
            return;
        }
        lastDifficulty = difficulty;
        lastPlayers = List.copyOf(players);
        current = new GameController(game, gamePanel, new ComputerPlayer(random), history, this);
        frame.show(MainFrame.GAME);
        current.start();
    }

    @Override
    public void back() {
        showStart();
    }

    // ------------------------------------------------------------------ game screen

    @Override
    public void gameEnded(boolean playAgain) {
        current = null;
        if (playAgain) {
            setupPanel.prefill(lastDifficulty, lastPlayers);
            startGame(lastDifficulty, lastPlayers);
        } else {
            showStart();
        }
    }

    @Override
    public void gameQuit() {
        current = null;
        showStart();
    }
}

