package helpdesk.gui;

import helpdesk.dao.ReportDao;
import helpdesk.model.QueryResult;
import helpdesk.model.Report;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Reports tab: pick one of the Week 4 queries, run it, and see the result and its SQL. */
class ReportsPanel extends JPanel {

    private final ReportDao reportDao = new ReportDao();
    private final JList<Report> list = new JList<>(reportDao.findAll().toArray(new Report[0]));
    private final JLabel title = new JLabel("Choose a report on the left, then click Run.");
    private final JLabel feature = new JLabel(" ");
    private final JSpinner parameter = new JSpinner(new SpinnerNumberModel(2, 0, 1000, 1));
    private final JLabel parameterLabel = new JLabel("N =");
    private final JLabel rowCount = new JLabel(" ");
    private final DefaultTableModel model = Ui.tableModel();
    private final JTable table = Ui.table(model);
    private final JTextArea sql = new JTextArea(6, 40);

    ReportsPanel() {
        super(new BorderLayout());

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
                                                          boolean focus) {
                Report r = (Report) value;
                return super.getListCellRendererComponent(l, r.number() + ". " + r.question(), index, selected, focus);
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    run();
                }
            }
        });
        JScrollPane listPane = new JScrollPane(list);
        Ui.titled(listPane, "Reports");

        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        controls.add(parameterLabel);
        controls.add(parameter);
        controls.add(Ui.button("Run", this::run));
        controls.add(rowCount);
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        header.add(title, BorderLayout.NORTH);
        header.add(feature, BorderLayout.CENTER);
        header.add(controls, BorderLayout.SOUTH);

        sql.setEditable(false);
        sql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sqlPane = new JScrollPane(sql);
        Ui.titled(sqlPane, "SQL");
        JScrollPane resultPane = new JScrollPane(table);
        Ui.titled(resultPane, "Result");
        JSplitPane resultAndSql = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultPane, sqlPane);
        resultAndSql.setResizeWeight(0.65);

        JPanel right = new JPanel(new BorderLayout());
        right.add(header, BorderLayout.NORTH);
        right.add(resultAndSql, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPane, right);
        split.setDividerLocation(400);
        add(split, BorderLayout.CENTER);

        list.setSelectedIndex(0);
    }

    private void showSelected() {
        Report r = list.getSelectedValue();
        if (r == null) {
            return;
        }
        title.setText("Query " + r.number() + ": " + r.question());
        feature.setText("SQL feature: " + r.sqlFeature());
        parameterLabel.setVisible(r.needsParameter());
        parameter.setVisible(r.needsParameter());
        sql.setText(r.sql()
                .replace(" FROM ", "\nFROM ").replace(" JOIN ", "\nJOIN ").replace(" LEFT\nJOIN ", "\nLEFT JOIN ")
                .replace(" WHERE ", "\nWHERE ").replace(" GROUP BY ", "\nGROUP BY ").replace(" HAVING ", "\nHAVING ")
                .replace(" ORDER BY ", "\nORDER BY ").replace(" LIMIT ", "\nLIMIT "));
        sql.setCaretPosition(0);
        model.setColumnCount(0);
        model.setRowCount(0);
        rowCount.setText(" ");
    }

    private void run() {
        Report r = list.getSelectedValue();
        if (r == null) {
            Ui.warn(this, "Choose a report first.");
            return;
        }
        Integer value = r.needsParameter() ? (Integer) parameter.getValue() : null;
        if (r.needsParameter()) {
            title.setText("Query " + r.number() + ": " + r.question().replace(" N ", " " + value + " "));
        }
        try {
            QueryResult result = reportDao.run(r, value);
            model.setRowCount(0);
            model.setColumnIdentifiers(result.columns().toArray());
            for (List<Object> row : result.rows()) {
                model.addRow(row.stream().map(ReportsPanel::display).toArray());
            }
            rowCount.setText(result.rows().size() + " row(s)");
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Dates are shown as yyyy-MM-dd HH:mm; numbers stay numbers so they sort correctly. */
    private static Object display(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return Ui.format(dateTime);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return Ui.format(timestamp.toLocalDateTime());
        }
        return value;
    }
}
