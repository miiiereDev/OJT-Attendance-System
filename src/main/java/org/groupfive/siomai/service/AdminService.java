package org.groupfive.siomai.service;

import org.groupfive.siomai.database.DatabaseOperations;
import org.groupfive.siomai.database.DatabaseOperationsImpl;
import org.groupfive.siomai.database.DatabaseConnector;
import org.groupfive.siomai.model.Admin;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

/**
 * Service class encapsulating Admin Module operations (CRUD, Auth, Reports, Code Generation).
 */
public class AdminService {
    private final DatabaseOperations dbOps;
    private final Random random = new Random();

    public AdminService() {
        this.dbOps = new DatabaseOperationsImpl();
    }

    public AdminService(DatabaseOperations dbOps) {
        this.dbOps = dbOps;
    }

    /**
     * Authenticates administrative login credentials.
     *
     * @param username Enter username
     * @param password Enter password
     * @return true if credentials are valid, false otherwise
     * @throws SQLException if database access fails
     */
    public boolean authenticate(String username, String password) throws SQLException {
        if (username == null || password == null || username.trim().isEmpty()) {
            return false;
        }
        Admin admin = dbOps.getAdminByUsername(username.trim());
        return admin != null && admin.getPassword().equals(password);
    }

    /**
     * Adds a new employee to the system with inputs validation.
     */
    public void addEmployee(String code, String name, String department) throws SQLException, IllegalArgumentException {
        validateEmployeeFields(code, name, department);
        
        // Check if code is already registered
        if (dbOps.getEmployeeByCode(code.trim()) != null) {
            throw new IllegalArgumentException("Employee Code '" + code + "' is already registered!");
        }

        Employee emp = new Employee(0, name.trim(), code.trim(), department.trim(), true);
        dbOps.addEmployee(emp);
    }

    /**
     * Updates an existing employee.
     */
    public void updateEmployee(int id, String code, String name, String department, boolean isActive) throws SQLException, IllegalArgumentException {
        validateEmployeeFields(code, name, department);

        Employee existing = dbOps.getEmployeeById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Employee not found!");
        }

        // If code changes, ensure new code is not taken by another employee
        Employee dup = dbOps.getEmployeeByCode(code.trim());
        if (dup != null && dup.getId() != id) {
            throw new IllegalArgumentException("Employee Code '" + code + "' is already in use by another employee!");
        }

        existing.setEmployeeCode(code.trim());
        existing.setFullName(name.trim());
        existing.setDepartment(department.trim());
        existing.setActive(isActive);
        dbOps.updateEmployee(existing);
    }

    /**
     * Deletes an employee from the system.
     */
    public void deleteEmployee(int id) throws SQLException {
        dbOps.deleteEmployee(id);
    }

    /**
     * Obtains the daily verification code for a specific employee today.
     * Auto-generates one if it doesn't exist yet.
     */
    public String getEmployeeCodeForToday(int employeeId) throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        String code = dbOps.getEmployeeDailyCode(employeeId, today);
        if (code == null) {
            int codeInt = 10000 + random.nextInt(90000);
            code = String.valueOf(codeInt);
            dbOps.setEmployeeDailyCode(employeeId, code, today);
        }
        return code;
    }

    /**
     * Resets/regenerates a daily code for a specific employee today.
     */
    public String resetEmployeeCode(int employeeId) throws SQLException {
        int codeInt = 10000 + random.nextInt(90000);
        String code = String.valueOf(codeInt);
        Date today = new Date(System.currentTimeMillis());
        dbOps.setEmployeeDailyCode(employeeId, code, today);
        return code;
    }

    /**
     * Lists all registered employees.
     */
    public List<Employee> getAllEmployees() throws SQLException {
        return dbOps.getAllEmployees();
    }



    /**
     * Generates a formatted work hours report.
     *
     * @return Report text
     * @throws SQLException if database access fails
     */
    public String generateWorkHoursReport() throws SQLException {
        List<Employee> employees = dbOps.getAllEmployees();
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("%-12s | %-22s | %-15s | %-15s\n", "CODE", "NAME", "DEPARTMENT", "TOTAL WORK HOURS"));
        sb.append("---------------------------------------------------------------------------------\n");

        try (java.sql.Connection conn = DatabaseConnector.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT SUM(work_hours) AS total_hrs FROM attendance_logs WHERE employee_id = ?")) {
            for (Employee emp : employees) {
                ps.setInt(1, emp.getId());
                double hours = 0.0;
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hours = rs.getDouble("total_hrs");
                    }
                }
                sb.append(String.format("%-12s | %-22s | %-15s | %.2f hours\n",
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getDepartment(),
                        hours));
            }
        }
        return sb.toString();
    }

    private void validateEmployeeFields(String code, String name, String department) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee Code cannot be empty!");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee Full Name cannot be empty!");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be empty!");
        }
    }
}
