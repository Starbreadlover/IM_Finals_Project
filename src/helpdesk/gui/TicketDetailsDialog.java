package helpdesk.gui;

import helpdesk.dao.CommentDao;
import helpdesk.dao.LookupDao;
import helpdesk.dao.TicketDao;
import helpdesk.model.Comment;
import helpdesk.model.HistoryEntry;
import helpdesk.model.Lookup;
import helpdesk.model.Ticket;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;

/** Shows one ticket with its status history and comments; comments can be added and deleted here. */
class TicketDetailsDialog extends JDialog {

    private final CommentDao commentDao = new CommentDao();
    private final LookupDao lookupDao = new LookupDao();
    private final Ticket ticket;
    private final DefaultTableModel comments = Ui.tableModel("ID", "Posted", "Author", "Internal", "Comment");
    private final JTable commentTable = Ui.table(comments);

    TicketDetailsDialog(Window owner, Ticket ticket) throws SQLException {
        super(owner, "Ticket #" + ticket.id() + ": " + ticket.subject(), ModalityType.APPLICATION_MODAL);
        this.ticket = ticket;

        JTextArea summary = new JTextArea(summaryText(ticket));
        summary.setEditable(false);
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summary.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        DefaultTableModel history = Ui.tableModel("Date", "Status", "Changed by", "Remarks");
        for (HistoryEntry h : new TicketDao().findHistory(ticket.id())) {
            history.addRow(new Object[]{Ui.format(h.dateChanged()), h.statusName(), h.changedByName(),
                    Ui.orEmpty(h.remarks())});
        }
        JScrollPane historyPane = new JScrollPane(Ui.table(history));
        Ui.titled(historyPane, "Status history");

        JPanel commentPanel = new JPanel(new BorderLayout());
        Ui.titled(commentPanel, "Comments");
        commentPanel.add(Ui.buttonBar(
                Ui.button("Add comment", this::addComment),
                Ui.button("Delete selected comment", this::deleteComment)), BorderLayout.NORTH);
        commentPanel.add(new JScrollPane(commentTable), BorderLayout.CENTER);
        commentTable.getColumnModel().getColumn(0).setMaxWidth(50);
        commentTable.getColumnModel().getColumn(4).setPreferredWidth(320);
        loadComments();

        JSplitPane lists = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyPane, commentPanel);
        lists.setResizeWeight(0.4);

        setLayout(new BorderLayout());
        add(new JScrollPane(summary), BorderLayout.NORTH);
        add(lists, BorderLayout.CENTER);
        add(Ui.buttonBar(Ui.button("Close", this::dispose)), BorderLayout.SOUTH);
        setSize(820, 640);
        setLocationRelativeTo(owner);
    }

    private static String summaryText(Ticket t) {
        return "Description : " + t.description() + "\n"
                + "Status      : " + t.statusName() + "        Priority : " + t.priorityName() + "\n"
                + "Category    : " + t.categoryName() + "\n"
                + "Requester   : " + t.requesterName() + "\n"
                + "Technician  : " + (t.technicianName() == null ? "(unassigned)" : t.technicianName()) + "\n"
                + "Asset       : " + (t.assetTag() == null ? "-" : t.assetTag()) + "\n"
                + "Created     : " + date(t.dateCreated()) + "        Updated  : " + date(t.dateUpdated()) + "\n"
                + "Resolved    : " + date(t.dateResolved()) + "        Closed   : " + date(t.dateClosed());
    }

    private static String date(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "-" : Ui.format(dateTime);
    }

    private void loadComments() {
        try {
            comments.setRowCount(0);
            for (Comment c : commentDao.findByTicket(ticket.id())) {
                comments.addRow(new Object[]{c.id(), Ui.format(c.datePosted()), c.authorName(),
                        c.internal() ? "Yes" : "No", c.text()});
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void addComment() {
        try {
            JComboBox<Lookup> author = Ui.combo(lookupDao.findActiveUsers());
            JTextArea text = Ui.textArea(4, 30);
            JCheckBox internal = new JCheckBox("Internal note (IT staff only)");
            JPanel form = Ui.form("Written by", author, "Comment", text, "", internal);
            while (Ui.showForm(this, "Add comment", form)) {
                if (text.getText().isBlank()) {
                    Ui.warn(this, "The comment cannot be empty.");
                    continue;
                }
                commentDao.insert(ticket.id(), Ui.selectedLookupId(author), text.getText().trim(),
                        internal.isSelected());
                loadComments();
                return;
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void deleteComment() {
        Integer id = Ui.selectedId(commentTable);
        if (id == null) {
            Ui.warn(this, "Select a comment first.");
            return;
        }
        if (!Ui.confirm(this, "Delete comment #" + id + "?")) {
            return;
        }
        try {
            commentDao.delete(id);
            loadComments();
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
