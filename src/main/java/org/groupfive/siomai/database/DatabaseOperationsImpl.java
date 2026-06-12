package org.groupfive.siomai.database;

import org.groupfive.siomai.model.Admin;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseOperationsImpl implements DatabaseOperations {

    @Override
    public Admin getAdminByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM admins WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("password")
                    );
                }
            }
        }
        return null;
    }

    @Override
    public void addEmployee(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (employee_code, full_name, department, is_active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getEmployeeCode());
            ps.setString(2, employee.getFullName());
            ps.setString(3, employee.getDepartment());
            ps.setInt(4, employee.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    employee.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public List<Employee> getAllEmployees() throws SQLException {
        String sql = "SELECT * FROM employees";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Employee(
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getString("employee_code"),
                    rs.getString("department"),
                    rs.getInt("is_active") == 1
                ));
            }
        }
        return list;
    }

    @Override
    public Employee getEmployeeById(int id) throws SQLException {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Employee(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("employee_code"),
                        rs.getString("department"),
                        rs.getInt("is_active") == 1
                    );
                }
            }
        }
        return null;
    }

    @Override
    public Employee getEmployeeByCode(String code) throws SQLException {
        String sql = "SELECT * FROM employees WHERE employee_code = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Employee(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("employee_code"),
                        rs.getString("department"),
                        rs.getInt("is_active") == 1
                    );
                }
            }
        }
        return null;
    }

    @Override
    public List<Employee> searchEmployees(String query) throws SQLException {
        String sql = "SELECT * FROM employees WHERE full_name LIKE ? OR employee_code LIKE ?";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String likePattern = "%" + query + "%";
            ps.setString(1, likePattern);
            ps.setString(2, likePattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Employee(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("employee_code"),
                        rs.getString("department"),
                        rs.getInt("is_active") == 1
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public void updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees SET employee_code = ?, full_name = ?, department = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getEmployeeCode());
            ps.setString(2, employee.getFullName());
            ps.setString(3, employee.getDepartment());
            ps.setInt(4, employee.isActive() ? 1 : 0);
            ps.setInt(5, employee.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteEmployee(int id) throws SQLException {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public String getDailyCode(Date date) throws SQLException {
        String sql = "SELECT validation_code FROM daily_codes WHERE generated_date = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("validation_code");
                }
            }
        }
        return null;
    }

    @Override
    public void setDailyCode(String code, Date date) throws SQLException {
        // Multi-db safe check-and-insert/update
        String selectSql = "SELECT id FROM daily_codes WHERE generated_date = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
            psSelect.setString(1, date.toString());
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    String updateSql = "UPDATE daily_codes SET validation_code = ? WHERE generated_date = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                        psUpdate.setString(1, code);
                        psUpdate.setString(2, date.toString());
                        psUpdate.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO daily_codes (validation_code, generated_date) VALUES (?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                        psInsert.setString(1, code);
                        psInsert.setString(2, date.toString());
                        psInsert.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public AttendanceRecord getTodayAttendanceRecord(int employeeId, Date date) throws SQLException {
        String sql = "SELECT * FROM attendance_logs WHERE employee_id = ? AND log_date = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp clockOut = rs.getTimestamp("clock_out");
                    Timestamp clockIn = rs.getTimestamp("clock_in");
                    return new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("employee_id"),
                        clockIn,
                        clockOut,
                        rs.getDate("log_date"),
                        rs.getDouble("work_hours")
                    );
                }
            }
        }
        return null;
    }

    @Override
    public void addAttendanceRecord(AttendanceRecord record) throws SQLException {
        String sql = "INSERT INTO attendance_logs (employee_id, clock_in, clock_out, log_date, work_hours) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getEmployeeId());
            ps.setString(2, record.getClockIn() != null ? record.getClockIn().toString() : null);
            ps.setString(3, record.getClockOut() != null ? record.getClockOut().toString() : null);
            ps.setString(4, record.getLogDate() != null ? record.getLogDate().toString() : new Date(System.currentTimeMillis()).toString());
            ps.setDouble(5, record.getWorkHours());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateAttendanceRecord(AttendanceRecord record) throws SQLException {
        String sql = "UPDATE attendance_logs SET clock_out = ?, work_hours = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getClockOut() != null ? record.getClockOut().toString() : null);
            ps.setDouble(2, record.getWorkHours());
            ps.setInt(3, record.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<AttendanceRecord> getAttendanceLogsForDate(Date date) throws SQLException {
        String sql = "SELECT * FROM attendance_logs WHERE log_date = ?";
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp clockOut = rs.getTimestamp("clock_out");
                    Timestamp clockIn = rs.getTimestamp("clock_in");
                    list.add(new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("employee_id"),
                        clockIn,
                        clockOut,
                        rs.getDate("log_date"),
                        rs.getDouble("work_hours")
                    ));
                }
            }
        }
        return list;
    }
}
