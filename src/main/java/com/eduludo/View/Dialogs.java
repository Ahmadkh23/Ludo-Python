package com.eduludo.View;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.GameRecord;
import com.eduludo.Model.Piece;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

/** Small dialogs used during the game: opponent choice, game over, rules, errors. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "EduLudo", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Surprise "sabotage": the player picks which opponent piece moves back. Returns the index in the list. */
    public static int chooseOpponentPiece(Component parent, List<Piece> candidates, int finishIndex) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "Surprise - choose an opponent piece", JDialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        int[] chosen = {0};
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        root.add(Theme.label("<html>Choose an opponent's piece.<br>A normal die is rolled and that piece moves back "
                + "half of the result.</html>", Theme.BODY, Theme.TEXT), BorderLayout.NORTH);
        JPanel list = new JPanel(new GridLayout(0, 1, 0, 6));
        list.setOpaque(false);
        for (int i = 0; i < candidates.size(); i++) {
            Piece p = candidates.get(i);
            int idx = i;
            RoundButton b = new RoundButton(p.owner().name() + " - piece " + p.number() + "  (square " + (p.position() + 1)
                    + " of " + (finishIndex + 1) + ")", Theme.color(p.owner().color()));
            if (p.owner().color() == com.eduludo.Model.PlayerColor.YELLOW) {
                b.setTextColor(Theme.TEXT);
            }
            b.addActionListener(e -> {
                chosen[0] = idx;
                d.dispose();
            });
            list.add(b);
        }
        root.add(new JScrollPane(list) {
            private static final long serialVersionUID = 1L;
            {
                setBorder(null);
                getViewport().setOpaque(false);
                setOpaque(false);
            }
        }, BorderLayout.CENTER);
        d.setContentPane(root);
        d.setSize(460, Math.min(520, 150 + candidates.size() * 48));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return chosen[0];
    }

    /** Final ranking. Returns true if the players want a rematch with the same settings. */
    public static boolean gameOver(Component parent, GameRecord record) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "Game over", JDialog.ModalityType.APPLICATION_MODAL);
        boolean[] again = {false};
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(20, 26, 18, 26));
        JLabel title = Theme.centered(record.winnerName() + " wins!", Theme.HEADING, Theme.TEXT);
        title.setIcon(Theme.assetIcon("crown"));
        root.add(title, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 6));
        rows.setOpaque(false);
        String[] medals = {"1st", "2nd", "3rd", "4th"};
        for (GameRecord.RankedPlayer p : record.ranking()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Theme.light(p.color()));
            row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            row.add(Theme.label(medals[p.place() - 1], Theme.BODY_BOLD, Theme.dark(p.color())), BorderLayout.WEST);
            row.add(Theme.label(p.name() + (p.computer() ? "  (computer)" : ""), Theme.BODY_BOLD, Theme.TEXT), BorderLayout.CENTER);
            row.add(Theme.label(p.color().displayName(), Theme.SMALL, Theme.dark(p.color())), BorderLayout.EAST);
            rows.add(row);
        }
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(rows, BorderLayout.CENTER);
        center.add(Theme.centered("Level: " + record.difficulty().displayName() + "     Time: " + record.formattedDuration()
                + "     Saved to the game history", Theme.SMALL, Theme.MUTED), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        RoundButton menu = RoundButton.secondary("Main menu");
        RoundButton rematch = RoundButton.primary("Play again");
        menu.addActionListener(e -> d.dispose());
        rematch.addActionListener(e -> {
            again[0] = true;
            d.dispose();
        });
        buttons.add(menu);
        buttons.add(rematch);
        root.add(buttons, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setSize(460, 250 + record.ranking().size() * 46);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return again[0];
    }

    /** The game rules and square legend. */
    public static void rules(Component parent) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, "How to play", JDialog.ModalityType.APPLICATION_MODAL);
        JEditorPane html = new JEditorPane("text/html", rulesHtml());
        html.setEditable(false);
        html.setCaretPosition(0);
        html.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JScrollPane scroll = new JScrollPane(html);
        scroll.setBorder(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(scroll, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        RoundButton close = RoundButton.primary("Close");
        close.addActionListener(e -> d.dispose());
        south.add(close);
        root.add(south, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setSize(new Dimension(720, 640));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    static String rulesHtml() {
        return "<html><body style='font-family:sans-serif;font-size:11px'>"
                + "<h2>EduLudo - learn while you play</h2>"
                + "<p>2 to 4 players, 4 pieces each. Bring all four pieces from your castle around the board and into the "
                + "finish in the centre. The game ends when all players except one have finished; the finishing order is saved.</p>"
                + "<h3>Turn</h3><ul>"
                + "<li>Roll the dice, then click one of your highlighted pieces.</li>"
                + "<li>A piece leaves the castle only with a <b>6</b> on the normal die (it goes to your <b>#</b> start square).</li>"
                + "<li>A <b>6</b> gives you another roll. The <b>third 6 in a row</b> is cancelled and the turn passes.</li>"
                + "<li>The finish needs an <b>exact</b> roll. If no piece can move, the turn passes.</li></ul>"
                + "<h3>Capture and blocks</h3><ul>"
                + "<li>Landing on a single opponent piece on an unprotected square sends it back to its castle.</li>"
                + "<li>Two pieces of the same colour on one square form a <b>Double Block</b>: opponents can't land on it "
                + "or pass it. Move one of the pieces to break the block.</li></ul>"
                + "<h3>Squares</h3><ul>"
                + "<li><b>Star</b> - safe square, pieces here can't be captured.</li>"
                + "<li><b>#</b> - start square of a colour (protected).</li>"
                + "<li><b>Arrow</b> - in your colour, your piece turns onto the next inner track (or into your home path).</li>"
                + "<li><b>Home path</b> - your coloured squares to the finish (protected).</li>"
                + "<li><b style='color:" + hex(Theme.color(com.eduludo.Model.QuestionLevel.EASY)) + "'>?</b> easy, "
                + "<b style='color:" + hex(Theme.color(com.eduludo.Model.QuestionLevel.MEDIUM)) + "'>?</b> medium, "
                + "<b style='color:" + hex(Theme.color(com.eduludo.Model.QuestionLevel.HARD)) + "'>?</b> hard question: "
                + "right answer +1 / +2 / +3 squares, wrong answer -1 / -2 / -3. Not protected.</li>"
                + "<li><b style='color:" + hex(Theme.SURPRISE) + "'>!</b> - surprise (protected): 10% rocket to the finish, "
                + "10% push an opponent piece back half a die roll, 10% back to the castle, 30% back 3 squares, "
                + "30% lose your next turn, 10% bonus roll with the UltraBoost die.</li></ul>"
                + "<h3>Levels</h3><ul>"
                + "<li><b>" + Difficulty.EASY.displayName() + "</b> - 52-square track. 4 safe squares, 4 arrows. Normal die.</li>"
                + "<li><b>" + Difficulty.MEDIUM.displayName() + "</b> - plus a 64-square outer ring. 8 safe, 8 arrows, "
                + "6 question and 2 surprise squares. Normal die + <b>Boost</b> die (1-3): steps = normal x (Boost - 1).</li>"
                + "<li><b>" + Difficulty.HARD.displayName() + "</b> - plus a 72-square ring. 12 safe, 12 arrows, 9 question "
                + "and 3 surprise squares. Normal die + <b>UltraBoost</b> die (1-6): steps = normal x (UltraBoost - 1).</li>"
                + "<li>With a 6 you may bring a piece out (the multiplier is ignored) <i>or</i> move a piece on the board "
                + "by the multiplied value.</li>"
                + "<li>Safe, question and surprise squares are placed at random at the start of every game.</li></ul>"
                + "</body></html>";
    }
}

