package helpdesk.gui;

import helpdesk.model.Lookup;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Small helpers shared by the Swing screens. */
final class Ui {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Used in optional choice lists (for example "no asset"). */
    static final Lookup NONE = new Lookup(0, "(none)");

    private Ui() {
    }

    static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_FORMAT);
    }

    static String orEmpty(String text) {
        return text == null ? "" : text;
    }

    // ---- tables ----

    /** A read-only table model that sorts numbers as numbers. */
    static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return Object.class;
            }
        };
    }

    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        return table;
    }

    /** The value of the first (ID) column of the selected row, or null if nothing is selected. */
    static Integer selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
    }

    // ---- choice lists ----

    static JComboBox<Lookup> combo(List<Lookup> items) {
        return new JComboBox<>(items.toArray(new Lookup[0]));
    }

    /** Selects the item with this id, if it is in the list. */
    static void select(JComboBox<Lookup> combo, Integer id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    static int selectedLookupId(JComboBox<Lookup> combo) {
        return ((Lookup) combo.getSelectedItem()).id();
    }

    /** Returns null when "(none)" is selected. */
    static Integer selectedOptionalId(JComboBox<Lookup> combo) {
        int id = selectedLookupId(combo);
        return id == NONE.id() ? null : id;
    }

    // ---- forms and dialogs ----

    /** Builds a two-column form from label, field, label, field, ... */
    static JPanel form(Object... labelsAndFields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < labelsAndFields.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            String label = labelsAndFields[i].toString();
            panel.add(new JLabel(label.isEmpty() ? "" : label + ":"), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            Component field = (Component) labelsAndFields[i + 1];
            panel.add(field instanceof JTextArea area ? new JScrollPane(area) : field, c);
        }
        return panel;
    }

    /** Shows a form in an OK / Cancel dialog. Returns true if OK was pressed. */
    static boolean showForm(Component parent, String title, JComponent form) {
        return JOptionPane.showConfirmDialog(parent, form, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    static JPanel buttonBar(JButton... buttons) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        for (JButton button : buttons) {
            bar.add(button);
        }
        return bar;
    }

    static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    static JTextArea textArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    static void titled(JComponent component, String title) {
        component.setBorder(BorderFactory.createTitledBorder(title));
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "IT Help Desk", JOptionPane.INFORMATION_MESSAGE);
    }

    static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    static void dbError(Component parent, SQLException e) {
        JOptionPane.showMessageDialog(parent, "Database error:\n" + e.getMessage(), "Database error",
                JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }
}
