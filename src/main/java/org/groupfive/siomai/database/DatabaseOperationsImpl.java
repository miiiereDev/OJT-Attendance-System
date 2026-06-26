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
            setDateHelper(ps, 1, date, conn);
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
            setDateHelper(psSelect, 1, date, conn);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    String updateSql = "UPDATE daily_codes SET validation_code = ? WHERE generated_date = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                        psUpdate.setString(1, code);
                        setDateHelper(psUpdate, 2, date, conn);
                        psUpdate.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO daily_codes (validation_code, generated_date) VALUES (?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                        psInsert.setString(1, code);
                        setDateHelper(psInsert, 2, date, conn);
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
            setDateHelper(ps, 2, date, conn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp clockOut = rs.getTimestamp("clock_out");
                    Timestamp clockIn = rs.getTimestamp("clock_in");
                    Date logDate = getLogDateHelper(rs, "log_date");
                    return new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("employee_id"),
                        clockIn,
                        clockOut,
                        logDate,
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
            setTimestampHelper(ps, 2, record.getClockIn(), conn);
            setTimestampHelper(ps, 3, record.getClockOut(), conn);
            setDateHelper(ps, 4, record.getLogDate() != null ? record.getLogDate() : new Date(System.currentTimeMillis()), conn);
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
            setTimestampHelper(ps, 1, record.getClockOut(), conn);
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
            setDateHelper(ps, 1, date, conn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp clockOut = rs.getTimestamp("clock_out");
                    Timestamp clockIn = rs.getTimestamp("clock_in");
                    Date logDate = getLogDateHelper(rs, "log_date");
                    list.add(new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("employee_id"),
                        clockIn,
                        clockOut,
                        logDate,
                        rs.getDouble("work_hours")
                    ));
                }
            }
        }
        return list;
    }
    @Override
    public String getEmployeeDailyCode(int employeeId, Date date) throws SQLException {
        String sql = "SELECT validation_code FROM employee_daily_codes WHERE employee_id = ? AND generated_date = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            setDateHelper(ps, 2, date, conn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("validation_code");
                }
            }
        }
        return null;
    }

    @Override
    public void setEmployeeDailyCode(int employeeId, String code, Date date) throws SQLException {
        String selectSql = "SELECT id FROM employee_daily_codes WHERE employee_id = ? AND generated_date = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
            psSelect.setInt(1, employeeId);
            setDateHelper(psSelect, 2, date, conn);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    String updateSql = "UPDATE employee_daily_codes SET validation_code = ? WHERE employee_id = ? AND generated_date = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                        psUpdate.setString(1, code);
                        psUpdate.setInt(2, employeeId);
                        setDateHelper(psUpdate, 3, date, conn);
                        psUpdate.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO employee_daily_codes (employee_id, validation_code, generated_date) VALUES (?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                        psInsert.setInt(1, employeeId);
                        psInsert.setString(2, code);
                        setDateHelper(psInsert, 3, date, conn);
                        psInsert.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public List<AttendanceRecord> getAttendanceLogsForEmployee(int employeeId) throws SQLException {
        String sql = "SELECT * FROM attendance_logs WHERE employee_id = ? ORDER BY log_date DESC, clock_in DESC";
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp clockOut = rs.getTimestamp("clock_out");
                    Timestamp clockIn = rs.getTimestamp("clock_in");
                    Date logDate = getLogDateHelper(rs, "log_date");
                    list.add(new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("employee_id"),
                        clockIn,
                        clockOut,
                        logDate,
                        rs.getDouble("work_hours")
                    ));
                }
            }
        }
        return list;
    }
    private Date getLogDateHelper(ResultSet rs, String columnName) throws SQLException {
        String dateStr = rs.getString(columnName);
        if (dateStr == null) {
            return null;
        }
        // If SQLite stores it with a time component (e.g. YYYY-MM-DD HH:MM:SS), truncate it
        if (dateStr.contains(" ")) {
            dateStr = dateStr.split(" ")[0];
        }
        try {
            return Date.valueOf(dateStr);
        } catch (IllegalArgumentException e) {
            return rs.getDate(columnName);
        }
    }

    private boolean isSQLite(Connection conn) throws SQLException {
        return conn.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite");
    }

    private void setDateHelper(PreparedStatement ps, int parameterIndex, Date date, Connection conn) throws SQLException {
        if (date == null) {
            ps.setNull(parameterIndex, java.sql.Types.DATE);
            return;
        }
        if (isSQLite(conn)) {
            // Store as ISO yyyy-MM-dd string format to match SQLite date functions
            ps.setString(parameterIndex, date.toString());
        } else {
            ps.setDate(parameterIndex, date);
        }
    }

    private void setTimestampHelper(PreparedStatement ps, int parameterIndex, Timestamp timestamp, Connection conn) throws SQLException {
        if (timestamp == null) {
            ps.setNull(parameterIndex, java.sql.Types.TIMESTAMP);
            return;
        }
        if (isSQLite(conn)) {
            // Store as ISO string format instead of millisecond bigint to prevent SQLite JDBC parser exceptions
            ps.setString(parameterIndex, timestamp.toString());
        } else {
            ps.setTimestamp(parameterIndex, timestamp);
        }
    }
}
