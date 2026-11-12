package com.eduludo.View;

import com.eduludo.Model.Board;
import com.eduludo.Model.Cell;
import com.eduludo.Model.Game;
import com.eduludo.Model.MoveOption;
import com.eduludo.Model.Phase;
import com.eduludo.Model.Piece;
import com.eduludo.Model.Player;
import com.eduludo.Model.PlayerColor;
import com.eduludo.Model.Square;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Draws the board and the pieces (the board part of the View). Pieces glide square by square to their new
 * position. The pieces the player may move are highlighted and can be clicked.
 * The canvas only reads the model; clicks are passed to the controller through a callback.
 */
public final class BoardCanvas extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final double[][] CASTLE_SLOTS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private transient Game game;
    private transient Consumer<Piece> onPieceClicked = p -> { };
    private transient BooleanSupplier interactive = () -> false;
    private final transient Map<Piece, Integer> shown = new IdentityHashMap<>();
    private final transient List<Hit> hits = new ArrayList<>();
    private final Timer animator;
    private int pulse;
    private double cs;
    private double ox;
    private double oy;

    private record Hit(Ellipse2D shape, Piece piece) {
    }

    public BoardCanvas() {
        setPreferredSize(new Dimension(760, 760));
        setMinimumSize(new Dimension(420, 420));
        ToolTipManager.sharedInstance().registerComponent(this);
        animator = new Timer(70, e -> tick());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Piece p = pieceAt(e.getPoint(), true);
                if (p != null) {
                    onPieceClicked.accept(p);
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(pieceAt(e.getPoint(), true) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void setGame(Game game) {
        this.game = game;
        shown.clear();
        if (game != null) {
            for (Player p : game.players()) {
                for (Piece piece : p.pieces()) {
                    shown.put(piece, piece.position());
                }
            }
            animator.start();
        } else {
            animator.stop();
        }
        repaint();
    }

    public void setOnPieceClicked(Consumer<Piece> onPieceClicked) {
        this.onPieceClicked = onPieceClicked;
    }

    /** Whether the human at the table may click pieces right now. */
    public void setInteractive(BooleanSupplier interactive) {
        this.interactive = interactive;
    }

    /** True while at least one piece is still gliding to its square. */
    public boolean isAnimating() {
        if (game == null) {
            return false;
        }
        for (Map.Entry<Piece, Integer> e : shown.entrySet()) {
            if (e.getValue() != e.getKey().position()) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        animator.stop();
    }

    private void tick() {
        pulse = (pulse + 1) % 30;
        if (game == null) {
            return;
        }
        for (Map.Entry<Piece, Integer> e : shown.entrySet()) {
            int actual = e.getKey().position();
            int s = e.getValue();
            if (s == actual) {
                continue;
            }
            if (s == Piece.IN_CASTLE || actual == Piece.IN_CASTLE) {
                e.setValue(actual); // entering or being captured: jump
            } else {
                e.setValue(s + Integer.signum(actual - s));
            }
        }
        repaint();
    }

    private Set<Piece> movablePieces() {
        if (game == null || game.phase() != Phase.CHOOSING_MOVE || !interactive.getAsBoolean()) {
            return Set.of();
        }
        Set<Piece> result = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (MoveOption o : game.options()) {
            result.add(o.piece());
        }
        return result;
    }

    private Piece pieceAt(Point p, boolean onlyMovable) {
        Set<Piece> movable = movablePieces();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.shape().contains(p) && (!onlyMovable || movable.contains(h.piece()))) {
                return h.piece();
            }
        }
        return null;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (game == null || cs <= 0) {
            return null;
        }
        Piece piece = pieceAt(e.getPoint(), false);
        if (piece != null) {
            String where = piece.isInCastle() ? "in the castle" : piece.isFinished() ? "finished"
                    : "square " + (piece.position() + 1) + " of " + (game.board().finishIndex() + 1);
            return piece + " (" + piece.owner().color().displayName() + ") - " + where;
        }
        int col = (int) Math.floor((e.getX() - ox) / cs);
        int row = (int) Math.floor((e.getY() - oy) / cs);
        Square sq = game.board().square(new Cell(row, col));
        return sq == null ? null : describe(sq);
    }

    static String describe(Square sq) {
        return switch (sq.type()) {
            case REGULAR -> "Regular square";
            case SAFE -> "Safe square (star): pieces here can't be captured";
            case START -> "Start square (#) of " + sq.owner().displayName() + " - protected";
            case ARROW -> sq.owner().displayName() + " arrow: " + sq.owner().displayName()
                    + " pieces turn onto the next inner track here";
            case HOME -> sq.owner().displayName() + " home path - protected, needs an exact roll";
            case QUESTION -> sq.questionLevel().displayName() + " question: right answer +" + sq.questionLevel().steps()
                    + ", wrong answer -" + sq.questionLevel().steps();
            case SURPRISE -> "Surprise square (protected): something random happens!";
        };
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Theme.paintTabletop(g, this);
        if (game == null) {
            g.dispose();
            return;
        }
        Board board = game.board();
        int n = board.size();
        double side = Math.min(getWidth(), getHeight()) - 16;
        cs = side / n;
        ox = (getWidth() - cs * n) / 2.0;
        oy = (getHeight() - cs * n) / 2.0;

        // board shadow and base
        g.setColor(new Color(0, 0, 0, 58));
        g.fill(new RoundRectangle2D.Double(ox + 5, oy + 7, cs * n, cs * n, 22, 22));
        g.setColor(Theme.FELT);
        g.fill(new RoundRectangle2D.Double(ox - 7, oy - 7, cs * n + 14, cs * n + 14, 22, 22));
        g.setColor(new Color(230, 215, 184, 145));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(ox - 4, oy - 4, cs * n + 8, cs * n + 8, 18, 18));

        int off = board.difficulty().outerRings();
        for (PlayerColor c : PlayerColor.values()) {
            drawCastle(g, board, c);
        }
        drawCenter(g, off);
        Set<Cell> finishCells = new java.util.HashSet<>();
        for (PlayerColor c : PlayerColor.values()) {
            List<Cell> path = board.path(c);
            finishCells.add(path.get(path.size() - 1));
        }
        for (Square sq : board.squares()) {
            if (!finishCells.contains(sq.cell())) {
                drawSquare(g, board, sq, off, n);
            }
        }
        drawPieces(g, board);
        g.dispose();
    }

    private double x(double col) {
        return ox + col * cs;
    }

    private double y(double row) {
        return oy + row * cs;
    }

    private void drawCastle(Graphics2D g, Board board, PlayerColor color) {
        double[] center = board.castleCenter(color);
        double r0 = center[0] - 3;
        double c0 = center[1] - 3;
        g.setColor(Theme.color(color));
        g.fill(new RoundRectangle2D.Double(x(c0), y(r0), 6 * cs, 6 * cs, cs * 0.6, cs * 0.6));
        g.setColor(Theme.PANEL);
        g.fill(new RoundRectangle2D.Double(x(c0 + 0.8), y(r0 + 0.8), 4.4 * cs, 4.4 * cs, cs, cs));
        for (double[] slot : CASTLE_SLOTS) {
            double cx = x(center[1] + slot[1]);
            double cy = y(center[0] + slot[0]);
            double rad = cs * 0.62;
            g.setColor(Theme.light(color));
            g.fill(new Ellipse2D.Double(cx - rad, cy - rad, rad * 2, rad * 2));
        }
    }

    private void drawCenter(Graphics2D g, int off) {
        double a = off + 6;
        double b = off + 9;
        double mid = off + 7.5;
        triangle(g, PlayerColor.RED, a, a, b, a, mid);   // left
        triangle(g, PlayerColor.GREEN, a, a, a, b, mid); // top
        triangle(g, PlayerColor.YELLOW, a, b, b, b, mid); // right
        triangle(g, PlayerColor.BLUE, b, a, b, b, mid);  // bottom
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new java.awt.geom.Rectangle2D.Double(x(a), y(a), 3 * cs, 3 * cs));
    }

    private void triangle(Graphics2D g, PlayerColor color, double r1, double c1, double r2, double c2, double mid) {
        Path2D p = new Path2D.Double();
        p.moveTo(x(c1), y(r1));
        p.lineTo(x(c2), y(r2));
        p.lineTo(x(mid), y(mid));
        p.closePath();
        g.setColor(Theme.color(color));
        g.fill(p);
        g.setColor(Theme.dark(color));
        g.draw(p);
    }

    private void drawSquare(Graphics2D g, Board board, Square sq, int off, int n) {
        Cell c = sq.cell();
        double sx = x(c.col());
        double sy = y(c.row());
        int ringDepth = Math.min(Math.min(c.row(), c.col()), Math.min(n - 1 - c.row(), n - 1 - c.col()));
        boolean onRing = ringDepth < off;
        Color fill = switch (sq.type()) {
            case REGULAR, ARROW -> onRing ? (ringDepth % 2 == 0 ? Theme.RING : Theme.mix(Theme.RING, Color.WHITE, 0.5)) : Theme.TRACK;
            case SAFE -> Theme.SAFE;
            case START -> Theme.light(sq.owner());
            case HOME -> Theme.color(sq.owner());
            case QUESTION -> Theme.color(sq.questionLevel());
            case SURPRISE -> Theme.SURPRISE;
        };
        g.setColor(fill);
        g.fill(new java.awt.geom.Rectangle2D.Double(sx, sy, cs, cs));
        g.setColor(new Color(Theme.BOARD_LINE.getRed(), Theme.BOARD_LINE.getGreen(), Theme.BOARD_LINE.getBlue(), 175));
        g.setStroke(new BasicStroke(Math.max(0.8f, (float) cs / 52f)));
        g.draw(new java.awt.geom.Rectangle2D.Double(sx, sy, cs, cs));

        double cx = sx + cs / 2;
        double cy = sy + cs / 2;
        switch (sq.type()) {
            case SAFE -> {
                g.setColor(new Color(0xB99B5E));
                g.fill(star(cx, cy, cs * 0.35, cs * 0.17));
            }
            case START -> {
                g.setColor(new Color(255, 255, 255, 88));
                g.fill(new Ellipse2D.Double(cx - cs * 0.34, cy - cs * 0.34, cs * 0.68, cs * 0.68));
                text(g, "#", cx, cy, Theme.dark(sq.owner()), Font.BOLD, 0.56);
            }
            case ARROW -> drawArrow(g, board, sq, cx, cy);
            case QUESTION -> {
                g.setColor(new Color(255, 255, 255, 75));
                g.fill(new Ellipse2D.Double(cx - cs * 0.34, cy - cs * 0.34, cs * 0.68, cs * 0.68));
                text(g, "?", cx, cy, Color.WHITE, Font.BOLD, 0.58);
            }
            case SURPRISE -> {
                g.setColor(new Color(255, 247, 232, 90));
                g.fill(star(cx, cy, cs * 0.35, cs * 0.17));
                text(g, "!", cx, cy, Color.WHITE, Font.BOLD, 0.54);
            }
            default -> { }
        }
    }

    private void drawArrow(Graphics2D g, Board board, Square sq, double cx, double cy) {
        List<Cell> path = board.path(sq.owner());
        int idx = path.indexOf(sq.cell());
        if (idx < 0 || idx + 1 >= path.size()) {
            return;
        }
        Cell next = path.get(idx + 1);
        double angle = Math.atan2(next.row() - sq.cell().row(), next.col() - sq.cell().col());
        Path2D arrow = new Path2D.Double();
        double s = cs * 0.36;
        arrow.moveTo(-s, -s * 0.22);
        arrow.lineTo(s * 0.15, -s * 0.22);
        arrow.lineTo(s * 0.15, -s * 0.6);
        arrow.lineTo(s, 0);
        arrow.lineTo(s * 0.15, s * 0.6);
        arrow.lineTo(s * 0.15, s * 0.22);
        arrow.lineTo(-s, s * 0.22);
        arrow.closePath();
        AffineTransform t = new AffineTransform();
        t.translate(cx, cy);
        t.rotate(angle);
        g.setColor(Theme.color(sq.owner()));
        g.fill(t.createTransformedShape(arrow));
        g.setColor(Theme.dark(sq.owner()));
        g.draw(t.createTransformedShape(arrow));
    }

    private void text(Graphics2D g, String s, double cx, double cy, Color color, int style, double scale) {
        g.setFont(new Font(Font.SANS_SERIF, style, Math.max(8, (int) (cs * scale))));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy - fm.getHeight() / 2.0 + fm.getAscent()));
    }

    private static Path2D star(double cx, double cy, double outer, double inner) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double px = cx + r * Math.cos(a);
            double py = cy + r * Math.sin(a);
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
        return p;
    }

    private void drawPieces(Graphics2D g, Board board) {
        hits.clear();
        Set<Piece> movable = movablePieces();
        Map<Cell, List<Piece>> byCell = new LinkedHashMap<>();
        Map<Piece, Point2D> positions = new HashMap<>();
        for (Player player : game.players()) {
            List<Cell> path = board.path(player.color());
            double[] castle = board.castleCenter(player.color());
            for (Piece piece : player.pieces()) {
                int idx = shown.getOrDefault(piece, piece.position());
                if (idx == Piece.IN_CASTLE) {
                    double[] slot = CASTLE_SLOTS[piece.number() - 1];
                    positions.put(piece, new Point2D.Double(castle[1] + slot[1], castle[0] + slot[0]));
                } else {
                    byCell.computeIfAbsent(path.get(idx), k -> new ArrayList<>()).add(piece);
                }
            }
        }
        Map<Piece, Double> radius = new HashMap<>();
        for (Map.Entry<Cell, List<Piece>> e : byCell.entrySet()) {
            List<Piece> list = e.getValue();
            double cx = e.getKey().col() + 0.5;
            double cy = e.getKey().row() + 0.5;
            for (int i = 0; i < list.size(); i++) {
                double dx = 0;
                double dy = 0;
                if (list.size() == 2) {
                    dx = i == 0 ? -0.2 : 0.2;
                } else if (list.size() > 2) {
                    dx = (i % 2 == 0) ? -0.2 : 0.2;
                    dy = (i / 2 == 0) ? -0.2 : 0.2;
                }
                positions.put(list.get(i), new Point2D.Double(cx + dx, cy + dy));
                radius.put(list.get(i), list.size() == 1 ? 0.36 : 0.25);
            }
        }
        // draw in a stable order so the movable pieces are on top
        List<Piece> order = new ArrayList<>(positions.keySet());
        order.sort((a, b) -> Boolean.compare(movable.contains(a), movable.contains(b)));
        double glow = 0.5 + 0.5 * Math.sin(pulse / 30.0 * 2 * Math.PI);
        for (Piece piece : order) {
            Point2D pos = positions.get(piece);
            double r = radius.getOrDefault(piece, 0.42) * cs;
            double px = x(pos.getX());
            double py = y(pos.getY());
            Color base = Theme.color(piece.owner().color());
            if (movable.contains(piece)) {
                int alpha = (int) (90 + 120 * glow);
                g.setColor(new Color(255, 255, 255, alpha));
                double gr = r * 1.45;
                g.fill(new Ellipse2D.Double(px - gr, py - gr, gr * 2, gr * 2));
                g.setColor(new Color(Theme.dark(piece.owner().color()).getRed(), Theme.dark(piece.owner().color()).getGreen(),
                        Theme.dark(piece.owner().color()).getBlue(), alpha));
                g.setStroke(new BasicStroke((float) Math.max(2, cs / 12)));
                g.draw(new Ellipse2D.Double(px - gr, py - gr, gr * 2, gr * 2));
            }
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Ellipse2D.Double(px - r + r * 0.12, py - r + r * 0.22, r * 2, r * 2));
            Ellipse2D body = new Ellipse2D.Double(px - r, py - r, r * 2, r * 2);
            g.setColor(base);
            g.fill(body);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke((float) Math.max(1.5, r / 6)));
            g.draw(new Ellipse2D.Double(px - r * 0.78, py - r * 0.78, r * 1.56, r * 1.56));
            g.setColor(Theme.dark(piece.owner().color()));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(body);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int) (r * 0.95))));
            FontMetrics fm = g.getFontMetrics();
            String num = Integer.toString(piece.number());
            g.setColor(piece.owner().color() == PlayerColor.YELLOW ? Theme.TEXT : Color.WHITE);
            g.drawString(num, (float) (px - fm.stringWidth(num) / 2.0), (float) (py - fm.getHeight() / 2.0 + fm.getAscent()));
            hits.add(new Hit(new Ellipse2D.Double(px - r * 1.2, py - r * 1.2, r * 2.4, r * 2.4), piece));
        }
    }
}

