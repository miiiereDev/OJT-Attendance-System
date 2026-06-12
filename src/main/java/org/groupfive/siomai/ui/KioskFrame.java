package org.groupfive.siomai.ui;

import org.groupfive.siomai.exception.AppException;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.service.KioskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Full-screen Kiosk Frame for employee clock-in and clock-out.
 */
public class KioskFrame extends JFrame {
    private static final Color BG_COLOR = new Color(18, 22, 33);         // Sleek deep dark
    private static final Color CARD_BG = new Color(30, 37, 53);          // Card container
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(160, 174, 192);
    private static final Color ACCENT_GREEN = new Color(46, 204, 113);   // Clock In
    private static final Color ACCENT_RED = new Color(231, 76, 60);       // Clock Out
    private static final Color BUTTON_BG = new Color(44, 62, 80);         // View stats / Back button

    private final JFrame parent;
    private final KioskService kioskService = new KioskService();

    private JTextField searchField;
    private JComboBox<Employee> employeeComboBox;
    private JTextField codeField;
    private JLabel statusLabel;
    private List<Employee> activeEmployees = new ArrayList<>();

    public KioskFrame(JFrame parent) {
        this.parent = parent;
        setTitle("OJT Attendance Kiosk");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_COLOR);

        // Load active employees
        try {
            activeEmployees = kioskService.getAllActiveEmployees();
        } catch (SQLException e) {
            activeEmployees = new ArrayList<>();
        }

