package helpdesk.gui;

import helpdesk.dao.LookupDao;
import helpdesk.dao.UserDao;
import helpdesk.model.Lookup;
import helpdesk.model.Role;
import helpdesk.model.User;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

/**
 * Users tab: list, add, edit, and activate / deactivate.
 * Users are never deleted (BR15); they are deactivated instead.
 */
class UsersPanel extends JPanel implements Refreshable {

    private final UserDao userDao = new UserDao();
    private final LookupDao lookupDao = new LookupDao();
    private final DefaultTableModel model = Ui.tableModel(
            "ID", "Name", "Email", "Contact", "Role", "Department", "Team", "Active");
    private final JTable table = Ui.table(model);

    UsersPanel() {
        super(new BorderLayout());
        add(Ui.buttonBar(
                Ui.button("Refresh", this::refresh),
                Ui.button("Add user", this::add),
                Ui.button("Edit user", this::edit),
                Ui.button("Activate / deactivate", this::toggleActive)), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (User u : userDao.findAll()) {
                model.addRow(new Object[]{u.id(), u.fullName(), u.email(), Ui.orEmpty(u.contactNumber()),
                        u.role().label(), u.departmentName(), Ui.orEmpty(u.teamName()), u.active() ? "Yes" : "No"});
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void add() {
        showUserForm(null);
    }

    private void edit() {
        Integer id = Ui.selectedId(table);
        if (id == null) {
            Ui.warn(this, "Select a user in the table first.");
            return;
        }
        try {
            Optional<User> user = userDao.findById(id);
            if (user.isPresent()) {
                showUserForm(user.get());
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Add form when current is null, edit form otherwise. */
    private void showUserForm(User current) {
        try {
            JTextField firstName = new JTextField(current == null ? "" : current.firstName(), 25);
            JTextField lastName = new JTextField(current == null ? "" : current.lastName(), 25);
            JTextField email = new JTextField(current == null ? "" : current.email(), 25);
            JTextField contact = new JTextField(current == null ? "" : Ui.orEmpty(current.contactNumber()), 25);
            JPasswordField password = new JPasswordField(25);
            JComboBox<Role> role = new JComboBox<>(Role.values());
            JComboBox<Lookup> department = Ui.combo(lookupDao.findDepartments());
            JComboBox<Lookup> team = Ui.combo(lookupDao.findSupportTeams());
            if (current != null) {
                role.setSelectedItem(current.role());
                Ui.select(department, current.departmentId());
                Ui.select(team, current.teamId());
            }
            // BR3: only technicians belong to a support team
            Runnable updateTeam = () -> team.setEnabled(role.getSelectedItem() == Role.TECHNICIAN);
            role.addActionListener(e -> updateTeam.run());
            updateTeam.run();

            JPanel form = current == null
                    ? Ui.form("First name", firstName, "Last name", lastName, "Email", email,
                            "Contact number (optional)", contact, "Password", password, "Role", role,
                            "Department", department, "Support team (technicians)", team)
                    : Ui.form("First name", firstName, "Last name", lastName, "Email", email,
                            "Contact number (optional)", contact, "Role", role,
                            "Department", department, "Support team (technicians)", team);
            String title = current == null ? "Add user" : "Edit user #" + current.id();

            while (Ui.showForm(this, title, form)) {
                String problem = validate(firstName, lastName, email, current == null ? password : null);
                if (problem != null) {
                    Ui.warn(this, problem);
                    continue;
                }
                Role selectedRole = (Role) role.getSelectedItem();
                Integer teamId = selectedRole == Role.TECHNICIAN ? Ui.selectedLookupId(team) : null;
                User user = new User(current == null ? 0 : current.id(), firstName.getText().trim(),
                        lastName.getText().trim(), email.getText().trim(), contact.getText().trim(), selectedRole,
                        current == null || current.active(), Ui.selectedLookupId(department), null, teamId, null);
                try {
                    if (current == null) {
                        int id = userDao.insert(user, new String(password.getPassword()));
                        Ui.info(this, "User #" + id + " added.");
                    } else {
                        userDao.update(user);
                        Ui.info(this, "User updated.");
                    }
                    refresh();
                    return;
                } catch (SQLIntegrityConstraintViolationException e) {
                    Ui.warn(this, "That email is already used by another account.");
                }
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private static String validate(JTextField firstName, JTextField lastName, JTextField email,
                                   JPasswordField password) {
        if (firstName.getText().isBlank() || lastName.getText().isBlank()) {
            return "First name and last name are required.";
        }
        String mail = email.getText().trim();
        if (!mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "Please enter a valid email address.";
        }
        if (password != null && password.getPassword().length == 0) {
            return "Password is required.";
        }
        return null;
    }

    private void toggleActive() {
        Integer id = Ui.selectedId(table);
        if (id == null) {
            Ui.warn(this, "Select a user in the table first.");
            return;
        }
        try {
            Optional<User> found = userDao.findById(id);
            if (found.isEmpty()) {
                return;
            }
            User user = found.get();
            boolean activate = !user.active();
            if (Ui.confirm(this, (activate ? "Reactivate " : "Deactivate ") + user.fullName() + "?")) {
                userDao.setActive(user.id(), activate);
                refresh();
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
