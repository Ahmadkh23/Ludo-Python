package com.eduludo.Controller;

import com.eduludo.Model.Question;
import com.eduludo.security.AdminSession;
import com.eduludo.security.AuthService;
import com.eduludo.service.QuestionService;
import com.eduludo.View.AdminPanel;
import com.eduludo.View.ChangePasswordDialog;
import com.eduludo.View.Dialogs;
import com.eduludo.View.LoginDialog;
import com.eduludo.View.MainFrame;
import com.eduludo.View.QuestionEditorDialog;

import java.util.Arrays;
import java.util.List;

/**
 * Controller of the question management wizard. Opening it requires the admin password (created on first use).
 * Every action goes through {@link QuestionService} with the current {@link AdminSession}; if the session has
 * expired the admin is sent back to the start screen.
 */
public final class AdminController implements AdminPanel.Actions {

    private final AuthService auth;
    private final QuestionService questions;
    private final AdminPanel panel;
    private final MainFrame frame;
    private final Runnable backToStart;
    private AdminSession session;

    public AdminController(AuthService auth, QuestionService questions, AdminPanel panel, MainFrame frame,
                           Runnable backToStart) {
        this.auth = auth;
        this.questions = questions;
        this.panel = panel;
        this.frame = frame;
        this.backToStart = backToStart;
        panel.setActions(this);
    }

    /** Asks for the password (or to create it) and opens the wizard. */
    public void open() {
        boolean create = !auth.isConfigured();
        boolean ok = LoginDialog.show(frame, create, input -> {
            try {
                if (create) {
                    if (!Arrays.equals(input[0], input[1])) {
                        return "The two passwords are different.";
                    }
                    auth.createAdmin(input[0]);
                }
                session = auth.login(input[0]);
                return null;
            } catch (SecurityException | IllegalArgumentException e) {
                return e.getMessage();
            }
        });
        if (ok && reload()) {
            frame.show(MainFrame.ADMIN);
        }
    }

    private boolean reload() {
        return guarded(() -> panel.setQuestions(questions.listAll(session)));
    }

    /** Runs an admin action and shows a clear message if it is refused. */
    private boolean guarded(Runnable action) {
        try {
            action.run();
            return true;
        } catch (SecurityException e) {
            Dialogs.error(panel, e.getMessage() + "\nPlease log in again.");
            closeWizard();
        } catch (IllegalStateException | IllegalArgumentException e) {
            Dialogs.error(panel, e.getMessage());
        } catch (RuntimeException e) {
            Dialogs.error(panel, "The change could not be saved: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void add() {
        Question q = QuestionEditorDialog.edit(panel, null);
        if (q != null) {
            guarded(() -> {
                questions.add(session, q);
                reload();
            });
        }
    }

    @Override
    public void edit(Question existing) {
        Question q = QuestionEditorDialog.edit(panel, existing);
        if (q != null) {
            guarded(() -> {
                questions.update(session, q);
                reload();
            });
        }
    }

    @Override
    public void delete(Question q) {
        if (Dialogs.confirm(panel, "Delete question", "Delete question #" + q.id() + "?\n\n" + q.text())) {
            guarded(() -> {
                questions.delete(session, q.id());
                reload();
            });
        }
    }

    @Override
    public void changePassword() {
        char[][] input = ChangePasswordDialog.ask(panel);
        if (input == null) {
            return;
        }
        try {
            if (!Arrays.equals(input[1], input[2])) {
                Dialogs.error(panel, "The new passwords are different.");
                return;
            }
            if (guarded(() -> auth.changePassword(session, input[0], input[1]))) {
                Dialogs.info(panel, "Password", "The admin password was changed.");
            }
        } finally {
            for (char[] a : input) {
                Arrays.fill(a, '\0');
            }
        }
    }

    @Override
    public void logout() {
        closeWizard();
    }

    /** Ends the session and removes the questions (with answers) from the screen. */
    private void closeWizard() {
        auth.logout(session);
        session = null;
        panel.setQuestions(List.of());
        backToStart.run();
    }
}

