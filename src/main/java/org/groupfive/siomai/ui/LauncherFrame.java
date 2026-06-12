package org.groupfive.siomai.ui;

import org.groupfive.siomai.service.AdminService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

/**
 * Landing portal providing cards to choose between Kiosk Mode and Admin Panel.
 */
public class LauncherFrame extends JFrame {
    private static final Color BG_COLOR = new Color(26, 31, 44);       // Deep Slate Dark
    private static final Color CARD_BG = new Color(37, 44, 62);        // Slate Card
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(160, 174, 192);   // Muted gray
    private static final Color ACCENT_BLUE = new Color(49, 130, 206);   // Hover highlight
    private static final Color ACCENT_GREEN = new Color(56, 161, 105);

    private final AdminService adminService = new AdminService();

    public LauncherFrame() {
        setTitle("Attendance System Portal");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Root Panel
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_COLOR);
        root.setBorder(new EmptyBorder(40, 40, 40, 40));
        add(root);

        // Header Panel
        JPanel header = new JPanel(new GridLayout(2, 1, 5, 5));
        header.setOpaque(false);
        JLabel title = new JLabel("EMPLOYEE ATTENDANCE SYSTEM", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Select a portal workspace to continue", JLabel.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_MUTED);
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        // Grid selection cards
        JPanel selectionGrid = new JPanel(new GridLayout(1, 2, 30, 0));
        selectionGrid.setOpaque(false);
        selectionGrid.setBorder(new EmptyBorder(40, 10, 40, 10));

        // Card 1: Kiosk
        JPanel kioskCard = createSelectionCard(
                "Employee Kiosk Mode",
                "<html><center>Launch the full-screen terminal for employees to clock in and out using their employee IDs and daily codes.</center></html>",
                ACCENT_GREEN,
                () -> {
                    this.setVisible(false);
                    KioskFrame kiosk = new KioskFrame(this);
                    kiosk.setVisible(true);
                }
        );

        // Card 2: Admin
        JPanel adminCard = createSelectionCard(
                "Admin Control Panel",
                "<html><center>Log in to access administrative dashboards, update employee records, generate daily validation codes, and compile reports.</center></html>",
                ACCENT_BLUE,
                this::handleAdminLogin
        );

        selectionGrid.add(kioskCard);
        selectionGrid.add(adminCard);
        root.add(selectionGrid, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("System Version 1.0.0 | Developed by Group 5", JLabel.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(new Color(113, 128, 150));
        root.add(footer, BorderLayout.SOUTH);
    }

    private JPanel createSelectionCard(String title, String desc, Color hoverColor, Runnable onClickAction) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(74, 85, 104), 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel cardTitle = new JLabel(title);
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        cardTitle.setForeground(TEXT_PRIMARY);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cardDesc = new JLabel(desc);
        cardDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardDesc.setForeground(TEXT_MUTED);
        cardDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(cardTitle);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(cardDesc);
        card.add(Box.createVerticalGlue());

        // Hover Effect Adapter
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_BG.brighter());
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(hoverColor, 2, true),
                        new EmptyBorder(24, 24, 24, 24)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BG);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(74, 85, 104), 1, true),
                        new EmptyBorder(25, 25, 25, 25)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                onClickAction.run();
            }
        });

        return card;
    }

    private void handleAdminLogin() {
        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        loginPanel.setBackground(CARD_BG);
        loginPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(TEXT_PRIMARY);
        JTextField usernameField = new JTextField();
        usernameField.setBackground(BG_COLOR);
        usernameField.setForeground(TEXT_PRIMARY);
        usernameField.setCaretColor(TEXT_PRIMARY);
        usernameField.setBorder(BorderFactory.createLineBorder(new Color(74, 85, 104)));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(TEXT_PRIMARY);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBackground(BG_COLOR);
        passwordField.setForeground(TEXT_PRIMARY);
        passwordField.setCaretColor(TEXT_PRIMARY);
        passwordField.setBorder(BorderFactory.createLineBorder(new Color(74, 85, 104)));

        loginPanel.add(userLabel);
        loginPanel.add(usernameField);
        loginPanel.add(passLabel);
        loginPanel.add(passwordField);

        // Customize JOptionPane dialog colors
        UIManager.put("OptionPane.background", CARD_BG);
        UIManager.put("Panel.background", CARD_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        int result = JOptionPane.showConfirmDialog(
                this,
                loginPanel,
                "Admin Authentication",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                if (adminService.authenticate(username, password)) {
                    this.setVisible(false);
                    AdminFrame adminFrame = new AdminFrame(this);
                    adminFrame.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials!", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
