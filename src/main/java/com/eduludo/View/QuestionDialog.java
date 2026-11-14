package com.eduludo.View;

import com.eduludo.Model.Question;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Shows a multiple-choice question. The player must answer (the window can't be closed without an answer).
 * After the answer, the correct choice turns green (a wrong choice red) until the player presses Continue.
 */
public final class QuestionDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] LETTERS = {"A", "B", "C", "D"};

    private int chosen = -1;

    private QuestionDialog(Window owner, Question q, String playerName, Color playerColor) {
        super(owner, "Question for " + playerName, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        JLabel level = Theme.label("  " + q.level().displayName() + " question  ", Theme.BODY_BOLD, Color.WHITE);
        level.setOpaque(true);
        level.setBackground(Theme.color(q.level()));
        header.add(level);
        if (!q.topic().isEmpty()) {
            header.add(Theme.label(q.topic(), Theme.BODY, Theme.MUTED));
        }
        header.add(Box.createHorizontalStrut(10));
        JLabel stakes = Theme.label("Right: +" + q.level().steps() + "   Wrong: -" + q.level().steps(), Theme.SMALL, Theme.MUTED);
        header.add(stakes);
        JLabel who = Theme.label(playerName, Theme.BODY_BOLD, playerColor);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.WEST);
        top.add(who, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        JTextArea text = new JTextArea(q.text());
        text.setFont(Theme.SUBHEADING);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setForeground(Theme.TEXT);

        JPanel answers = new JPanel(new GridLayout(Question.OPTION_COUNT, 1, 0, 8));
        answers.setOpaque(false);
        RoundButton[] buttons = new RoundButton[Question.OPTION_COUNT];
        JLabel feedback = Theme.label(" ", Theme.SUBHEADING, Theme.TEXT);
        RoundButton cont = RoundButton.primary("Continue");
        cont.setVisible(false);
        cont.addActionListener(e -> dispose());

        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            int idx = i;
            RoundButton b = new RoundButton(LETTERS[i] + ".  " + q.options().get(i), new Color(0xF1F3F5), Theme.TEXT);
            b.setFont(Theme.BODY);
            b.setHorizontalAlignment(JLabel.LEFT);
            buttons[i] = b;
            b.addActionListener(e -> {
                if (chosen >= 0) {
                    return;
                }
                chosen = idx;
                for (int j = 0; j < buttons.length; j++) {
                    if (j == q.correctIndex()) {
                        buttons[j].setBase(Theme.SUCCESS);
                        buttons[j].setTextColor(Color.WHITE);
                    } else if (j == idx) {
                        buttons[j].setBase(Theme.DANGER);
                        buttons[j].setTextColor(Color.WHITE);
                    }
                }
                boolean right = q.isCorrect(idx);
                feedback.setText(right ? "Correct!  The piece moves " + q.level().steps() + " forward."
                        : "Wrong.  The piece moves " + q.level().steps() + " back.");
                feedback.setForeground(right ? Theme.SUCCESS : Theme.DANGER);
                cont.setVisible(true);
                cont.requestFocusInWindow();
                getRootPane().setDefaultButton(cont);
            });
            answers.add(b);
        }

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        answers.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(text);
        center.add(Box.createVerticalStrut(14));
        center.add(answers);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(feedback, BorderLayout.CENTER);
        bottom.add(cont, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        // Answer buttons draw their text on one line, so widen the dialog to fit the longest answer
        // (but never wider than the screen).
        int widest = 0;
        for (RoundButton b : buttons) {
            widest = Math.max(widest, b.getPreferredSize().width);
        }
        int screenWidth = getGraphicsConfiguration() != null ? getGraphicsConfiguration().getBounds().width : 1280;
        setSize(new Dimension(Math.min(Math.max(640, widest + 60), screenWidth - 80), 470));
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(owner);
    }

    /** Shows the question and returns the chosen answer index (0-3). */
    public static int ask(Component parent, Question question, String playerName, Color playerColor) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        QuestionDialog d = new QuestionDialog(owner, question, playerName, playerColor);
        d.setVisible(true);
        return d.chosen;
    }
}

