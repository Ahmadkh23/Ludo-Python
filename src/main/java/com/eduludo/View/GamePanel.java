package com.eduludo.View;

import com.eduludo.Model.Game;
import com.eduludo.Model.GameEvent;
import com.eduludo.Model.GameRecord;
import com.eduludo.Model.Phase;
import com.eduludo.Model.Piece;
import com.eduludo.Model.Player;
import com.eduludo.Model.Question;
import com.eduludo.Model.dice.DiceRoll;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

/**
 * The game screen (View): the board on the left; on the right the current player, timer, dice, Roll button,
 * players and the game log. It shows the model and passes user actions to the controller through {@link Actions}.
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** What the user can do on this screen; implemented by the GameController. */
    public interface Actions {
        void roll();

        void pieceClicked(Piece piece);

        void quit();

        void showRules();
    }

    private final BoardCanvas board = new BoardCanvas();
    private final DiceView dice = new DiceView();
    private final JLabel turnLabel = Theme.label(" ", Theme.HEADING, Theme.TEXT);
    private final JLabel hintLabel = Theme.label(" ", Theme.BODY, Theme.MUTED);
    private final JLabel timerLabel = Theme.label("00:00", Theme.MONO, Theme.TEXT);
    private final JLabel levelLabel = Theme.label(" ", Theme.BODY_BOLD, Theme.MUTED);
    private final JLabel rollLabel = Theme.centered(" ", Theme.BODY_BOLD, Theme.TEXT);
    private final RoundButton rollButton = RoundButton.primary("Roll dice");
    private final JPanel playersPanel = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JTextPane log = new JTextPane();
    private final Timer clock;
    private transient Game game;
    private transient Actions actions;
    private boolean rollAllowed;

    public GamePanel() {
        super(new BorderLayout());
        setOpaque(false);
        add(board, BorderLayout.CENTER);

        // Sidebar: fixed-height controls on top, the game log takes the remaining height, buttons at the bottom.
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setBackground(Theme.PANEL);
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        sidebar.setPreferredSize(new Dimension(340, 10));
        JPanel side = new JPanel(new GridBagLayout());
        side.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(levelLabel, BorderLayout.WEST);
        top.add(timerLabel, BorderLayout.EAST);
        addRow(side, top, 0);
        addRow(side, Theme.label("Turn", Theme.SMALL, Theme.MUTED), 10);
        addRow(side, turnLabel, 0);
        addRow(side, hintLabel, 0);
        addRow(side, dice, 12);
        addRow(side, rollLabel, 0);
        rollButton.setFont(Theme.SUBHEADING);
        rollButton.setIcon(Theme.assetIcon("dice", 20));
        rollButton.addActionListener(e -> {
            if (actions != null && rollAllowed) {
                actions.roll();
            }
        });
        addRow(side, rollButton, 8);
        addRow(side, Theme.label("Players", Theme.SMALL, Theme.MUTED), 14);
        playersPanel.setOpaque(false);
        addRow(side, playersPanel, 4);
        addRow(side, Theme.label("Game log", Theme.SMALL, Theme.MUTED), 12);
        log.setEditable(false);
        log.setFont(Theme.SMALL);
        log.setBackground(new Color(0xFBF8F1));
        log.setForeground(Theme.TEXT);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBackground(Theme.PANEL);
        logScroll.getViewport().setBackground(new Color(0xFBF8F1));
        logScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        logScroll.setPreferredSize(new Dimension(300, 120));

        JPanel bottom = new JPanel(new GridLayout(1, 2, 8, 0));
        bottom.setOpaque(false);
        RoundButton rules = RoundButton.secondary("Rules");
        RoundButton quit = RoundButton.danger("Quit game");
        rules.setIcon(Theme.assetIcon("rules"));
        quit.setIcon(Theme.assetIcon("exit"));
        rules.addActionListener(e -> {
            if (actions != null) {
                actions.showRules();
            }
        });
        quit.addActionListener(e -> {
            if (actions != null) {
                actions.quit();
            }
        });
        bottom.add(rules);
        bottom.add(quit);
        sidebar.add(side, BorderLayout.NORTH);
        sidebar.add(logScroll, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        add(sidebar, BorderLayout.EAST);

        board.setOnPieceClicked(p -> {
            if (actions != null) {
                actions.pieceClicked(p);
            }
        });
        clock = new Timer(500, e -> {
            if (game != null) {
                timerLabel.setText(GameRecord.formatSeconds(game.elapsedSeconds()));
            }
        });
    }

    /** Adds a full-width row below the previous one, with some space above it. */
    private static void addRow(JPanel side, Component c, int spaceAbove) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(spaceAbove, 0, 0, 0);
        side.add(c, gc);
    }

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        Theme.paintTabletop(g, this);
        g.dispose();
    }

    /** Connects the panel to a new game. */
    public void bind(Game game, Actions actions) {
        this.game = game;
        this.actions = actions;
        board.setGame(game);
        board.setInteractive(() -> game.phase() == Phase.CHOOSING_MOVE && !game.currentPlayer().isComputer());
        levelLabel.setText(game.difficulty().displayName() + " level");
        if (game.multiplierDie() != null) {
            dice.configure(game.multiplierDie().name(), game.multiplierDie().faces());
        } else {
            dice.configure(null, 0);
        }
        log.setText("");
        rollLabel.setText(" ");
        clock.start();
        refresh();
    }

    public void unbind() {
        clock.stop();
        board.setGame(null);
        game = null;
        actions = null;
    }

    public boolean isAnimating() {
        return board.isAnimating();
    }

    public void setRollAllowed(boolean allowed) {
        this.rollAllowed = allowed;
        rollButton.setEnabled(allowed);
    }

    /** Redraws everything from the current model state. */
    public void refresh() {
        if (game == null) {
            return;
        }
        Player current = game.currentPlayer();
        Color c = Theme.color(current.color());
        turnLabel.setText(current.name() + (current.isComputer() ? "  (computer)" : ""));
        turnLabel.setForeground(Theme.dark(current.color()));
        dice.setAccent(c);
        rollButton.setBase(c);
        rollButton.setTextColor(current.color() == com.eduludo.Model.PlayerColor.YELLOW ? Theme.TEXT : Color.WHITE);
        rollButton.setIcon(Theme.assetIcon("dice", 20,
                current.color() == com.eduludo.Model.PlayerColor.YELLOW ? Theme.TEXT : Color.WHITE));
        hintLabel.setText(hint());
        rebuildPlayers();
        board.repaint();
    }

    private String hint() {
        Player p = game.currentPlayer();
        if (p.isComputer() && game.phase() != Phase.GAME_OVER) {
            return "The computer is playing...";
        }
        return switch (game.phase()) {
            case WAITING_FOR_ROLL -> game.isBonusRollActive() ? "Bonus roll with the UltraBoost die!" : "Roll the dice.";
            case CHOOSING_MOVE -> "Click a highlighted piece to move it.";
            case ANSWERING_QUESTION -> "Answer the question.";
            case CHOOSING_OPPONENT -> "Choose an opponent piece.";
            case GAME_OVER -> "Game over.";
        };
    }

    private void rebuildPlayers() {
        playersPanel.removeAll();
        for (Player p : game.players()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            boolean current = p == game.currentPlayer() && !game.isOver();
            row.setBackground(current ? Theme.light(p.color()) : Theme.PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 6, 0, 0, Theme.color(p.color())),
                    BorderFactory.createEmptyBorder(6, 10, 6, 14)));
            String name = p.name() + (p.isComputer() ? " (computer)" : "");
            JLabel player = Theme.label(name, current ? Theme.BODY_BOLD : Theme.BODY, Theme.TEXT);
            player.setIcon(Theme.assetIcon("pawn", 19, Theme.color(p.color())));
            row.add(player, BorderLayout.CENTER);
            String status;
            int place = game.finishOrder().indexOf(p);
            if (place >= 0 && p.hasFinished()) {
                status = "Place " + (place + 1);
            } else {
                status = p.finishedPieces() + "/4 home" + (p.isSkipNextTurn() ? " - skips next turn" : "");
            }
            row.add(Theme.label(status, Theme.SMALL, Theme.MUTED), BorderLayout.EAST);
            playersPanel.add(row);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    public void showRoll(DiceRoll roll) {
        dice.showRoll(roll);
        rollLabel.setText((roll.bonus() ? "Bonus: " : "") + roll.describe());
    }

    /** Adds a line to the game log, coloured by the kind of event. */
    public void log(GameEvent event) {
        StyledDocument doc = log.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        Color color = switch (event.type()) {
            case CAPTURED, THIRD_SIX -> Theme.DANGER;
            case PLAYER_FINISHED, GAME_OVER, PIECE_FINISHED -> Theme.SUCCESS;
            case SURPRISE, BONUS_ROLL, OPPONENT_MOVED -> Theme.SURPRISE;
            case QUESTION_ASKED, QUESTION_ANSWERED -> new Color(0xE67700);
            case TURN_CHANGED, GAME_STARTED -> Theme.PRIMARY_DARK;
            default -> Theme.TEXT;
        };
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, event.type() == GameEvent.Type.TURN_CHANGED || event.type() == GameEvent.Type.GAME_OVER);
        try {
            doc.insertString(doc.getLength(), event.message() + "\n", attrs);
        } catch (BadLocationException ignored) {
            // cannot happen when appending at the end
        }
        log.setCaretPosition(doc.getLength());
    }

    public int askQuestion(Question q, Player player) {
        return QuestionDialog.ask(this, q, player.name(), Theme.dark(player.color()));
    }

    public int chooseOpponentPiece(List<Piece> candidates) {
        return Dialogs.chooseOpponentPiece(this, candidates, game.board().finishIndex());
    }

    public boolean showGameOver(GameRecord record) {
        clock.stop();
        timerLabel.setText(record.formattedDuration());
        refresh();
        return Dialogs.gameOver(this, record);
    }
}

