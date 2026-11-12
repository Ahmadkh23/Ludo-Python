package com.eduludo.View;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The application window. Screens are switched with a CardLayout. */
public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String START = "start";
    public static final String SETUP = "setup";
    public static final String GAME = "game";
    public static final String HISTORY = "history";
    public static final String ADMIN = "admin";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public MainFrame(Runnable onCloseRequest) {
        super("EduLudo - Educational Ludo");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseRequest.run();
            }
        });
        root.setBackground(Theme.BACKGROUND);
        setContentPane(root);
        fitToScreen();
        setIconImage(AppIcon.create(64));
    }

    /**
     * Uses 1280x860 when it fits, otherwise the usable screen area (laptops with display scaling are often smaller),
     * so the window never opens larger than the screen.
     */
    private void fitToScreen() {
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.min(1280, usable.width);
        int height = Math.min(860, usable.height);
        setMinimumSize(new Dimension(Math.min(900, width), Math.min(640, height)));
        setSize(width, height);
        setLocationRelativeTo(null);
        if (width < 1280 || height < 860) {
            setExtendedState(MAXIMIZED_BOTH);
        }
    }

    public void addScreen(String name, JPanel panel) {
        root.add(panel, name);
    }

    public void show(String name) {
        cards.show(root, name);
    }
}

