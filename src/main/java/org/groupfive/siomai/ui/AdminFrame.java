package org.groupfive.siomai.ui;

import org.groupfive.siomai.database.DatabaseOperationsImpl;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.service.AdminService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Admin Portal Dashboard Frame featuring side navigation tabs and CRUD tables.
 */
public class AdminFrame extends JFrame {
    private static final Color BG_DARK = new Color(18, 22, 33);
    private static final Color SIDEBAR_BG = new Color(26, 31, 44);
    private static final Color CARD_BG = new Color(30, 37, 53);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(160, 174, 192);
    private static final Color ACCENT_BLUE = new Color(49, 130, 206);
    private static final Color ACCENT_RED = new Color(231, 76, 60);

    private final JFrame parent;
    private final AdminService adminService = new AdminService();

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private DefaultTableModel employeeModel;
    private JTable employeeTable;
    private DefaultTableModel dailyCodeModel;
    private JTable dailyCodeTable;
    private JTextArea reportTextArea;

    public AdminFrame(JFrame parent) {
        this.parent = parent;
        setTitle("Admin Control Portal");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_DARK);
        add(mainPanel);

        // Sidebar Navigation
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 600));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(25, 20, 25, 20));
        mainPanel.add(sidebar, BorderLayout.WEST);

        JLabel sidebarTitle = new JLabel("ADMIN PORTAL");
        sidebarTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sidebarTitle.setForeground(TEXT_PRIMARY);
        sidebarTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sidebarSub = new JLabel("Control Center");
        sidebarSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sidebarSub.setForeground(TEXT_MUTED);
        sidebarSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(sidebarTitle);
        sidebar.add(sidebarSub);
        sidebar.add(Box.createRigidArea(new Dimension(0, 40)));

        // Navigation buttons
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JButton btnDirectory = createSidebarButton("Employee Directory");
        btnDirectory.addActionListener(e -> cardLayout.show(contentPanel, "directory"));

        JButton btnCodeCenter = createSidebarButton("Daily Code Center");
        btnCodeCenter.addActionListener(e -> {
            refreshDailyCode();
            cardLayout.show(contentPanel, "codecenter");
        });

        JButton btnReports = createSidebarButton("Work Hours Report");
        btnReports.addActionListener(e -> {
            refreshReport();
            cardLayout.show(contentPanel, "reports");
        });

        JButton btnLogout = createSidebarButton("Logout Portal");
        btnLogout.setBackground(new Color(45, 55, 72));
        btnLogout.addActionListener(e -> {
            this.dispose();
            parent.setVisible(true);
        });

        sidebar.add(btnDirectory);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnCodeCenter);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnReports);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnLogout);

        // Staging cards
        initDirectoryCard();
        initCodeCenterCard();
        initReportsCard();

        // Refresh initially
        refreshEmployeeTable();
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(CARD_BG);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setBorder(new EmptyBorder(10, 10, 10, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(CARD_BG.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(CARD_BG);
            }
        });

        return btn;
    }

    private void initDirectoryCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header search
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel("Employee Registry Directory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        topPanel.add(title, BorderLayout.WEST);

        // Search Field
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Fuzzy Filter: "));
        JTextField searchField = new JTextField(15);
        searchField.setBackground(CARD_BG);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createLineBorder(new Color(74, 85, 104)));
        searchField.addActionListener(e -> filterEmployees(searchField.getText()));
        JButton filterBtn = new JButton("Filter");
        filterBtn.addActionListener(e -> filterEmployees(searchField.getText()));
        searchBar.add(searchField);
        searchBar.add(filterBtn);
        topPanel.add(searchBar, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        employeeModel = new DefaultTableModel(new String[]{"ID", "Employee Code", "Full Name", "Department", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeModel);
        styleTable(employeeTable);
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control CRUD Buttons
        JPanel crudPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        crudPanel.setOpaque(false);
        crudPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JButton addBtn = createActionButton("Add Employee", ACCENT_BLUE);
        addBtn.addActionListener(e -> showAddEmployeeDialog());

        JButton editBtn = createActionButton("Edit Selected", ACCENT_BLUE);
        editBtn.addActionListener(e -> showEditEmployeeDialog());

        JButton deleteBtn = createActionButton("Delete Selected", ACCENT_RED);
        deleteBtn.addActionListener(e -> showDeleteEmployeeDialog());

        crudPanel.add(addBtn);
        crudPanel.add(editBtn);
        crudPanel.add(deleteBtn);
        panel.add(crudPanel, BorderLayout.SOUTH);

        contentPanel.add(panel, "directory");
    }

    private void styleTable(JTable table) {
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(new Color(45, 55, 72));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(25);
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setBackground(SIDEBAR_BG);
        header.setForeground(TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
        table.getColumnModel().getColumn(4).setCellRenderer(renderer);
    }

    private JButton createActionButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
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

    private void initCodeCenterCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("Daily Code Center");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        dailyCodeModel = new DefaultTableModel(new String[]{"ID", "Employee Code", "Full Name", "Department", "Daily Code"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        dailyCodeTable = new JTable(dailyCodeModel);
        styleTable(dailyCodeTable);

        // Customize the "Daily Code" column cell renderer to make it stand out
        dailyCodeTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Monospaced", Font.BOLD, 14));
                if (!isSelected) {
                    setForeground(ACCENT_BLUE);
                } else {
                    setForeground(TEXT_PRIMARY);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(dailyCodeTable);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JButton resetBtn = createActionButton("Reset Selected Employee Code", ACCENT_BLUE);
        resetBtn.addActionListener(e -> {
            int selectedRow = dailyCodeTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an employee from the table first!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int employeeId = (int) dailyCodeTable.getValueAt(selectedRow, 0);
            String empName = (String) dailyCodeTable.getValueAt(selectedRow, 2);
            try {
                String newCode = adminService.resetEmployeeCode(employeeId);
                refreshDailyCode();
                JOptionPane.showMessageDialog(this, "Code successfully reset/regenerated for " + empName + ": " + newCode);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error resetting code: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton refreshBtn = createActionButton("Refresh List", CARD_BG);
        refreshBtn.addActionListener(e -> refreshDailyCode());

        controlPanel.add(resetBtn);
        controlPanel.add(refreshBtn);
        panel.add(controlPanel, BorderLayout.SOUTH);

        contentPanel.add(panel, "codecenter");
    }

    private void refreshDailyCode() {
        try {
            dailyCodeModel.setRowCount(0);
            List<Employee> list = adminService.getAllEmployees();
            for (Employee e : list) {
                // Only show codes for active employees
                if (e.isActive()) {
                    String code = adminService.getEmployeeCodeForToday(e.getId());
                    dailyCodeModel.addRow(new Object[]{
                            e.getId(),
                            e.getEmployeeCode(),
                            e.getFullName(),
                            e.getDepartment(),
                            code
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error listing daily codes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initReportsCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("Employee Work Hours Summary Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        reportTextArea = new JTextArea();
        reportTextArea.setBackground(CARD_BG);
        reportTextArea.setForeground(TEXT_PRIMARY);
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportTextArea.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(reportTextArea);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));
        panel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(panel, "reports");
    }

    private void refreshReport() {
        try {
            String report = adminService.generateWorkHoursReport();
            reportTextArea.setText(report);
        } catch (SQLException e) {
            reportTextArea.setText("Error loading report: " + e.getMessage());
        }
    }

    private void refreshEmployeeTable() {
        try {
            List<Employee> list = adminService.getAllEmployees();
            updateTableData(list);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error listing employees: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterEmployees(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshEmployeeTable();
            return;
        }
        try {
            List<Employee> list = new DatabaseOperationsImpl().searchEmployees(query.trim());
            updateTableData(list);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTableData(List<Employee> list) {
        employeeModel.setRowCount(0);
        for (Employee e : list) {
            employeeModel.addRow(new Object[]{
                    e.getId(),
                    e.getEmployeeCode(),
                    e.getFullName(),
                    e.getDepartment(),
                    e.isActive() ? "Active" : "Inactive"
            });
        }
    }

    private void showAddEmployeeDialog() {
        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField deptField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBackground(CARD_BG);
        panel.add(new JLabel("Employee Code (e.g. EMP-1001):"));
        panel.add(codeField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Department:"));
        panel.add(deptField);

        int res = JOptionPane.showConfirmDialog(this, panel, "Add New Employee", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                adminService.addEmployee(codeField.getText(), nameField.getText(), deptField.getText());
                refreshEmployeeTable();
                JOptionPane.showMessageDialog(this, "Employee successfully added!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void showEditEmployeeDialog() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee from the table first!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) employeeTable.getValueAt(selectedRow, 0);
        String code = (String) employeeTable.getValueAt(selectedRow, 1);
        String name = (String) employeeTable.getValueAt(selectedRow, 2);
        String dept = (String) employeeTable.getValueAt(selectedRow, 3);
        boolean isActive = employeeTable.getValueAt(selectedRow, 4).equals("Active");

        JTextField codeField = new JTextField(code);
        JTextField nameField = new JTextField(name);
        JTextField deptField = new JTextField(dept);
        JCheckBox activeBox = new JCheckBox("Active Account", isActive);
        activeBox.setForeground(TEXT_PRIMARY);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBackground(CARD_BG);
        panel.add(new JLabel("Employee Code:"));
        panel.add(codeField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Department:"));
        panel.add(deptField);
        panel.add(new JLabel("Account Status:"));
        panel.add(activeBox);

        int res = JOptionPane.showConfirmDialog(this, panel, "Update Employee details", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                adminService.updateEmployee(id, codeField.getText(), nameField.getText(), deptField.getText(), activeBox.isSelected());
                refreshEmployeeTable();
                JOptionPane.showMessageDialog(this, "Employee successfully updated!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDeleteEmployeeDialog() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee from the table first!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) employeeTable.getValueAt(selectedRow, 0);
        String code = (String) employeeTable.getValueAt(selectedRow, 1);
        String name = (String) employeeTable.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete employee:\n" + code + " - " + name + "?\nThis operation cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                adminService.deleteEmployee(id);
                refreshEmployeeTable();
                JOptionPane.showMessageDialog(this, "Employee deleted successfully.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
