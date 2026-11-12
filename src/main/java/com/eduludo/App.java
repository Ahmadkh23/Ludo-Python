package com.eduludo;

import com.eduludo.Controller.AppController;
import com.eduludo.persistence.CsvHistoryRepository;
import com.eduludo.persistence.DataPaths;
import com.eduludo.persistence.EncryptedQuestionRepository;
import com.eduludo.persistence.HistoryRepository;
import com.eduludo.persistence.QuestionRepository;
import com.eduludo.security.AuthService;
import com.eduludo.security.CryptoBox;
import com.eduludo.service.QuestionService;
import com.eduludo.View.Dialogs;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point. Wires the layers together (persistence, security, services, controllers, views) and opens the window.
 * Any unexpected error is written to {@code error.log} in the data folder and shown to the user instead of a silent crash.
 */
public final class App {

    /** Key material for the question-bank file encryption (see SECURITY.md for what this does and does not protect). */
    private static final char[] QUESTION_FILE_SECRET = "EduLudo/question-bank/AES-GCM/v1".toCharArray();

    private App() {
    }

    /** True while an error dialog is on screen, so a failure inside it cannot trigger another one. */
    private static final AtomicBoolean showingCrashDialog = new AtomicBoolean(false);

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("EduLudo is a desktop game and needs a graphical display.");
            System.exit(1);
            return;
        }
        DataPaths paths = DataPaths.defaults();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> reportCrash(paths, error));
        SwingUtilities.invokeLater(() -> launch(paths));
    }

    private static void launch(DataPaths paths) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default look and feel is fine too
        }
        AuthService auth = new AuthService(paths.adminCredentials());
        QuestionService questions = openQuestionBank(paths, auth);
        if (questions == null) {
            System.exit(1);
            return;
        }
        CsvHistoryRepository.seedFromCourseTemplate(paths.history());
        HistoryRepository history = new CsvHistoryRepository(paths.history());
        new AppController(questions, auth, history).show();
    }

    /** Opens the question bank. If the file is damaged or was edited by hand, offers to restore the default questions. */
    private static QuestionService openQuestionBank(DataPaths paths, AuthService auth) {
        QuestionRepository repo = new EncryptedQuestionRepository(paths.questions(), new CryptoBox(QUESTION_FILE_SECRET));
        try {
            return new QuestionService(repo, auth, new Random());
        } catch (SecurityException | IllegalArgumentException e) {
            int choice = JOptionPane.showConfirmDialog(null,
                    "The question bank file is damaged or was changed outside the game:\n" + e.getMessage()
                            + "\n\nRestore the original questions? (The damaged file is kept as a backup.)",
                    "EduLudo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return null;
            }
            try {
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Files.move(paths.questions(), paths.questions().resolveSibling("questions.dat.damaged-" + stamp));
                return new QuestionService(repo, auth, new Random());
            } catch (IOException | RuntimeException ex) {
                Dialogs.error(null, "The question bank could not be restored: " + ex.getMessage());
                return null;
            }
        }
    }

    private static void reportCrash(DataPaths paths, Throwable error) {
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        Path logFile = paths.base().resolve("error.log");
        try {
            Files.createDirectories(paths.base());
            Files.writeString(logFile, LocalDateTime.now() + System.lineSeparator() + sw + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // nothing more we can do
        }
        System.err.println(sw);
        // Show at most one error dialog at a time. If showing it fails too (for example, no display),
        // do not report that failure again: that would loop forever and keep growing error.log.
        if (GraphicsEnvironment.isHeadless() || !showingCrashDialog.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Dialogs.error(null, "Unexpected error: " + error + "\nDetails were saved to " + logFile);
            } catch (Throwable dialogFailure) {
                System.err.println("Could not show the error dialog: " + dialogFailure);
            } finally {
                showingCrashDialog.set(false);
            }
        });
    }
}

