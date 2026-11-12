package com.eduludo.View;

import com.eduludo.Model.dice.DiceRoll;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Shows the normal die and, on MEDIUM/HARD, the Boost / UltraBoost die next to it, with a short rolling animation.
 */
public final class DiceView extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int[][][] PIPS = {
            {},
            {{1, 1}},
            {{0, 0}, {2, 2}},
            {{0, 0}, {1, 1}, {2, 2}},
            {{0, 0}, {0, 2}, {2, 0}, {2, 2}},
            {{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}},
            {{0, 0}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 2}},
    };

    private final transient Random random = new Random();
    private final Timer rollTimer;
    private int normal = 1;
    private int multiplierFace;
    private int multiplierFaces;
    private String multiplierName;
    private int levelMultiplierFaces;
    private String levelMultiplierName;
    private Color accent = Theme.PRIMARY;
    private int rollingFrames;
    private transient DiceRoll target;

    public DiceView() {
        setPreferredSize(new Dimension(260, 112));
        setMinimumSize(new Dimension(220, 100));
        rollTimer = new Timer(55, e -> {
            if (rollingFrames-- > 0) {
                normal = random.nextInt(6) + 1;
                if (multiplierFaces > 0) {
                    multiplierFace = random.nextInt(multiplierFaces) + 1;
                }
            } else {
                ((Timer) e.getSource()).stop();
                if (target != null) {
                    normal = target.normal();
                    multiplierFace = target.multiplierFace();
                }
            }
            repaint();
        });
    }

    /** Which multiplier die this level uses (0 faces = none). */
    public void configure(String multiplierName, int multiplierFaces) {
        this.levelMultiplierName = multiplierName;
        this.levelMultiplierFaces = multiplierFaces;
        this.multiplierName = multiplierName;
        this.multiplierFaces = multiplierFaces;
        this.multiplierFace = 0; // "?" until the first roll
        this.normal = 0;
        this.target = null;
        repaint();
    }

    public void setAccent(Color accent) {
        this.accent = accent;
        repaint();
    }

    /** Animates and then shows the given roll. */
    public void showRoll(DiceRoll roll) {
        this.target = roll;
        if (roll.hasMultiplier()) {
            // the surprise bonus roll uses the UltraBoost die even on levels that normally use another one
            multiplierName = roll.multiplierName();
            multiplierFaces = roll.bonus() ? 6 : levelMultiplierFaces;
        } else {
            multiplierName = levelMultiplierName;
            multiplierFaces = levelMultiplierFaces;
        }
        rollingFrames = 7;
        rollTimer.restart();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int size = Math.min(getHeight() - 26, 80);
        boolean showMult = multiplierFaces > 0;
        int total = showMult ? size * 2 + 22 : size;
        int x0 = (getWidth() - total) / 2;
        drawDie(g, x0, 4, size, normal, new Color(0xFFFDF8), accent);
        label(g, "Normal", x0 + size / 2, size + 20);
        if (showMult) {
            int mx = x0 + size + 22;
            Color fill = "UltraBoost".equals(multiplierName) ? Theme.PRIMARY_DARK : new Color(0xD6B66D);
            Color pips = "UltraBoost".equals(multiplierName) ? new Color(0xF5E9C9) : Theme.PRIMARY_DARK;
            drawDie(g, mx, 4, size, multiplierFace, fill, pips);
            String name = multiplierName == null ? "Boost" : multiplierName;
            label(g, multiplierFace > 0 ? name + "  x" + (multiplierFace - 1) : name, mx + size / 2, size + 20);
        }
        g.dispose();
    }

    private void label(Graphics2D g, String s, int cx, int baseline) {
        g.setFont(Theme.SMALL);
        g.setColor(Theme.MUTED);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }

    private void drawDie(Graphics2D g, int x, int y, int size, int value, Color fill, Color pip) {
        g.setColor(new Color(20, 32, 24, 55));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 5, size, size, size * 0.23, size * 0.23));
        g.setColor(new Color(0xB99B5E));
        g.fill(new RoundRectangle2D.Double(x - 1, y - 1, size + 2, size + 2, size * 0.24, size * 0.24));
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(x + 2, y + 2, size - 4, size - 4, size * 0.20, size * 0.20));
        g.setColor(new Color(255, 255, 255, 115));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(x + 5, y + 5, size - 10, size - 10, size * 0.16, size * 0.16));
        g.setColor(pip);
        double cell = size / 3.0;
        double r = size * 0.09;
        if (value >= 1 && value <= 6) {
            for (int[] p : PIPS[value]) {
                double cx = x + cell * p[1] + cell / 2;
                double cy = y + cell * p[0] + cell / 2;
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
        } else {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
            FontMetrics fm = g.getFontMetrics();
            String s = value <= 0 ? "?" : Integer.toString(value);
            g.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
}

