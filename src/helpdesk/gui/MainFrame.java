package helpdesk.gui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The main window: one tab per part of the system. */
class MainFrame extends JFrame {

    MainFrame(String connectionInfo) {
        super("IT Help Desk Ticketing System");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        TicketsPanel tickets = new TicketsPanel();
        UsersPanel users = new UsersPanel();
        AssetsPanel assets = new AssetsPanel();
        ReportsPanel reports = new ReportsPanel();
        ReferencePanel reference = new ReferencePanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tickets", tickets);
        tabs.addTab("Users", users);
        tabs.addTab("Assets", assets);
        tabs.addTab("Reports", reports);
        tabs.addTab("Reference data", reference);
        // reload a tab's data when it is opened, so changes made in other tabs show up
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() instanceof Refreshable panel) {
                panel.refresh();
            }
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.addActionListener(e -> exit());
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(exitItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JLabel status = new JLabel(connectionInfo);
        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setSize(1150, 680);
        setLocationRelativeTo(null);

        tickets.refresh();
    }

    private void exit() {
        if (Ui.confirm(this, "Exit the IT Help Desk system?")) {
            dispose();
            System.exit(0);
        }
    }
}
