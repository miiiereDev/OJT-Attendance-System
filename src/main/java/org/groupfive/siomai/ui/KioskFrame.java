package org.groupfive.siomai.ui;

import org.groupfive.siomai.exception.AppException;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.service.KioskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
    private static final Color BUTTON_BG = new Color(44, 62, 80);         // Back button

    private final JFrame parent;
    private final KioskService kioskService = new KioskService();

    private JTextField searchField;
    private JTextField codeField;
    private JLabel statusLabel;

    public KioskFrame(JFrame parent) {
        this.parent = parent;
        setTitle("Employee Attendance Kiosk");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_COLOR);

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
        headerPanel.add(clockPanel, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Run Live Clock timer
        runClock(timeLabel, dateLabel);

        // Center card container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JPanel kioskCard = new JPanel();
        kioskCard.setLayout(new BoxLayout(kioskCard, BoxLayout.Y_AXIS));
        kioskCard.setBackground(CARD_BG);
        kioskCard.setPreferredSize(new Dimension(500, 420));
        kioskCard.setMaximumSize(new Dimension(500, 420));
        kioskCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(45, 55, 72), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        JLabel cardTitle = new JLabel("EMPLOYEE SIGN-IN");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        cardTitle.setForeground(TEXT_PRIMARY);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cardSub = new JLabel("Enter your details to log attendance logs");
        cardSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardSub.setForeground(TEXT_MUTED);
        cardSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        kioskCard.add(cardTitle);
        kioskCard.add(Box.createRigidArea(new Dimension(0, 5)));
        kioskCard.add(cardSub);
        kioskCard.add(Box.createRigidArea(new Dimension(0, 30)));

        // Inputs Panel
        JPanel inputsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        inputsPanel.setOpaque(false);
        inputsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel searchLabel = new JLabel("Employee Name or Code:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(TEXT_MUTED);

        searchField = new JTextField();
        styleTextField(searchField);

        JLabel codeLabel = new JLabel("Daily Verification Code:");
        codeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        codeLabel.setForeground(TEXT_MUTED);

        codeField = new JTextField();
        styleTextField(codeField);

        inputsPanel.add(searchLabel);
        inputsPanel.add(searchField);
        inputsPanel.add(codeLabel);
        inputsPanel.add(codeField);
        kioskCard.add(inputsPanel);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonsPanel.setOpaque(false);
        buttonsPanel.setBorder(new EmptyBorder(25, 0, 15, 0));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton clockInBtn = createButton("CLOCK IN", ACCENT_GREEN);
        clockInBtn.addActionListener(e -> triggerClockAction(true));

        JButton clockOutBtn = createButton("CLOCK OUT", ACCENT_RED);
        clockOutBtn.addActionListener(e -> triggerClockAction(false));

        buttonsPanel.add(clockInBtn);
        buttonsPanel.add(clockOutBtn);
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

    private void triggerClockAction(boolean isClockIn) {
        statusLabel.setText(" ");
        String searchInput = searchField.getText();
        String dailyCode = codeField.getText();

        if (searchInput.trim().isEmpty() || dailyCode.trim().isEmpty()) {
            showStatus("Please fill in both fields!", ACCENT_RED);
            return;
        }

        try {
            // 1. Search active employees
            List<Employee> matches = kioskService.searchActiveEmployees(searchInput);
            Employee selectedEmployee;

            if (matches.size() == 1) {
                selectedEmployee = matches.get(0);
            } else {
                // Prompt dropdown selection
                Object selection = JOptionPane.showInputDialog(
                        this,
                        "Multiple employees matched. Please select yours:",
                        "Select Employee",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        matches.stream().map(e -> e.getEmployeeCode() + " - " + e.getFullName()).toArray(),
                        null
                );

                if (selection == null) {
                    return; // Cancelled
                }

                String selectedStr = (String) selection;
                String selectedCode = selectedStr.split(" - ")[0];
                selectedEmployee = matches.stream()
                        .filter(e -> e.getEmployeeCode().equals(selectedCode))
                        .findFirst()
                        .orElse(null);
            }

            if (selectedEmployee == null) {
                return;
            }

            // 2. Validate daily code
            kioskService.validateDailyCode(dailyCode);

            // 3. Process clock transaction
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

    private void showStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }
}
