package com.eduludo.View;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Admin password dialog. In "create" mode it asks for the new password twice. The check itself is done by the
 * callback (the AuthService); this dialog only collects the input and shows the result message.
 */
public final class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private boolean success;

    /**
     * @param attempt receives the password (and the repeat in create mode); returns null on success or an error message
     */
    private LoginDialog(Window owner, boolean create, Function<char[][], String> attempt) {
        super(owner, create ? "Create the admin password" : "Admin login", ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        String intro = create
                ? "<html><b>First time:</b> create the admin password for the question manager.<br>"
                + "At least 8 characters, with letters and digits. Only the admin should know it.</html>"
                : "<html>The question manager shows the answers, so it is for the admin only.<br>Enter the admin password.</html>";
        root.add(Theme.label(intro, Theme.BODY, Theme.TEXT), BorderLayout.NORTH);

        JPasswordField pw = new JPasswordField(20);
        JPasswordField repeat = new JPasswordField(20);
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 4));
        fields.setOpaque(false);
        fields.add(Theme.label("Password", Theme.SMALL, Theme.MUTED));
        fields.add(pw);
        if (create) {
            fields.add(Theme.label("Repeat password", Theme.SMALL, Theme.MUTED));
            fields.add(repeat);
        }
        JLabel error = Theme.label(" ", Theme.BODY_BOLD, Theme.DANGER);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(fields, BorderLayout.CENTER);
        center.add(error, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        RoundButton cancel = RoundButton.secondary("Cancel");
        RoundButton ok = RoundButton.primary(create ? "Create" : "Log in");
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            char[] a = pw.getPassword();
            char[] b = repeat.getPassword();
            try {
                String problem = attempt.apply(new char[][] {a, b});
                if (problem == null) {
                    success = true;
                    dispose();
                } else {
                    error.setText("<html>" + problem + "</html>");
                    pw.setText("");
                    repeat.setText("");
                    pw.requestFocusInWindow();
                }
            } finally {
                Arrays.fill(a, '\0');
                Arrays.fill(b, '\0');
            }
        });
        buttons.add(cancel);
        buttons.add(ok);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        setContentPane(root);
        pack();
        setSize(Math.max(getWidth(), 480), getHeight());
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog; returns true once {@code attempt} accepted the input. */
    public static boolean show(Component parent, boolean create, Function<char[][], String> attempt) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        LoginDialog d = new LoginDialog(owner, create, attempt);
        d.setVisible(true);
        return d.success;
    }
}

