package com.eduludo.Model;

import com.eduludo.Model.ai.ComputerPlayer;
import com.eduludo.View.AdminPanel;
import com.eduludo.View.Dialogs;
import com.eduludo.View.GamePanel;
import com.eduludo.View.HistoryPanel;
import com.eduludo.View.QuestionDialog;
import com.eduludo.View.Theme;
import com.eduludo.persistence.CsvHistoryRepository;
import com.eduludo.persistence.QuestionJson;
import com.eduludo.persistence.QuestionRepository;
import com.eduludo.security.AuthService;
import com.eduludo.service.QuestionService;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Re-creates the system-report screenshots from real gameplay (needs a desktop; run report-screenshots.sh after
 * building). Seeded games are played by the real Game model; ComputerPlayer chooses every move (including for the
 * seats named Dana and Eli) and questions come from the real QuestionService. Game events go to the real GamePanel
 * log. Finished games are saved with the real CsvHistoryRepository into a history file that was started from the
 * lecturer's CSV exactly as the application does on first run, then read back and shown in the real HistoryPanel.
 * The game clock is simulated (3 seconds per action) so the screenshots are reproducible.
 */
public class ReportScreenshots {

    private static File out;
    private static String pendingDialog;

    public static void main(String[] args) throws Exception {
        out = new File(args[0]);
        out.mkdirs();

        List<Question> bank = QuestionJson.fromJson(new String(
                ReportScreenshots.class.getResourceAsStream("/com/eduludo/questions-default.json").readAllBytes(),
                StandardCharsets.UTF_8));
        QuestionService questions = questionService(bank);
        captureDialogsWhenTheyOpen();

        // 1. A Medium game in progress.
        AtomicLong now = new AtomicLong(millis(LocalDateTime.of(2026, 11, 8, 15, 30, 0)));
        Game[] holder = new Game[1];
        GamePanel[] panelHolder = new GamePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            Game g = Game.builder()
                    .board(BoardFactory.create(Difficulty.MEDIUM, new Random(7)))
                    .difficulty(Difficulty.MEDIUM)
                    .questions(questions)
                    .random(new Random(11))
                    .clock(now::get)
                    .addPlayer("Dana", PlayerColor.RED, false)
                    .addPlayer("Eli", PlayerColor.GREEN, false)
                    .addPlayer("Computer", PlayerColor.BLUE, true)
                    .build();
            GamePanel panel = new GamePanel();
            panel.bind(g, new GamePanel.Actions() {
                public void roll() { }
                public void pieceClicked(Piece p) { }
                public void quit() { }
                public void showRules() { }
            });
            g.addListener((game, event) -> panel.log(event));
            holder[0] = g;
            panelHolder[0] = panel;
        });
        Game game = holder[0];
        GamePanel panel = panelHolder[0];
        ComputerPlayer ai = new ComputerPlayer(new Random(5));

        boolean[] seenSurprise = {false};
        boolean[] seenQuestion = {false};
        game.addListener((g, event) -> {
            if (event.type() == GameEvent.Type.SURPRISE) seenSurprise[0] = true;
            if (event.type() == GameEvent.Type.QUESTION_ANSWERED) seenQuestion[0] = true;
        });
        game.start();
        int actions = 0;
        while (!interestingMoment(game, seenSurprise[0], seenQuestion[0], actions)) {
            SwingUtilities.invokeAndWait(() -> step(game, ai));
            now.addAndGet(3000);
            actions++;
            if (actions > 5000) {
                throw new IllegalStateException("No suitable mid-game moment found");
            }
        }
        System.out.println("mid-game after " + actions + " actions");
        captureMidGame(panel);

        // 2. Finish the same game, and play a short Easy game for the history list.
        while (game.phase() != Phase.GAME_OVER) {
            SwingUtilities.invokeAndWait(() -> step(game, ai));
            now.addAndGet(3000);
        }
        GameRecord mediumGame = game.toRecord();
        GameRecord easyGame = playEasyGame(questions, ai);

        // 3. History: start the file from the lecturer's CSV (as the app does on first run), save, read back.
        Path csv = Files.createTempDirectory("eduludo-report").resolve("history.csv");
        CsvHistoryRepository.seedFromCourseTemplate(csv);
        CsvHistoryRepository repo = new CsvHistoryRepository(csv);
        repo.save(easyGame);
        repo.save(mediumGame);
        List<GameRecord> loaded = repo.findAll();
        System.out.println("reloaded " + loaded.size() + " records from the history file:");
        Files.readAllLines(csv).forEach(line -> System.out.println("  " + line));
        Files.delete(csv);
        Files.delete(csv.getParent());

        SwingUtilities.invokeAndWait(() -> {
            HistoryPanel history = new HistoryPanel(() -> { });
            history.setRecords(loaded);
            saveDisplayable(history, "history", 1366, 768);

            AdminPanel admin = new AdminPanel();
            admin.setQuestions(bank);
            saveDisplayable(admin, "admin", 1366, 768);
        });

        // 4. Real dialogs, captured as soon as they open.
        Question architecture = bank.stream().filter(q -> q.id() == 40).findFirst().orElse(bank.get(0));
        pendingDialog = "question-dialog";
        SwingUtilities.invokeAndWait(() -> QuestionDialog.ask(null, architecture, "Dana", Theme.color(PlayerColor.RED)));
        Thread.sleep(300);
        pendingDialog = "game-over";
        SwingUtilities.invokeAndWait(() -> Dialogs.gameOver(new JPanel(), mediumGame));
        Thread.sleep(500);
        System.exit(0);
    }

    // ------------------------------------------------------------------ gameplay

    private static void step(Game g, ComputerPlayer ai) {
        switch (g.phase()) {
            case WAITING_FOR_ROLL -> g.roll();
            case CHOOSING_MOVE -> g.choose(ai.chooseMove(g));
            case ANSWERING_QUESTION -> g.answer(ai.chooseAnswer(g.pendingQuestion()));
            case CHOOSING_OPPONENT -> g.chooseOpponentPiece(ai.chooseOpponentPiece(g));
            case GAME_OVER -> { }
        }
    }

    /** A moment worth showing: Dana to roll, a question and a surprise already in the log, many pieces in play. */
    private static boolean interestingMoment(Game g, boolean surprise, boolean question, int actions) {
        long piecesInPlay = g.players().stream()
                .flatMap(p -> p.pieces().stream())
                .filter(p -> p.isActive() && !p.isFinished())
                .count();
        return surprise && question && actions > 60
                && g.phase() == Phase.WAITING_FOR_ROLL
                && g.currentPlayer().name().equals("Dana")
                && piecesInPlay >= 6;
    }

    private static GameRecord playEasyGame(QuestionService questions, ComputerPlayer ai) {
        AtomicLong now = new AtomicLong(millis(LocalDateTime.of(2026, 11, 7, 20, 5, 0)));
        Game g = Game.builder()
                .board(BoardFactory.create(Difficulty.EASY, new Random(2)))
                .difficulty(Difficulty.EASY)
                .questions(questions)
                .random(new Random(4))
                .clock(now::get)
                .addPlayer("Eli", PlayerColor.GREEN, false)
                .addPlayer("Dana", PlayerColor.RED, false)
                .build();
        g.start();
        while (g.phase() != Phase.GAME_OVER) {
            step(g, ai);
            now.addAndGet(3000);
        }
        return g.toRecord();
    }

    private static QuestionService questionService(List<Question> bank) throws Exception {
        QuestionRepository inMemory = new QuestionRepository() {
            public List<Question> loadAll() { return bank; }
            public void saveAll(List<Question> questions) { }
        };
        Path credentials = Files.createTempFile("eduludo-admin", ".properties");
        Files.delete(credentials);
        return new QuestionService(inMemory, new AuthService(credentials), new Random(3));
    }

    private static long millis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ------------------------------------------------------------------ capturing

    private static void captureMidGame(GamePanel panel) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.setSize(1366, 768);
            layout(panel);
            panel.refresh();
        });
        // Let the board's piece animation finish so pieces are drawn at their real positions.
        boolean[] animating = {true};
        for (int i = 0; i < 100 && animating[0]; i++) {
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> animating[0] = panel.isAnimating());
        }
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(() -> {
            layout(panel);
            scrollLogsToEnd(panel);
            save(panel, "midgame-medium", 1366, 768);
        });
    }

    private static void scrollLogsToEnd(Container c) {
        for (Component child : allOf(c)) {
            if (child instanceof JScrollPane scroll) {
                JViewport viewport = scroll.getViewport();
                int bottom = viewport.getView().getPreferredSize().height - viewport.getHeight();
                viewport.setViewPosition(new Point(0, Math.max(0, bottom)));
            }
        }
    }

    private static void captureDialogsWhenTheyOpen() {
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEventListener) e -> {
            if (e.getID() == WindowEvent.WINDOW_OPENED && e.getSource() instanceof JDialog dialog && pendingDialog != null) {
                String name = pendingDialog;
                pendingDialog = null;
                SwingUtilities.invokeLater(() -> {
                    JRootPane root = dialog.getRootPane();
                    save(root, name, root.getWidth(), root.getHeight());
                    dialog.dispose();
                });
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    /**
     * Panels with tables must be displayable (inside a frame) so Swing attaches the table header to its scroll
     * pane, exactly as on screen. The frame is never shown.
     */
    private static void saveDisplayable(JPanel panel, String name, int w, int h) {
        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.addNotify();
        try {
            save(panel, name, w, h);
        } finally {
            frame.dispose();
        }
    }

    private static void save(Component c, String name, int w, int h) {
        try {
            if (c.getWidth() != w || c.getHeight() != h) {
                c.setSize(w, h);
                if (c instanceof Container container) {
                    layout(container);
                }
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            c.printAll(g);
            g.dispose();
            ImageIO.write(image, "png", new File(out, name + ".png"));
            System.out.println("saved " + name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container container) {
                layout(container);
            }
        }
    }

    private static List<Component> allOf(Container c) {
        List<Component> all = new ArrayList<>();
        for (Component child : c.getComponents()) {
            all.add(child);
            if (child instanceof Container container) {
                all.addAll(allOf(container));
            }
        }
        return all;
    }
}
