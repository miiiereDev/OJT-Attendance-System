package org.groupfive.siomai.service;

import org.groupfive.siomai.database.DatabaseOperations;
import org.groupfive.siomai.database.DatabaseOperationsImpl;
import org.groupfive.siomai.exception.EmployeeNotFoundException;
import org.groupfive.siomai.exception.InvalidDailyCodeException;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service class encapsulating the Employee Kiosk transaction flows.
 */
public class KioskService {
    private final DatabaseOperations dbOps;

    public KioskService() {
        this.dbOps = new DatabaseOperationsImpl();
    }

    public KioskService(DatabaseOperations dbOps) {
        this.dbOps = dbOps;
    }

    /**
     * Searches for active employees matching a keyword or exact code.
     *
     * @param query Search query
     * @return List of matching active employees
     * @throws EmployeeNotFoundException if no active employee matches
     * @throws SQLException              if database access fails
     */
    public List<Employee> searchActiveEmployees(String query) throws EmployeeNotFoundException, SQLException {
        if (query == null || query.trim().isEmpty()) {
            throw new EmployeeNotFoundException("Search query cannot be empty");
        }

        // Try exact code matching first
        Employee exactMatch = dbOps.getEmployeeByCode(query.trim());
        if (exactMatch != null && exactMatch.isActive()) {
            return List.of(exactMatch);
        }

        // Otherwise fuzzy name/code search
        List<Employee> results = dbOps.searchEmployees(query.trim());
        List<Employee> activeResults = results.stream()
                .filter(Employee::isActive)
                .toList();

        if (activeResults.isEmpty()) {
            throw new EmployeeNotFoundException("No active employee found matching: " + query);
        }

        return activeResults;
    }

    /**
     * Validates the daily code for the current date.
     *
     * @param inputCode Entered code
     * @throws InvalidDailyCodeException if the code is invalid or not generated yet
     * @throws SQLException              if database access fails
     */
    public void validateDailyCode(String inputCode) throws InvalidDailyCodeException, SQLException {
        Date today = new Date(System.currentTimeMillis());
        String correctCode = dbOps.getDailyCode(today);

        if (correctCode == null) {
            throw new InvalidDailyCodeException("Today's verification code has not been generated yet. Please contact an Administrator.");
        }

        if (inputCode == null || !correctCode.equals(inputCode.trim())) {
            throw new InvalidDailyCodeException("Invalid Daily Verification Code!");
        }
    }

    /**
     * Processes clock-in or clock-out for a given employee.
     *
     * @param employee The employee performing the transaction
     * @return Transaction summary message
     * @throws SQLException if database access fails
     */
    public String processAttendance(Employee employee) throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        AttendanceRecord record = dbOps.getTodayAttendanceRecord(employee.getId(), today);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (record == null) {
            // Case 1: No log exists today -> Clock-In
            record = new AttendanceRecord();
            record.setEmployeeId(employee.getId());
            record.setClockIn(now);
            record.setLogDate(today);
            record.setWorkHours(0.0);

            dbOps.addAttendanceRecord(record);
            return String.format("Clock-in Successful!\nEmployee: %s\nTime: %s", employee.getFullName(), now);
        } else if (record.getClockOut() == null) {
            // Case 2: Clock-in exists, but no clock-out -> Clock-Out
            record.setClockOut(now);
            dbOps.updateAttendanceRecord(record);
            return String.format("Clock-out Successful!\nEmployee: %s\nTime: %s\nShift Work Hours: %.2f hours",
                    employee.getFullName(), now, record.getWorkHours());
        } else {
            // Case 3: Both exist -> Shift completed
            return String.format("You have already completed your shift for today!\nEmployee: %s\nTotal Hours logged: %.2f hours",
                    employee.getFullName(), record.getWorkHours());
        }
    }
}
