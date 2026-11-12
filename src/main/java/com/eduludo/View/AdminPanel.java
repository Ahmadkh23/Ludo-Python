package com.eduludo.View;

import com.eduludo.Model.Question;
import com.eduludo.Model.QuestionLevel;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Question management wizard (admin only): list, search, add, edit and delete questions. */
public final class AdminPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Implemented by the AdminController. */
    public interface Actions {
        void add();

        void edit(Question q);

        void delete(Question q);

        void changePassword();

        void logout();
    }

    private final Model model = new Model();
    private final JTable table = new JTable(model) {
        private static final long serialVersionUID = 1L;

        /** Shows the full text of long cells (question text, answers) on hover. */
        @Override
        public String getToolTipText(java.awt.event.MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            int col = columnAtPoint(e.getPoint());
            if (row < 0 || col < 2) {
                return null;
            }
            Object v = getValueAt(row, col);
            return v == null ? null : "<html><body style='width:380px'>" + escapeHtml(v.toString()) + "</body></html>";
        }
    };
    private final transient TableRowSorter<Model> sorter = new TableRowSorter<>(model);
    private final JLabel status = Theme.label(" ", Theme.BODY, Theme.MUTED);
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> levelFilter = new JComboBox<>(new String[] {"All levels", "Easy", "Medium", "Hard"});
    private transient Actions actions;

    public AdminPanel() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = Theme.label("Question manager", Theme.HEADING, Theme.TEXT);
        title.setIcon(Theme.assetIcon("admin", 28, Theme.PRIMARY));
        top.add(title, BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        filters.add(Theme.label("Search:", Theme.BODY, Theme.MUTED));
        filters.add(search);
        filters.add(levelFilter);
        top.add(filters, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.PANEL);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(new Color(0xDCE9DF));
        table.setSelectionForeground(Theme.TEXT);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Theme.PANEL);
        table.getTableHeader().setOpaque(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(480);
        table.getColumnModel().getColumn(4).setPreferredWidth(240);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selected() != null && actions != null) {
                    actions.edit(selected());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(Theme.PANEL);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scroll, BorderLayout.CENTER);

        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        };
        search.getDocument().addDocumentListener(dl);
        levelFilter.addActionListener(e -> applyFilter());
        search.setBackground(Theme.PANEL);
        search.setForeground(Theme.TEXT);
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        RoundButton add = RoundButton.primary("Add");
        RoundButton edit = RoundButton.secondary("Edit");
        RoundButton delete = RoundButton.danger("Delete");
        RoundButton password = RoundButton.secondary("Change password");
        RoundButton logout = RoundButton.secondary("Log out");
        password.setIcon(Theme.assetIcon("admin"));
        logout.setIcon(Theme.assetIcon("exit"));
        add.addActionListener(e -> actions.add());
        edit.addActionListener(e -> {
            if (selected() == null) {
                Dialogs.info(this, "Edit", "Select a question first.");
            } else {
                actions.edit(selected());
            }
        });
        delete.addActionListener(e -> {
            if (selected() == null) {
                Dialogs.info(this, "Delete", "Select a question first.");
            } else {
                actions.delete(selected());
            }
        });
        password.addActionListener(e -> actions.changePassword());
        logout.addActionListener(e -> actions.logout());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(password);
        buttons.add(logout);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        Theme.paintTabletop(g, this);
        g.dispose();
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    public void setQuestions(List<Question> questions) {
        model.rows = new ArrayList<>(questions);
        model.fireTableDataChanged();
        long easy = questions.stream().filter(q -> q.level() == QuestionLevel.EASY).count();
        long medium = questions.stream().filter(q -> q.level() == QuestionLevel.MEDIUM).count();
        long hard = questions.stream().filter(q -> q.level() == QuestionLevel.HARD).count();
        status.setText(questions.size() + " questions  (easy " + easy + ", medium " + medium + ", hard " + hard
                + ")    Double-click a row to edit.");
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Question selected() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : model.rows.get(table.convertRowIndexToModel(viewRow));
    }

    private void applyFilter() {
        List<RowFilter<Model, Integer>> filters = new ArrayList<>();
        String text = search.getText().strip();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        int lvl = levelFilter.getSelectedIndex();
        if (lvl > 0) {
            String name = QuestionLevel.values()[lvl - 1].displayName();
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(name) + "$", 1));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private static final class Model extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"ID", "Level", "Topic", "Question", "Correct answer"};
        private transient List<Question> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int c) {
            return COLUMNS[c];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int r, int c) {
            Question q = rows.get(r);
            return switch (c) {
                case 0 -> q.id();
                case 1 -> q.level().displayName();
                case 2 -> q.topic();
                case 3 -> q.text();
                default -> (char) ('A' + q.correctIndex()) + ". " + q.options().get(q.correctIndex());
            };
        }
    }
}

