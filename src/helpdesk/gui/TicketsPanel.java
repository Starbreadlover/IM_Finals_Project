package helpdesk.gui;

import helpdesk.dao.LookupDao;
import helpdesk.dao.TicketDao;
import helpdesk.model.Lookup;
import helpdesk.model.Ticket;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tickets tab: list, submit, assign, change status / priority, and open details. */
class TicketsPanel extends JPanel implements Refreshable {

    private final TicketDao ticketDao = new TicketDao();
    private final LookupDao lookupDao = new LookupDao();
    private final DefaultTableModel model = Ui.tableModel(
            "ID", "Subject", "Status", "Priority", "Category", "Requester", "Technician", "Asset", "Created");
    private final JTable table = Ui.table(model);

    TicketsPanel() {
        super(new BorderLayout());
        add(Ui.buttonBar(
                Ui.button("Refresh", this::refresh),
                Ui.button("New ticket", this::submit),
                Ui.button("View details / comments", this::details),
                Ui.button("Assign technician", this::assignTechnician),
                Ui.button("Change status", this::changeStatus),
                Ui.button("Change priority", this::changePriority)), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    details();
                }
            }
        });
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (Ticket t : ticketDao.findAll()) {
                model.addRow(new Object[]{t.id(), t.subject(), t.statusName(), t.priorityName(), t.categoryName(),
                        t.requesterName(), Ui.orEmpty(t.technicianName()), Ui.orEmpty(t.assetTag()),
                        Ui.format(t.dateCreated())});
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void submit() {
        try {
            List<Lookup> users = lookupDao.findActiveUsers();
            if (users.isEmpty()) {
                Ui.warn(this, "There are no active users yet. Add one in the Users tab first.");
                return;
            }
            JTextField subject = new JTextField(30);
            JTextArea description = Ui.textArea(4, 30);
            JComboBox<Lookup> requester = Ui.combo(users);
            JComboBox<Lookup> category = Ui.combo(lookupDao.findCategories());
            JComboBox<Lookup> priority = Ui.combo(lookupDao.findPriorities());
            List<Lookup> assets = new ArrayList<>(List.of(Ui.NONE));
            assets.addAll(lookupDao.findAssets());
            JComboBox<Lookup> asset = Ui.combo(assets);
            JPanel form = Ui.form("Subject", subject, "Description", description, "Requester", requester,
                    "Category", category, "Priority", priority, "Asset (optional)", asset);

            while (Ui.showForm(this, "New ticket", form)) {
                if (subject.getText().isBlank() || description.getText().isBlank()) {
                    Ui.warn(this, "Subject and description are required.");
                    continue;
                }
                int id = ticketDao.create(subject.getText().trim(), description.getText().trim(),
                        Ui.selectedLookupId(requester), Ui.selectedLookupId(category),
                        Ui.selectedLookupId(priority), Ui.selectedOptionalId(asset));
                refresh();
                Ui.info(this, "Ticket #" + id + " submitted.");
                return;
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void details() {
        try {
            Optional<Ticket> ticket = selectedTicket();
            if (ticket.isPresent()) {
                new TicketDetailsDialog(SwingUtilities.getWindowAncestor(this), ticket.get()).setVisible(true);
                refresh();
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void assignTechnician() {
        try {
            Optional<Ticket> ticket = selectedTicket();
            if (ticket.isEmpty()) {
                return;
            }
            List<Lookup> technicians = lookupDao.findActiveTechnicians();
            if (technicians.isEmpty()) {
                Ui.warn(this, "There are no active technicians.");
                return;
            }
            JComboBox<Lookup> technician = Ui.combo(technicians);
            Ui.select(technician, ticket.get().technicianId());
            JComboBox<Lookup> assignedBy = Ui.combo(lookupDao.findActiveStaff());
            JPanel form = Ui.form("Ticket", new JLabel("#" + ticket.get().id() + " " + ticket.get().subject()),
                    "Technician", technician, "Assigned by (staff)", assignedBy);
            if (Ui.showForm(this, "Assign technician", form)) {
                ticketDao.assignTechnician(ticket.get().id(), Ui.selectedLookupId(technician),
                        Ui.selectedLookupId(assignedBy));
                refresh();
                Ui.info(this, "Technician assigned.");
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void changeStatus() {
        try {
            Optional<Ticket> ticket = selectedTicket();
            if (ticket.isEmpty()) {
                return;
            }
            JComboBox<Lookup> status = Ui.combo(lookupDao.findStatuses());
            Ui.select(status, ticket.get().statusId());
            JComboBox<Lookup> changedBy = Ui.combo(lookupDao.findActiveUsers());
            JTextField remarks = new JTextField(30);
            JPanel form = Ui.form("Ticket", new JLabel("#" + ticket.get().id() + " " + ticket.get().subject()),
                    "Current status", new JLabel(ticket.get().statusName()),
                    "New status", status, "Changed by", changedBy, "Remarks (optional)", remarks);
            if (Ui.showForm(this, "Change status", form)) {
                ticketDao.changeStatus(ticket.get().id(), Ui.selectedLookupId(status),
                        Ui.selectedLookupId(changedBy), remarks.getText().trim());
                refresh();
                Ui.info(this, "Status updated.");
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void changePriority() {
        try {
            Optional<Ticket> ticket = selectedTicket();
            if (ticket.isEmpty()) {
                return;
            }
            JComboBox<Lookup> priority = Ui.combo(lookupDao.findPriorities());
            Ui.select(priority, ticket.get().priorityId());
            JPanel form = Ui.form("Ticket", new JLabel("#" + ticket.get().id() + " " + ticket.get().subject()),
                    "Priority", priority);
            if (Ui.showForm(this, "Change priority", form)) {
                ticketDao.changePriority(ticket.get().id(), Ui.selectedLookupId(priority));
                refresh();
                Ui.info(this, "Priority updated.");
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Loads the selected ticket, or tells the user to select one. */
    private Optional<Ticket> selectedTicket() throws SQLException {
        Integer id = Ui.selectedId(table);
        if (id == null) {
            Ui.warn(this, "Select a ticket in the table first.");
            return Optional.empty();
        }
        Optional<Ticket> ticket = ticketDao.findById(id);
        if (ticket.isEmpty()) {
            Ui.warn(this, "Ticket #" + id + " no longer exists.");
            refresh();
        }
        return ticket;
    }
}