        // Root Panel layout
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(30, 30, 30, 30));
        add(root);

        // Header Panel (Title + Live Clock)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel kioskTitle = new JLabel("KIOSK TERMINAL", JLabel.LEFT);
        kioskTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        kioskTitle.setForeground(TEXT_MUTED);
        headerPanel.add(kioskTitle, BorderLayout.WEST);

        // Right header container
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 0));
        rightHeader.setOpaque(false);

        // Sync Status Panel
        JPanel syncPanel = new JPanel();
        syncPanel.setLayout(new BoxLayout(syncPanel, BoxLayout.Y_AXIS));
        syncPanel.setOpaque(false);

        JLabel syncStatusLabel = new JLabel("● Synced");
        syncStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        syncStatusLabel.setForeground(ACCENT_GREEN);

        JLabel syncTimeLabel = new JLabel("Last Sync: Just Now");
        syncTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        syncTimeLabel.setForeground(TEXT_MUTED);

        JButton syncBtn = new JButton("Sync Data");
        syncBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        syncBtn.setForeground(TEXT_PRIMARY);
        syncBtn.setBackground(BUTTON_BG);
        syncBtn.setFocusPainted(false);
        syncBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 68, 92), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        syncBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        syncBtn.addActionListener(e -> {
            if (kioskService.getDbOps() instanceof org.groupfive.siomai.database.CachedDatabaseOperations) {
                org.groupfive.siomai.database.CachedDatabaseOperations cachedOps =
                        (org.groupfive.siomai.database.CachedDatabaseOperations) kioskService.getDbOps();
                boolean triggered = cachedOps.forceRefresh();
                if (triggered) {
                    syncStatusLabel.setText("↻ Syncing...");
                    syncStatusLabel.setForeground(new Color(230, 126, 34)); // Orange
                } else {
                    JOptionPane.showMessageDialog(this, "Please wait at least 5 seconds between manual syncs.",
                            "Cooldown Active", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        syncBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                syncBtn.setBackground(BUTTON_BG.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                syncBtn.setBackground(BUTTON_BG);
            }
        });

        syncPanel.add(syncStatusLabel);
        syncPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        syncPanel.add(syncTimeLabel);
        syncPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        syncPanel.add(syncBtn);

        // Live Clock Panel
        JPanel clockPanel = new JPanel(new GridLayout(2, 1));
        clockPanel.setOpaque(false);
        JLabel timeLabel = new JLabel("", JLabel.RIGHT);
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        timeLabel.setForeground(TEXT_PRIMARY);
        JLabel dateLabel = new JLabel("", JLabel.RIGHT);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setForeground(TEXT_MUTED);
        clockPanel.add(timeLabel);
        clockPanel.add(dateLabel);

        rightHeader.add(syncPanel);
        rightHeader.add(clockPanel);
        headerPanel.add(rightHeader, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Run Live Clock timer
        runClock(timeLabel, dateLabel);

        // Timer to update sync indicators & auto-refresh UI
        final long[] lastSeenSyncTime = {System.currentTimeMillis()};
        new Timer(2000, e -> {
            if (kioskService.getDbOps() instanceof org.groupfive.siomai.database.CachedDatabaseOperations) {
                org.groupfive.siomai.database.CachedDatabaseOperations cachedOps =
                        (org.groupfive.siomai.database.CachedDatabaseOperations) kioskService.getDbOps();

                if (cachedOps.isSyncing()) {
                    syncStatusLabel.setText("↻ Syncing...");
                    syncStatusLabel.setForeground(new Color(230, 126, 34));
                } else {
                    syncStatusLabel.setText("● Synced");
                    syncStatusLabel.setForeground(ACCENT_GREEN);
                }

                long currentSyncTime = cachedOps.getLastSyncTime();
                if (currentSyncTime > lastSeenSyncTime[0]) {
                    lastSeenSyncTime[0] = currentSyncTime;
                    refreshActiveEmployees(); // Refresh items in selector!
                }

                long elapsedSec = (System.currentTimeMillis() - currentSyncTime) / 1000;
                if (elapsedSec < 5) {
                    syncTimeLabel.setText("Last Sync: Just Now");
                } else {
                    syncTimeLabel.setText("Last Sync: " + elapsedSec + "s ago");
                }
            }
        }).start();

        // Center card container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JPanel kioskCard = new JPanel();
        kioskCard.setLayout(new BoxLayout(kioskCard, BoxLayout.Y_AXIS));
        kioskCard.setBackground(CARD_BG);
        kioskCard.setPreferredSize(new Dimension(500, 580));
        kioskCard.setMaximumSize(new Dimension(500, 580));
        kioskCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(45, 55, 72), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        JLabel cardTitle = new JLabel("EMPLOYEE SIGN-IN");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        cardTitle.setForeground(TEXT_PRIMARY);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cardSub = new JLabel("Select employee & enter your verification code");
        cardSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardSub.setForeground(TEXT_MUTED);
        cardSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        kioskCard.add(cardTitle);
        kioskCard.add(Box.createRigidArea(new Dimension(0, 5)));
        kioskCard.add(cardSub);
        kioskCard.add(Box.createRigidArea(new Dimension(0, 30)));

        // Inputs Panel
        JPanel inputsPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        inputsPanel.setOpaque(false);
        inputsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel searchLabel = new JLabel("Search Employee (Name or Code):");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(TEXT_MUTED);

        searchField = new JTextField();
        styleTextField(searchField);

        JLabel selectLabel = new JLabel("Select Employee:");
        selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectLabel.setForeground(TEXT_MUTED);

        employeeComboBox = new JComboBox<>();
        styleComboBox(employeeComboBox);

        // Initial filtering
        filterEmployees("");

        // Add filter document listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                SwingUtilities.invokeLater(() -> filterEmployees(searchField.getText()));
            }
        });

        JLabel codeLabel = new JLabel("Daily Verification Code:");
        codeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        codeLabel.setForeground(TEXT_MUTED);

        codeField = new JPasswordField() {
            // Use password field structure but allow normal rendering or standard input.
            // Actually standard JTextField is cleaner, let's keep it as JTextField so they see the numbers.
        };
        codeField = new JTextField();
        styleTextField(codeField);

        inputsPanel.add(searchLabel);
        inputsPanel.add(searchField);
        inputsPanel.add(selectLabel);
        inputsPanel.add(employeeComboBox);
        inputsPanel.add(codeLabel);
        inputsPanel.add(codeField);
        kioskCard.add(inputsPanel);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setOpaque(false);
        buttonsPanel.setBorder(new EmptyBorder(25, 0, 15, 0));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 20, 0));
        row1.setOpaque(false);
        JButton clockInBtn = createButton("CLOCK IN", ACCENT_GREEN);
        clockInBtn.addActionListener(e -> triggerClockAction(true));

        JButton clockOutBtn = createButton("CLOCK OUT", ACCENT_RED);
        clockOutBtn.addActionListener(e -> triggerClockAction(false));

        row1.add(clockInBtn);
        row1.add(clockOutBtn);
        buttonsPanel.add(row1);

        buttonsPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton viewStatsBtn = createButton("VIEW MY STATS & HISTORY", BUTTON_BG);
        viewStatsBtn.addActionListener(e -> triggerViewStatsAction());
        viewStatsBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        viewStatsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(viewStatsBtn);

        kioskCard.add(buttonsPanel);

        // Status Label
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        kioskCard.add(statusLabel);

        centerPanel.add(kioskCard);
        root.add(centerPanel, BorderLayout.CENTER);

        // Footer panel (Exit options)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        JButton backBtn = new JButton("Back to Launcher");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backBtn.setForeground(TEXT_PRIMARY);
        backBtn.setBackground(BUTTON_BG);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            this.dispose();
            parent.setVisible(true);
        });

        // Hover effect for back button
        backBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                backBtn.setBackground(BUTTON_BG.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backBtn.setBackground(BUTTON_BG);
            }
        });

        footerPanel.add(backBtn, BorderLayout.EAST);
        root.add(footerPanel, BorderLayout.SOUTH);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(new Color(23, 28, 41));
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 68, 92), 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleComboBox(JComboBox<Employee> box) {
        box.setBackground(new Color(23, 28, 41));
        box.setForeground(TEXT_PRIMARY);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 68, 92), 1, true),
                new EmptyBorder(5, 5, 5, 5)
        ));

        // Use a customized renderer for list items to match the theme.
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Employee) {
                    Employee emp = (Employee) value;
                    setText(emp.getEmployeeCode() + " - " + emp.getFullName());
                } else if (value == null) {
                    setText("");
                }
                setBackground(isSelected ? new Color(45, 55, 72) : new Color(23, 28, 41));
                setForeground(TEXT_PRIMARY);
                return this;
            }
        });

        // Try to style the popup panel list background if using BasicComboPopup
        Object child = box.getAccessibleContext().getAccessibleChild(0);
        if (child instanceof BasicComboPopup) {
            BasicComboPopup popup = (BasicComboPopup) child;
            popup.getList().setBackground(new Color(23, 28, 41));
            popup.getList().setForeground(TEXT_PRIMARY);
            popup.getList().setSelectionBackground(new Color(45, 55, 72));
            popup.getList().setSelectionForeground(TEXT_PRIMARY);
        }
    }

    private JButton createButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(baseColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
            }
        });

        return btn;
    }

    private void runClock(JLabel timeLabel, JLabel dateLabel) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        new Timer(1000, e -> {
            Date now = new Date();
            timeLabel.setText(timeFormat.format(now));
            dateLabel.setText(dateFormat.format(now));
        }).start();
    }

    private void filterEmployees(String query) {
        String text = query.toLowerCase().trim();
        Employee previouslySelected = (Employee) employeeComboBox.getSelectedItem();
        employeeComboBox.removeAllItems();
        
        Employee newSelection = null;
        for (Employee emp : activeEmployees) {
            if (text.isEmpty() ||
                emp.getFullName().toLowerCase().contains(text) ||
                emp.getEmployeeCode().toLowerCase().contains(text)) {
                employeeComboBox.addItem(emp);
                if (previouslySelected != null && emp.getId() == previouslySelected.getId()) {
                    newSelection = emp;
                }
            }
        }
        
        if (newSelection != null) {
            employeeComboBox.setSelectedItem(newSelection);
        }
    }

    private void refreshActiveEmployees() {
        try {
            activeEmployees = kioskService.getAllActiveEmployees();
            filterEmployees(searchField.getText());
        } catch (SQLException e) {
            showStatus("Database error refreshing list: " + e.getMessage(), ACCENT_RED);
        }
    }

    private void triggerClockAction(boolean isClockIn) {
        statusLabel.setText(" ");
        Employee selectedEmployee = (Employee) employeeComboBox.getSelectedItem();
        String dailyCode = codeField.getText().trim();

        if (selectedEmployee == null) {
            showStatus("Please select your employee name from the dropdown!", ACCENT_RED);
            return;
        }

        if (dailyCode.isEmpty()) {
            showStatus("Please enter your daily verification code!", ACCENT_RED);
            return;
        }

        try {
            // 1. Validate daily code
            kioskService.validateEmployeeDailyCode(selectedEmployee.getId(), dailyCode);

            // 2. Process clock transaction
            String resultMsg = kioskService.processAttendance(selectedEmployee);

            // Display results nicely
            showStatus(resultMsg.replace("\n", " | "), ACCENT_GREEN);
            searchField.setText("");
            codeField.setText("");

        } catch (AppException e) {
            showStatus(e.getMessage(), ACCENT_RED);
        } catch (SQLException e) {
            showStatus("Database error: " + e.getMessage(), ACCENT_RED);
        }
    }

    private void triggerViewStatsAction() {
        statusLabel.setText(" ");
        Employee selectedEmployee = (Employee) employeeComboBox.getSelectedItem();
        String dailyCode = codeField.getText().trim();

        if (selectedEmployee == null) {
            showStatus("Please select your employee name from the dropdown!", ACCENT_RED);
            return;
        }

        if (dailyCode.isEmpty()) {
            showStatus("Please enter your daily verification code!", ACCENT_RED);
            return;
        }

        try {
            // 1. Validate daily code
            kioskService.validateEmployeeDailyCode(selectedEmployee.getId(), dailyCode);

            // 2. Fetch stats
            String statsText = kioskService.getEmployeeStats(selectedEmployee);

            // 3. Display stats popup with clean styled components
            JTextArea textArea = new JTextArea(statsText);
            textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            textArea.setBackground(new Color(23, 28, 41));
            textArea.setForeground(Color.WHITE);
            textArea.setEditable(false);
            textArea.setCaretColor(Color.WHITE);
            textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(650, 420));
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(55, 68, 92)));

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    selectedEmployee.getFullName() + " - Attendance Performance Stats",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (AppException e) {
            showStatus(e.getMessage(), ACCENT_RED);
        } catch (SQLException e) {
            showStatus("Database error: " + e.getMessage(), ACCENT_RED);
        }
    }

    private void showStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }
}
