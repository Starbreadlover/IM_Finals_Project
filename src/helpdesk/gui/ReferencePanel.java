package helpdesk.gui;

import helpdesk.dao.LookupDao;
import helpdesk.model.Lookup;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

/** Reference data tab: the lookup tables (departments, teams, categories, priorities, statuses). */
class ReferencePanel extends JPanel implements Refreshable {

    private final LookupDao lookupDao = new LookupDao();
    private final DefaultTableModel departments = Ui.tableModel("ID", "Department");
    private final DefaultTableModel teams = Ui.tableModel("ID", "Support team");
    private final DefaultTableModel categories = Ui.tableModel("ID", "Category");
    private final DefaultTableModel priorities = Ui.tableModel("ID", "Priority");
    private final DefaultTableModel statuses = Ui.tableModel("ID", "Ticket status");

    ReferencePanel() {
        super(new GridLayout(1, 5, 6, 6));
        for (DefaultTableModel model : List.of(departments, teams, categories, priorities, statuses)) {
            JTable table = Ui.table(model);
            table.getColumnModel().getColumn(0).setMaxWidth(40);
            add(new JScrollPane(table));
        }
    }

    @Override
    public void refresh() {
        try {
            fill(departments, lookupDao.findDepartments());
            fill(teams, lookupDao.findSupportTeams());
            fill(categories, lookupDao.findCategories());
            fill(priorities, lookupDao.findPriorities());
            fill(statuses, lookupDao.findStatuses());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private static void fill(DefaultTableModel model, List<Lookup> items) {
        model.setRowCount(0);
        for (Lookup item : items) {
            model.addRow(new Object[]{item.id(), item.name()});
        }
    }
}
