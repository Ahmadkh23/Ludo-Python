package com.eduludo.View;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the window icon (four coloured quarters with a die dot), so no image file is needed. */
public final class AppIcon {

    private AppIcon() {
    }

    public static BufferedImage create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.PRIMARY_DARK);
        g.fillRoundRect(1, 1, size - 2, size - 2, size / 3, size / 3);
        int inset = size / 6;
        g.setColor(new Color(0xF5E9CE));
        g.fillRoundRect(inset, inset, size - inset * 2, size - inset * 2, size / 5, size / 5);
        javax.swing.Icon dice = Theme.assetIcon("dice", Math.max(12, size / 2), Theme.PRIMARY);
        dice.paintIcon(null, g, (size - dice.getIconWidth()) / 2, (size - dice.getIconHeight()) / 2);
        g.dispose();
        return img;
    }
}

