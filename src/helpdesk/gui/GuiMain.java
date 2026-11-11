package helpdesk.gui;

import helpdesk.db.Database;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * IT Help Desk Ticketing System - Swing (GUI) version.
 * Uses the same dao and model packages as the console version.
 * Class 4-310 | Group 3
 */
public class GuiMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look and feel
        }

        String connectionInfo;
        try (Connection con = Database.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            connectionInfo = "Connected to " + Database.getUrl() + "  (" + meta.getDatabaseProductName() + " "
                    + meta.getDatabaseProductVersion() + ")";
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the database:\n" + e.getMessage()
                            + "\n\nCheck that MySQL is running and that db.properties is correct.",
                    "IT Help Desk", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new MainFrame(connectionInfo).setVisible(true));
    }
}
