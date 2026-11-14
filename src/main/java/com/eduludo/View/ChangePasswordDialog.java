package com.eduludo.View;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.awt.GridLayout;

/** Asks for the current admin password and the new one (twice). */
public final class ChangePasswordDialog {

    private ChangePasswordDialog() {
    }

    /** Returns {current, new, repeat}, or null if cancelled. The caller must clear the arrays after use. */
    public static char[][] ask(Component parent) {
        JPasswordField current = new JPasswordField(20);
        JPasswordField fresh = new JPasswordField(20);
        JPasswordField repeat = new JPasswordField(20);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.add(Theme.label("Current password", Theme.SMALL, Theme.MUTED));
        p.add(current);
        p.add(Theme.label("New password (8+ characters, letters and digits)", Theme.SMALL, Theme.MUTED));
        p.add(fresh);
        p.add(Theme.label("Repeat new password", Theme.SMALL, Theme.MUTED));
        p.add(repeat);
        int r = JOptionPane.showConfirmDialog(parent, p, "Change admin password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return null;
        }
        return new char[][] {current.getPassword(), fresh.getPassword(), repeat.getPassword()};
    }
}

