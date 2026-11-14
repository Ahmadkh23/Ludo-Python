package com.eduludo.View;

import javax.swing.JButton;
import javax.swing.Icon;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A flat, rounded button that looks the same on Windows, macOS and Linux. */
public final class RoundButton extends JButton {

    private static final long serialVersionUID = 1L;

    private Color base;
    private Color textColor;

    public RoundButton(String text, Color base) {
        this(text, base, Color.WHITE);
    }

    public RoundButton(String text, Color base, Color textColor) {
        super(text);
        this.base = base;
        this.textColor = textColor;
        setFont(Theme.BODY_BOLD);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static RoundButton primary(String text) {
        return new RoundButton(text, Theme.PRIMARY);
    }

    public static RoundButton secondary(String text) {
        return new RoundButton(text, new Color(0xEAE2D3), Theme.TEXT);
    }

    public static RoundButton danger(String text) {
        return new RoundButton(text, Theme.DANGER);
    }

    public void setBase(Color base) {
        this.base = base;
        repaint();
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Icon icon = getIcon();
        int iconWidth = icon == null ? 0 : icon.getIconWidth() + 10;
        int iconHeight = icon == null ? 0 : icon.getIconHeight() + 12;
        return new Dimension(Math.max(110, fm.stringWidth(getText()) + 36 + iconWidth),
                Math.max(40, Math.max(fm.getHeight() + 18, iconHeight)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color fill = base;
        if (!isEnabled()) {
            fill = Theme.mix(base, Color.WHITE, 0.55);
        } else if (getModel().isPressed()) {
            fill = Theme.mix(base, Color.BLACK, 0.18);
        } else if (getModel().isRollover()) {
            fill = Theme.mix(base, Color.BLACK, 0.08);
        }
        g2.setColor(new Color(0, 0, 0, isEnabled() ? 22 : 10));
        g2.fillRoundRect(0, 2, getWidth() - 1, getHeight() - 2, 14, 14);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 14, 14);
        if (isFocusOwner()) {
            g2.setColor(Theme.mix(fill, Color.BLACK, 0.35));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(isEnabled() ? textColor : Theme.mix(textColor, fill, 0.45));
        Icon icon = getIcon();
        int iconWidth = icon == null ? 0 : icon.getIconWidth();
        int gap = icon == null ? 0 : 10;
        int contentWidth = iconWidth + gap + fm.stringWidth(getText());
        int x = (getWidth() - contentWidth) / 2;
        if (icon != null) {
            icon.paintIcon(this, g2, x, (getHeight() - icon.getIconHeight()) / 2 - 1);
            x += iconWidth + gap;
        }
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}

