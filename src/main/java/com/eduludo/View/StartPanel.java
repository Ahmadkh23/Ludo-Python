package com.eduludo.View;

import com.eduludo.Model.PlayerColor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

/** Opening screen: new game, game history, question manager (admin only), rules, exit. */
public final class StartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JPanel card = new JPanel(new BorderLayout());

    /** Implemented by the AppController. */
    public interface Actions {
        void newGame();

        void history();

        void questionManager();

        void rules();

        void exit();
    }

    private final JLabel bankInfo = Theme.centered(" ", Theme.SMALL, Theme.MUTED);

    public StartPanel(Actions actions) {
        super(new GridBagLayout());
        setOpaque(false);

        card.setBackground(Theme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 249, 235, 190), 2),
                BorderFactory.createLineBorder(new Color(26, 47, 36, 100), 1)));
        card.setPreferredSize(new Dimension(1050, 620));

        BrandPanel brand = new BrandPanel();
        brand.setPreferredSize(new Dimension(420, 620));
        buildBrand(brand);
        card.add(brand, BorderLayout.WEST);
        card.add(buildMenu(actions), BorderLayout.CENTER);
        add(card);
    }

    @Override
    public void doLayout() {
        if (getWidth() > 0 && getHeight() > 0) {
            card.setPreferredSize(new Dimension(Math.max(1, Math.min(1050, getWidth() - 48)),
                    Math.max(1, Math.min(620, getHeight() - 24))));
        }
        super.doLayout();
    }

    private static void buildBrand(JPanel brand) {
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(48, 44, 34, 36));

        JPanel mark = new JPanel(new BorderLayout());
        mark.setOpaque(false);
        JLabel dice = new JLabel(Theme.assetIcon("dice"));
        dice.setHorizontalAlignment(JLabel.CENTER);
        dice.setOpaque(true);
        dice.setBackground(new Color(0xC77A4D));
        dice.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        mark.add(dice, BorderLayout.WEST);
        JLabel eyebrow = Theme.label("A GAME FOR CURIOUS MINDS", Theme.SMALL, new Color(0xE6C99C));
        JPanel eyebrowBox = new JPanel(new BorderLayout());
        eyebrowBox.setOpaque(false);
        eyebrowBox.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        eyebrowBox.add(eyebrow, BorderLayout.CENTER);
        mark.add(eyebrowBox, BorderLayout.CENTER);
        mark.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        brand.add(mark);

        brand.add(Box.createVerticalStrut(54));
        JLabel title = Theme.label("EduLudo", new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 52),
                new Color(0xFFF8E9));
        brand.add(title);
        brand.add(Box.createVerticalStrut(15));
        java.awt.Font statementFont = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 24);
        brand.add(Theme.label("Roll the dice.", statementFont, new Color(0xF1E8D6)));
        brand.add(Theme.label("Learn something.", statementFont, new Color(0xF1E8D6)));
        brand.add(Theme.label("Race home.", statementFont, new Color(0xF1E8D6)));
        brand.add(Box.createVerticalStrut(22));
        JLabel description = Theme.label("<html>Classic Ludo with a little<br>software engineering trivia.</html>",
                Theme.BODY, new Color(0xD2DFD1));
        brand.add(description);
        brand.add(Box.createVerticalGlue());

        JPanel colors = new JPanel();
        colors.setOpaque(false);
        colors.setLayout(new BoxLayout(colors, BoxLayout.X_AXIS));
        for (PlayerColor playerColor : PlayerColor.values()) {
            JPanel swatch = new JPanel();
            swatch.setBackground(Theme.color(playerColor));
            swatch.setPreferredSize(new Dimension(15, 15));
            swatch.setMaximumSize(new Dimension(15, 15));
            swatch.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 145), 2));
            colors.add(swatch);
            colors.add(Box.createHorizontalStrut(9));
        }
        JLabel playerNote = Theme.label("UP TO FOUR PLAYERS", Theme.SMALL, new Color(0xE6C99C));
        colors.add(Box.createHorizontalStrut(7));
        colors.add(playerNote);
        brand.add(colors);
        brand.add(Box.createVerticalStrut(18));
        brand.add(Theme.label("OFFLINE  /  PASS & PLAY", Theme.SMALL, new Color(0xD2DFD1)));
    }

    private JPanel buildMenu(Actions actions) {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Theme.PANEL);
        menu.setBorder(BorderFactory.createEmptyBorder(50, 48, 32, 48));

        JLabel heading = Theme.label("Set the board.", Theme.HEADING, Theme.TEXT);
        menu.add(heading);
        menu.add(Box.createVerticalStrut(7));
        menu.add(Theme.label("Choose where you want to begin.", Theme.BODY, Theme.MUTED));
        menu.add(Box.createVerticalStrut(30));

        RoundButton play = RoundButton.primary("Start a new game");
        RoundButton history = RoundButton.secondary("Game history");
        RoundButton admin = RoundButton.secondary("Question manager (admin)");
        RoundButton rules = RoundButton.secondary("How to play");
        RoundButton exit = RoundButton.secondary("Exit");
        play.setFont(Theme.SUBHEADING);
        play.setIcon(Theme.assetIcon("pawn"));
        history.setIcon(Theme.assetIcon("history"));
        admin.setIcon(Theme.assetIcon("admin"));
        rules.setIcon(Theme.assetIcon("rules"));
        exit.setIcon(Theme.assetIcon("exit"));
        play.addActionListener(e -> actions.newGame());
        history.addActionListener(e -> actions.history());
        admin.addActionListener(e -> actions.questionManager());
        rules.addActionListener(e -> actions.rules());
        exit.addActionListener(e -> actions.exit());
        for (RoundButton button : new RoundButton[] {play, history, admin, rules, exit}) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            button.setPreferredSize(new Dimension(390, 48));
            menu.add(button);
            menu.add(Box.createVerticalStrut(11));
        }

        menu.add(Box.createVerticalGlue());
        bankInfo.setHorizontalAlignment(JLabel.LEFT);
        bankInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(bankInfo);
        menu.add(Box.createVerticalStrut(14));
        menu.add(Theme.label("EDUCATIONAL LUDO  •  DESKTOP EDITION", Theme.SMALL, Theme.MUTED));
        return menu;
    }

    public void setBankInfo(String text) {
        bankInfo.setText(text);
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme.paintTabletop(g, this);
        g.setColor(new Color(255, 239, 211, 24));
        for (int i = 0; i < 7; i++) {
            int diameter = 90 + i * 40;
            g.drawOval(getWidth() - diameter / 2, getHeight() - diameter / 2, diameter, diameter);
        }
        g.dispose();
    }

    private static final class BrandPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0x245B49), getWidth(), getHeight(),
                    new Color(0x153D32)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(238, 221, 185, 20));
            for (int i = 0; i < 6; i++) {
                g.drawRoundRect(getWidth() - 190 + i * 4, getHeight() - 205 + i * 4,
                        215 - i * 8, 215 - i * 8, 26, 26);
            }
            g.setColor(new Color(255, 255, 255, 15));
            g.fillOval(getWidth() - 106, getHeight() - 118, 66, 66);
            g.dispose();
        }
    }
}

