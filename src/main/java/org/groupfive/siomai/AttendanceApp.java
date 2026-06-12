package org.groupfive.siomai;

import org.groupfive.siomai.exception.AppException;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.service.AdminService;
import org.groupfive.siomai.service.KioskService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Main Application class driving the user interface using JOptionPane menus.
 */
public class AttendanceApp {
    private static final KioskService kioskService = new KioskService();
    private static final AdminService adminService = new AdminService();

    public static void main(String[] args) {
        // Set standard system look and feel for premium aesthetics
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        while (true) {
            String[] options = {"Employee Kiosk", "Admin Panel", "Exit"};
            int selection = JOptionPane.showOptionDialog(
                    null,
                    "Welcome to the Attendance Monitoring Kiosk\n\nPlease select your module:",
                    "Main Portal",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (selection == 0) {
                runEmployeeKiosk();
            } else if (selection == 1) {
                runAdminPanel();
            } else {
                break;
            }
        }
    }

    private static void runEmployeeKiosk() {
        try {
            String query = JOptionPane.showInputDialog(
                    null,
                    "Enter your Employee Name or Code:",
                    "Kiosk Lookup",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (query == null || query.trim().isEmpty()) {
                return; // User cancelled or entered empty string
            }

            // Search matching active employees
            List<Employee> matches = kioskService.searchActiveEmployees(query);
            Employee selectedEmployee;

            if (matches.size() == 1) {
                selectedEmployee = matches.get(0);
            } else {
                // Let user select if multiple matches
                Object selection = JOptionPane.showInputDialog(
                        null,
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

            // Prompt daily code
            String dailyCode = JOptionPane.showInputDialog(
                    null,
                    "Enter today's 5-digit Verification Code:",
                    "Daily Code Verification",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (dailyCode == null) {
                return;
            }

            // Validate code
            kioskService.validateDailyCode(dailyCode);

            // Process clock in / out
            String summary = kioskService.processAttendance(selectedEmployee);
            JOptionPane.showMessageDialog(null, summary, "Kiosk Transaction Result", JOptionPane.INFORMATION_MESSAGE);

        } catch (AppException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Kiosk Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void runAdminPanel() {
        // Custom login panel to mask administrative password inputs
        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
                null,
                loginPanel,
                "Admin Authentication",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            if (!adminService.authenticate(username, password)) {
                JOptionPane.showMessageDialog(null, "Invalid Admin Credentials!", "Access Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Admin panel loop
            while (true) {
                String[] adminOptions = {
                        "1. Add Employee",
                        "2. View Employees",
                        "3. Search Employee",
                        "4. Update Employee Details",
                        "5. Delete Employee",
                        "6. Generate Daily Verification Code",
                        "7. Generate Work Hours Report",
                        "8. Logout"
                };

                Object selection = JOptionPane.showInputDialog(
                        null,
                        "Admin Control Panel\n\nChoose an option:",
                        "Admin Menu",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        adminOptions,
                        adminOptions[0]
                );

                if (selection == null || selection.equals("8. Logout")) {
                    break;
                }

                String optionStr = (String) selection;
                if (optionStr.startsWith("1.")) {
                    addEmployeeDialog();
                } else if (optionStr.startsWith("2.")) {
                    viewEmployeesDialog();
                } else if (optionStr.startsWith("3.")) {
                    searchEmployeesDialog();
                } else if (optionStr.startsWith("4.")) {
                    updateEmployeeDialog();
                } else if (optionStr.startsWith("5.")) {
                    deleteEmployeeDialog();
                } else if (optionStr.startsWith("6.")) {
                    generateDailyCodeDialog();
                } else if (optionStr.startsWith("7.")) {
                    generateReportDialog();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void addEmployeeDialog() {
        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField deptField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Employee Code (e.g. EMP-1001):"));
        panel.add(codeField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Department:"));
        panel.add(deptField);

        int res = JOptionPane.showConfirmDialog(null, panel, "Add New Employee", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                adminService.addEmployee(codeField.getText(), nameField.getText(), deptField.getText());
                JOptionPane.showMessageDialog(null, "Employee successfully added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static void viewEmployeesDialog() throws SQLException {
        List<Employee> employees = adminService.getAllEmployees();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s | %-25s | %-15s | %-10s\n", "CODE", "NAME", "DEPARTMENT", "STATUS"));
        sb.append("-------------------------------------------------------------------------\n");
        for (Employee e : employees) {
            sb.append(String.format("%-12s | %-25s | %-15s | %-10s\n",
                    e.getEmployeeCode(), e.getFullName(), e.getDepartment(), e.isActive() ? "Active" : "Inactive"));
        }

        showScrollableMessage(sb.toString(), "Registered Employees Directory");
    }

    private static void searchEmployeesDialog() throws SQLException {
        String query = JOptionPane.showInputDialog(null, "Enter Name or Code to search:");
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        List<Employee> matches;
        try {
            matches = kioskService.searchActiveEmployees(query);
        } catch (AppException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Not Found", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s | %-25s | %-15s\n", "CODE", "NAME", "DEPARTMENT"));
        sb.append("------------------------------------------------------------\n");
        for (Employee e : matches) {
            sb.append(String.format("%-12s | %-25s | %-15s\n",
                    e.getEmployeeCode(), e.getFullName(), e.getDepartment()));
        }

        showScrollableMessage(sb.toString(), "Search Results");
    }

    private static void updateEmployeeDialog() throws SQLException {
        String code = JOptionPane.showInputDialog(null, "Enter Employee Code to update:");
        if (code == null || code.trim().isEmpty()) {
            return;
        }

        Employee emp = new DatabaseOperationsImpl().getEmployeeByCode(code.trim());

        if (emp == null) {
            JOptionPane.showMessageDialog(null, "Employee not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField codeField = new JTextField(emp.getEmployeeCode());
        JTextField nameField = new JTextField(emp.getFullName());
        JTextField deptField = new JTextField(emp.getDepartment());
        JCheckBox activeBox = new JCheckBox("Active Status", emp.isActive());

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Employee Code:"));
        panel.add(codeField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Department:"));
        panel.add(deptField);
        panel.add(new JLabel("Account Status:"));
        panel.add(activeBox);

        int res = JOptionPane.showConfirmDialog(null, panel, "Update Employee details", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                adminService.updateEmployee(emp.getId(), codeField.getText(), nameField.getText(), deptField.getText(), activeBox.isSelected());
                JOptionPane.showMessageDialog(null, "Employee successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void deleteEmployeeDialog() throws SQLException {
        String code = JOptionPane.showInputDialog(null, "Enter Employee Code to delete:");
        if (code == null || code.trim().isEmpty()) {
            return;
        }

        Employee emp = new DatabaseOperationsImpl().getEmployeeByCode(code.trim());
        if (emp == null) {
            JOptionPane.showMessageDialog(null, "Employee not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                null,
                "Are you sure you want to delete employee:\n" + emp.getEmployeeCode() + " - " + emp.getFullName() + "?\nThis operation cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            adminService.deleteEmployee(emp.getId());
            JOptionPane.showMessageDialog(null, "Employee deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void generateDailyCodeDialog() throws SQLException {
        String code = adminService.generateDailyCode();
        JOptionPane.showMessageDialog(
                null,
                "Daily Verification Code generated successfully!\n\nCode: " + code + "\n\nGive this to employees for clocking in today.",
                "Daily Code Generator",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void generateReportDialog() throws SQLException {
        String report = adminService.generateWorkHoursReport();
        showScrollableMessage(report, "Employee Work Hours Summary Report");
    }

    private static void showScrollableMessage(String text, String title) {
        JTextArea textArea = new JTextArea(15, 60);
        textArea.setText(text);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(null, scrollPane, title, JOptionPane.PLAIN_MESSAGE);
    }
}
