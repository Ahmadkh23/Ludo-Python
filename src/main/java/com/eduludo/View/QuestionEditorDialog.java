package com.eduludo.View;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/** Add / edit one question: level, topic, text, four answers and which one is correct. */
public final class QuestionEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private transient Question result;

    private QuestionEditorDialog(Window owner, Question existing) {
        super(owner, existing == null ? "Add question" : "Edit question #" + existing.id(), ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JComboBox<QuestionLevel> level = new JComboBox<>(QuestionLevel.values());
        JTextField topic = new JTextField(20);
        JTextArea text = new JTextArea(3, 40);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.BODY);
        List<JTextField> answers = new ArrayList<>();
        JRadioButton[] correct = new JRadioButton[Question.OPTION_COUNT];
        ButtonGroup group = new ButtonGroup();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        addRow(form, c, "Level", level);
        addRow(form, c, "Topic", topic);
        JScrollPane textScroll = new JScrollPane(text);
        addRow(form, c, "Question", textScroll);
        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            JTextField f = new JTextField(36);
            answers.add(f);
            correct[i] = new JRadioButton("correct");
            correct[i].setOpaque(false);
            group.add(correct[i]);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.add(f, BorderLayout.CENTER);
            row.add(correct[i], BorderLayout.EAST);
            addRow(form, c, "Answer " + (char) ('A' + i), row);
        }
        correct[0].setSelected(true);

        if (existing != null) {
            level.setSelectedItem(existing.level());
            topic.setText(existing.topic());
            text.setText(existing.text());
            for (int i = 0; i < Question.OPTION_COUNT; i++) {
                answers.get(i).setText(existing.options().get(i));
            }
            correct[existing.correctIndex()].setSelected(true);
        }
        root.add(form, BorderLayout.CENTER);

        JLabel error = Theme.label(" ", Theme.BODY_BOLD, Theme.DANGER);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(error, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        RoundButton cancel = RoundButton.secondary("Cancel");
        RoundButton save = RoundButton.primary("Save");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            int idx = 0;
            for (int i = 0; i < correct.length; i++) {
                if (correct[i].isSelected()) {
                    idx = i;
                }
            }
            List<String> opts = answers.stream().map(JTextField::getText).toList();
            try {
                result = new Question(existing == null ? 0 : existing.id(), (QuestionLevel) level.getSelectedItem(),
                        topic.getText(), text.getText(), opts, idx);
                dispose();
            } catch (IllegalArgumentException | NullPointerException ex) {
                error.setText(ex.getMessage());
            }
        });
        buttons.add(cancel);
        buttons.add(save);
        south.add(buttons, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    private static void addRow(JPanel form, GridBagConstraints c, String label, Component field) {
        c.gridx = 0;
        c.weightx = 0;
        form.add(Theme.label(label, Theme.BODY_BOLD, Theme.TEXT), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
        c.gridy++;
    }

    /** Returns the edited question, or null if cancelled. The id is 0 for a new question. */
    public static Question edit(Component parent, Question existing) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        QuestionEditorDialog d = new QuestionEditorDialog(owner, existing);
        d.setVisible(true);
        return d.result;
    }
}

