package org.groupfive.siomai.database;

import org.groupfive.siomai.model.Admin;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface declaring abstract CRUD operations.
 * Showcases Abstraction.
 */
public interface DatabaseOperations {
    
    // --- Admin Operations ---
    Admin getAdminByUsername(String username) throws SQLException;
    
    // --- Employee Operations ---
    void addEmployee(Employee employee) throws SQLException;
    List<Employee> getAllEmployees() throws SQLException;
    Employee getEmployeeById(int id) throws SQLException;
    Employee getEmployeeByCode(String code) throws SQLException;
    List<Employee> searchEmployees(String query) throws SQLException;
    void updateEmployee(Employee employee) throws SQLException;
    void deleteEmployee(int id) throws SQLException;
    
    // --- Daily Codes ---
    String getDailyCode(Date date) throws SQLException;
    void setDailyCode(String code, Date date) throws SQLException;

    // --- Per-Employee Daily Codes ---
    String getEmployeeDailyCode(int employeeId, Date date) throws SQLException;
    void setEmployeeDailyCode(int employeeId, String code, Date date) throws SQLException;
    List<AttendanceRecord> getAttendanceLogsForEmployee(int employeeId) throws SQLException;
    
    // --- Attendance Logs ---
    AttendanceRecord getTodayAttendanceRecord(int employeeId, Date date) throws SQLException;
    void addAttendanceRecord(AttendanceRecord record) throws SQLException;
    void updateAttendanceRecord(AttendanceRecord record) throws SQLException;
    List<AttendanceRecord> getAttendanceLogsForDate(Date date) throws SQLException;
}
