package com.eduludo.View;

import com.eduludo.Model.PlayerColor;
import com.eduludo.Model.QuestionLevel;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Colours and fonts used by every screen, so the whole UI looks consistent. */
public final class Theme {

    public static final Color BACKGROUND = new Color(0xF2EBDD);
    public static final Color PANEL = new Color(0xFFFDF8);
    public static final Color TEXT = new Color(0x26352E);
    public static final Color MUTED = new Color(0x71786F);
    public static final Color PRIMARY = new Color(0x245B49);
    public static final Color PRIMARY_DARK = new Color(0x174535);
    public static final Color DANGER = new Color(0xB74738);
    public static final Color SUCCESS = new Color(0x347857);
    public static final Color BORDER = new Color(0xD9CCB4);
    public static final Color BOARD_LINE = new Color(0xC4B79D);
    public static final Color TRACK = new Color(0xFBF6EA);
    public static final Color RING = new Color(0xEAE0CD);
    public static final Color SAFE = new Color(0xE5EDE2);
    public static final Color SURPRISE = new Color(0xC86A48);
    public static final Color FELT = new Color(0x245B49);
    private static final BufferedImage TABLETOP = loadTabletop();
    private static final Map<String, BufferedImage> ICONS = new HashMap<>();

    public static final Font TITLE = new Font(Font.SERIF, Font.BOLD, 40);
    public static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font SUBHEADING = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 20);

    private Theme() {
    }

    private static BufferedImage loadTabletop() {
        try (InputStream input = Theme.class.getResourceAsStream("/com/eduludo/assets/tabletop-wood.jpg")) {
            if (input == null) {
                throw new IllegalStateException("Missing visual asset: tabletop-wood.jpg");
            }
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load tabletop background texture", e);
        }
    }

    public static BufferedImage tabletop() {
        return TABLETOP;
    }

    public static void paintTabletop(Graphics2D g, Component component) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(TABLETOP, 0, 0, component.getWidth(), component.getHeight(), null);
        g.setColor(new Color(25, 39, 29, 132));
        g.fillRect(0, 0, component.getWidth(), component.getHeight());
    }

    public static Color color(PlayerColor c) {
        return switch (c) {
            case RED -> new Color(0xC95245);
            case GREEN -> new Color(0x43845D);
            case YELLOW -> new Color(0xD5A83F);
            case BLUE -> new Color(0x4D78A5);
        };
    }

    public static Color light(PlayerColor c) {
        return mix(color(c), Color.WHITE, 0.62);
    }

    public static Color dark(PlayerColor c) {
        return mix(color(c), Color.BLACK, 0.30);
    }

    public static Color color(QuestionLevel level) {
        return switch (level) {
            case EASY -> new Color(0x538B66);
            case MEDIUM -> new Color(0xD09C42);
            case HARD -> new Color(0xBA5C50);
        };
    }

    /** Mixes two colours: amount 0 = a, 1 = b. */
    public static Color mix(Color a, Color b, double amount) {
        double k = 1 - amount;
        return new Color(
                (int) Math.round(a.getRed() * k + b.getRed() * amount),
                (int) Math.round(a.getGreen() * k + b.getGreen() * amount),
                (int) Math.round(a.getBlue() * k + b.getBlue() * amount));
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel centered(String text, Font font, Color color) {
        JLabel l = label(text, font, color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    public static ImageIcon assetIcon(String name) {
        return assetIcon(name, 24);
    }

    public static ImageIcon assetIcon(String name, int size) {
        BufferedImage source = loadIcon(name);
        Image image = source.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    public static ImageIcon assetIcon(String name, int size, Color tint) {
        BufferedImage source = loadIcon(name);
        BufferedImage colored = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getRGB(x, y) >>> 24;
                colored.setRGB(x, y, (alpha << 24) | (tint.getRGB() & 0x00FFFFFF));
            }
        }
        return new ImageIcon(colored.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    private static BufferedImage loadIcon(String name) {
        BufferedImage existing = ICONS.get(name);
        if (existing != null) {
            return existing;
        }
        URL resource = Theme.class.getResource("/com/eduludo/assets/icons/" + name + ".png");
        if (resource == null) {
            throw new IllegalStateException("Missing visual asset: " + name);
        }
        try {
            BufferedImage image = ImageIO.read(resource);
            ICONS.put(name, image);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load visual asset: " + name, e);
        }
    }

    public static Border card() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    public static <T extends JComponent> T padded(T c, int top, int left, int bottom, int right) {
        c.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return c;
    }
}

