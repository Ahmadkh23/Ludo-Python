package com.eduludo.Model;

import com.eduludo.Controller.GameController;
import com.eduludo.Model.ai.ComputerPlayer;
import com.eduludo.View.GamePanel;
import com.eduludo.View.SetupPanel;
import com.eduludo.View.StartPanel;
import com.eduludo.persistence.HistoryRepository;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Iteration 1 interaction checks (run by verify-ui.sh, headless). The first three drive the game model with
 * scripted dice; the others drive the real Swing panels and the GameController. Screens are saved to
 * test-results/ui.
 */
public class Iteration1Check {

    private static int passed;
    private static int failed;

    // ------------------------------------------------------------------ helpers

    static void check(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable e) {
            failed++;
            System.out.println("FAIL " + name + ": " + e);
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static Game make(ScriptedDice die) {
        return Game.builder()
                .board(BoardFactory.createLayout(Difficulty.EASY))
                .normalDie(die)
                .addPlayer("Alice", PlayerColor.RED, false)
                .addPlayer("Bob", PlayerColor.GREEN, false)
                .build();
    }

    static <T> List<T> find(Component c, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(c)) {
            found.add(type.cast(c));
        }
        if (c instanceof Container p) {
            for (Component child : p.getComponents()) {
                found.addAll(find(child, type));
            }
        }
        return found;
    }

    static AbstractButton button(Component c, String text) {
        return find(c, AbstractButton.class).stream()
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElseThrow();
    }

    static long labelsShowing(Component c, String text) {
        return find(c, JLabel.class).stream().filter(l -> text.equals(l.getText())).count();
    }

