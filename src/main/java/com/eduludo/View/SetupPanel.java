package com.eduludo.View;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.Game;
import com.eduludo.Model.PlayerColor;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Game setup: number of players (2-4), difficulty, and for each player a name, a unique colour and whether the
 * computer plays. Input is checked here for a friendly message, and again by {@link Game.Builder}.
 */
public final class SetupPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Implemented by the AppController. */
    public interface Actions {
        void startGame(Difficulty difficulty, List<Game.PlayerSpec> players);

        void back();
    }

    private final JComboBox<Integer> count = new JComboBox<>(new Integer[] {2, 3, 4});
    private final JRadioButton[] levels = new JRadioButton[Difficulty.values().length];
    private final JTextField[] names = new JTextField[4];
    private final transient List<JComboBox<PlayerColor>> colors = new ArrayList<>();
    private final JCheckBox[] computer = new JCheckBox[4];
    private final JPanel[] rows = new JPanel[4];
    private final JLabel error = Theme.label(" ", Theme.BODY_BOLD, Theme.DANGER);

    public SetupPanel(Actions actions) {
        super(new GridBagLayout());
        setOpaque(false);
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Theme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(28, 34, 28, 34)));
        JPanel heading = new JPanel(new BorderLayout(14, 0));
        heading.setOpaque(false);
        JLabel mark = new JLabel(Theme.assetIcon("pawn"));
        mark.setOpaque(true);
        mark.setBackground(Theme.PRIMARY);
        mark.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        heading.add(mark, BorderLayout.WEST);
        JPanel headingText = new JPanel(new GridLayout(0, 1, 0, 3));
        headingText.setOpaque(false);
        headingText.add(Theme.label("Set up your table", Theme.HEADING, Theme.TEXT));
        headingText.add(Theme.label("Choose a level and who's playing.", Theme.SMALL, Theme.MUTED));
        heading.add(headingText, BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(Theme.label("Players", Theme.BODY_BOLD, Theme.TEXT), gc);
        gc.gridx = 1;
        count.setSelectedItem(4);
        count.addActionListener(e -> updateRows());
        form.add(count, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.anchor = GridBagConstraints.NORTHWEST;
        form.add(Theme.label("Level", Theme.BODY_BOLD, Theme.TEXT), gc);
        gc.gridx = 1;
        JPanel levelPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        levelPanel.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        String[] descriptions = {
                "Easy - classic 52-square board, normal die",
                "Medium - +64-square ring, questions, surprises, Boost die",
                "Hard - +72-square ring, more questions and surprises, UltraBoost die"};
        for (Difficulty d : Difficulty.values()) {
            JRadioButton rb = new JRadioButton(descriptions[d.ordinal()]);
            rb.setOpaque(false);
            rb.setFont(Theme.BODY);
            rb.setForeground(Theme.TEXT);
            rb.setIconTextGap(10);
            group.add(rb);
            levels[d.ordinal()] = rb;
            levelPanel.add(rb);
        }
        levels[Difficulty.EASY.ordinal()].setSelected(true);
        form.add(levelPanel, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JPanel table = new JPanel(new GridLayout(0, 1, 0, 7));
        table.setOpaque(false);
        JPanel header = row(Theme.label("Name", Theme.SMALL, Theme.MUTED), Theme.label("Colour", Theme.SMALL, Theme.MUTED),
                Theme.label("Computer", Theme.SMALL, Theme.MUTED));
        table.add(header);
        PlayerColor[] all = PlayerColor.values();
        for (int i = 0; i < 4; i++) {
            names[i] = new JTextField("Player " + (i + 1), 14);
            names[i].setFont(Theme.BODY);
            names[i].setBackground(new Color(0xFFFDF8));
            names[i].setForeground(Theme.TEXT);
            names[i].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)));
            ((AbstractDocument) names[i].getDocument()).setDocumentFilter(new MaxLength(Game.Builder.MAX_NAME_LENGTH));
            JComboBox<PlayerColor> cb = new JComboBox<>(all);
            cb.setSelectedItem(all[i]);
            cb.setRenderer(new ColorRenderer());
            colors.add(cb);
            computer[i] = new JCheckBox();
            computer[i].setText("AI");
            computer[i].setToolTipText("Play this seat with a computer opponent");
            computer[i].setForeground(Theme.TEXT);
            computer[i].setFont(Theme.SMALL);
            computer[i].setOpaque(false);
            rows[i] = row(names[i], cb, computer[i]);
            rows[i].setBackground(i % 2 == 0 ? new Color(0xF5F0E5) : new Color(0xFBF8F1));
            rows[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.color(all[i])),
                    BorderFactory.createEmptyBorder(4, 8, 4, 4)));
            rows[i].setOpaque(true);
            table.add(rows[i]);
        }
        form.add(table, gc);
        card.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(error, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        RoundButton back = RoundButton.secondary("Back");
        RoundButton start = RoundButton.primary("Start game");
        back.setIcon(Theme.assetIcon("back"));
        start.setIcon(Theme.assetIcon("pawn"));
        back.addActionListener(e -> actions.back());
        start.addActionListener(e -> {
            String problem = validateInput();
            if (problem != null) {
                error.setText(problem);
                return;
            }
            error.setText(" ");
            actions.startGame(selectedDifficulty(), players());
        });
        buttons.add(back);
        buttons.add(start);
        south.add(buttons, BorderLayout.EAST);
        card.add(south, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(680, 540));
        add(card);
        updateRows();
    }

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        Theme.paintTabletop(g, this);
        g.dispose();
    }

    private static JPanel row(Component name, Component color, Component bot) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        name.setPreferredSize(new Dimension(260, 32));
        p.add(name, c);
        c.weightx = 0;
        color.setPreferredSize(new Dimension(150, 32));
        p.add(color, c);
        bot.setPreferredSize(new Dimension(80, 32));
        p.add(bot, c);
        return p;
    }

    private void updateRows() {
        int n = (Integer) count.getSelectedItem();
        for (int i = 0; i < 4; i++) {
            boolean on = i < n;
            names[i].setEnabled(on);
            colors.get(i).setEnabled(on);
            computer[i].setEnabled(on);
        }
    }

    private Difficulty selectedDifficulty() {
        for (Difficulty d : Difficulty.values()) {
            if (levels[d.ordinal()].isSelected()) {
                return d;
            }
        }
        return Difficulty.EASY;
    }

    private List<Game.PlayerSpec> players() {
        int n = (Integer) count.getSelectedItem();
        List<Game.PlayerSpec> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Game.PlayerSpec(names[i].getText().strip(), (PlayerColor) colors.get(i).getSelectedItem(),
                    computer[i].isSelected()));
        }
        return list;
    }

    /** Returns null when the setup is valid, otherwise a message for the user. */
    String validateInput() {
        List<Game.PlayerSpec> list = players();
        Set<PlayerColor> used = EnumSet.noneOf(PlayerColor.class);
        Set<String> usedNames = new HashSet<>();
        for (Game.PlayerSpec p : list) {
            if (p.name().isEmpty()) {
                return "Every player needs a name.";
            }
            if (!usedNames.add(p.name().toLowerCase(Locale.ROOT))) {
                return "Two players have the same name: " + p.name();
            }
            if (!used.add(p.color())) {
                return "Each player must choose a different colour.";
            }
        }
        if (list.stream().allMatch(Game.PlayerSpec::computer)) {
            return "At least one player must be a person.";
        }
        return null;
    }

    /** Restores the previous setup (used by "Play again"). */
    public void prefill(Difficulty difficulty, List<Game.PlayerSpec> players) {
        levels[difficulty.ordinal()].setSelected(true);
        count.setSelectedItem(players.size());
        for (int i = 0; i < players.size(); i++) {
            names[i].setText(players.get(i).name());
            colors.get(i).setSelectedItem(players.get(i).color());
            computer[i].setSelected(players.get(i).computer());
        }
        updateRows();
    }

    /** Shows the colour name with a coloured square. */
    private static final class ColorRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof PlayerColor pc) {
                l.setText(pc.displayName());
                l.setIcon(new javax.swing.Icon() {
                    @Override
                    public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
                        g.setColor(Theme.color(pc));
                        g.fillRoundRect(x, y, 14, 14, 4, 4);
                        g.setColor(Color.DARK_GRAY);
                        g.drawRoundRect(x, y, 14, 14, 4, 4);
                    }

                    @Override
                    public int getIconWidth() {
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        return 15;
                    }
                });
            }
            return l;
        }
    }

    /** Limits a text field to a maximum number of characters. */
    static final class MaxLength extends DocumentFilter {
        private final int max;

        MaxLength(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String t = text == null ? "" : text;
            int allowed = max - (fb.getDocument().getLength() - length);
            if (allowed <= 0) {
                return;
            }
            super.replace(fb, offset, length, t.length() > allowed ? t.substring(0, allowed) : t, attrs);
        }
    }
}