    static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container p) {
                layout(p);
            }
        }
    }

    static void render(JPanel p, String name, int w, int h) {
        try {
            p.setSize(w, h);
            layout(p);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            p.printAll(g);
            g.dispose();
            ImageIO.write(image, "png", new File("test-results/ui/" + name + ".png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final GamePanel.Actions NO_ACTIONS = new GamePanel.Actions() {
        public void roll() { }
        public void pieceClicked(Piece q) { }
        public void quit() { }
        public void showRules() { }
    };

    // ------------------------------------------------------------------ checks

    public static void main(String[] args) throws Exception {
        new File("test-results/ui").mkdirs();

        check("T1.6 partial move before block when no full move exists", () -> {
            Game g = make(new ScriptedDice(6, "Normal").then(4));
            g.players().get(0).piece(1).setPosition(5);
            int n = g.board().path(PlayerColor.GREEN).indexOf(g.board().path(PlayerColor.RED).get(8));
            g.players().get(1).piece(1).setPosition(n);
            g.players().get(1).piece(2).setPosition(n);
            g.roll();
            require(g.options().stream().anyMatch(o -> o.target() == 7),
                    "Expected target 7; actual options=" + g.options() + ", phase=" + g.phase());
        });

        check("T1.5/T1.7 reject double roll and invalid selection", () -> {
            Game g = make(new ScriptedDice(6, "Normal").then(6));
            g.roll();
            try {
                g.roll();
                throw new AssertionError("Second roll accepted");
            } catch (IllegalStateException expected) {
                // a second roll in the same phase must be rejected
            }
            require(!g.chooseMoveFor(g.players().get(1).piece(1)), "Opponent selectable");
        });

        check("T1.6/T1.7 enter, move, advance turn", () -> {
            Game g = make(new ScriptedDice(6, "Normal").then(6, 3));
            Piece p = g.players().get(0).piece(1);
            g.roll();
            require(g.chooseMoveFor(p), "Entry refused");
            require(p.position() == 0 && g.currentPlayer() == p.owner(), "Entry/extra turn incorrect");
            g.roll();
            require(g.chooseMoveFor(p), "Move refused");
            require(p.position() == 3 && g.currentPlayer() == g.players().get(1), "Move/turn incorrect");
        });

        SwingUtilities.invokeAndWait(() -> {
            check("T1.1/T1.8 menu action callback and render", () -> {
                boolean[] clicked = {false};
                StartPanel p = new StartPanel(new StartPanel.Actions() {
                    public void newGame() { clicked[0] = true; }
                    public void history() { }
                    public void questionManager() { }
                    public void rules() { }
                    public void exit() { }
                });
                button(p, "Start a new game").doClick();
                require(clicked[0], "Missing callback");
                render(p, "menu", 1366, 768);
            });

            check("T1.2/T1.8 setup validation, handoff and Back", () -> {
                List<List<Game.PlayerSpec>> calls = new ArrayList<>();
                boolean[] back = {false};
                SetupPanel p = new SetupPanel(new SetupPanel.Actions() {
                    public void startGame(Difficulty d, List<Game.PlayerSpec> players) { calls.add(players); }
                    public void back() { back[0] = true; }
                });
                List<JTextField> names = find(p, JTextField.class);
                AbstractButton start = button(p, "Start game");

                start.doClick();
                require(calls.size() == 1 && calls.get(0).size() == 4, "Default handoff");

                names.get(0).setText(" ");
                start.doClick();
                require(calls.size() == 1, "Blank name accepted");

                names.get(0).setText("Player 2");
                start.doClick();
                require(calls.size() == 1, "Duplicate name accepted");

                names.get(0).setText("Alice");
                @SuppressWarnings("rawtypes")
                List<JComboBox> boxes = find(p, JComboBox.class);
                boxes.get(1).setSelectedItem(PlayerColor.GREEN);
                start.doClick();
                require(calls.size() == 1, "Duplicate colour accepted");

                boxes.get(1).setSelectedItem(PlayerColor.RED);
                boxes.get(0).setSelectedItem(2);
                start.doClick();
                require(calls.size() == 2 && calls.get(1).size() == 2, "Two-player handoff failed");

                boxes.get(0).setSelectedItem(3);
                start.doClick();
                require(calls.size() == 3 && calls.get(2).size() == 3, "Three-player handoff failed");

                button(p, "Back").doClick();
                require(back[0], "Back callback failed");
                render(p, "setup", 1366, 768);
            });

            check("T1.4/T1.10 screen shows current player and refreshes", () -> {
                Game g = make(new ScriptedDice(6, "Normal").then(2));
                GamePanel p = new GamePanel();
                p.bind(g, NO_ACTIONS);
                try {
                    require(labelsShowing(p, "Alice") >= 2, "Alice missing from turn/player displays");
                    render(p, "board1366", 1366, 768);
                    g.roll();
                    p.refresh();
                    require(labelsShowing(p, "Bob") >= 2, "Bob missing after turn change");
                    render(p, "board1920", 1920, 1080);
                } finally {
                    p.unbind();
                }
            });
        });

        Game[] game = {null};
        GamePanel[] panel = {null};
        GameController[] controller = {null};
        SwingUtilities.invokeAndWait(() -> check("T1.12 controller/view/model integration start and roll", () -> {
            game[0] = make(new ScriptedDice(6, "Normal").then(6, 3));
            panel[0] = new GamePanel();
            HistoryRepository noHistory = new HistoryRepository() {
                public void save(GameRecord r) { }
                public List<GameRecord> findAll() { return List.of(); }
            };
            GameController.Exit noExit = new GameController.Exit() {
                public void gameEnded(boolean again) { }
                public void gameQuit() { }
            };
            controller[0] = new GameController(game[0], panel[0], new ComputerPlayer(new Random(1)), noHistory, noExit);
            controller[0].start();
            button(panel[0], "Roll dice").doClick();
            require(game[0].phase() == Phase.CHOOSING_MOVE, "Roll button did not reach model");
        }));

        Thread.sleep(650);
        SwingUtilities.invokeAndWait(() -> check("T1.10/T1.12 controller selects piece and updates display", () -> {
            Piece p = game[0].players().get(0).piece(1);
            controller[0].pieceClicked(p);
            require(p.position() == 0, "Controller did not enter piece");
            require(labelsShowing(panel[0], "Alice") >= 2, "Alice (extra turn after a 6) not shown");
        }));

        // Second roll (3): the only legal move is played by the controller; the turn passes to Bob.
        Thread.sleep(650);
        SwingUtilities.invokeAndWait(() -> button(panel[0], "Roll dice").doClick());
        Thread.sleep(2500);
        SwingUtilities.invokeAndWait(() -> {
            check("T1.10/T1.12 move is applied and the display shows the next player", () -> {
                Piece p = game[0].players().get(0).piece(1);
                require(p.position() == 3, "Piece did not move 3 (position " + p.position() + ")");
                require(game[0].currentPlayer() == game[0].players().get(1), "Turn did not pass to Bob");
                require(labelsShowing(panel[0], "Bob") >= 2, "Display does not show Bob's turn");
            });
            controller[0].stop();
        });

        System.out.println("Additional checks: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
