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
     * Retrieves all active employees in the system.
     *
     * @return List of active employees
     * @throws SQLException if database access fails
     */
    public List<Employee> getAllActiveEmployees() throws SQLException {
        List<Employee> results = dbOps.getAllEmployees();
        return results.stream()
                .filter(Employee::isActive)
                .toList();
    }

    /**
     * Obtains or auto-generates the daily code for a specific employee.
     */
    public String getOrGenerateEmployeeCode(int employeeId) throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        String code = dbOps.getEmployeeDailyCode(employeeId, today);
        if (code == null) {
            // Auto-generate a random 5-digit code
            int randVal = 10000 + new java.util.Random().nextInt(90000);
            code = String.valueOf(randVal);
            dbOps.setEmployeeDailyCode(employeeId, code, today);
        }
        return code;
    }

    /**
     * Validates the daily code for a specific employee.
     *
     * @param employeeId The employee ID
     * @param inputCode  Entered code
     * @throws InvalidDailyCodeException if the code is invalid or not generated yet
     * @throws SQLException              if database access fails
     */
    public void validateEmployeeDailyCode(int employeeId, String inputCode) throws InvalidDailyCodeException, SQLException {
        String correctCode = getOrGenerateEmployeeCode(employeeId);
        if (inputCode == null || !correctCode.equals(inputCode.trim())) {
            throw new InvalidDailyCodeException("Invalid Daily Verification Code!");
        }
    }

    /**
     * Compiles attendance history and metrics for an employee.
     */
    public String getEmployeeStats(Employee employee) throws SQLException {
        List<AttendanceRecord> records = dbOps.getAttendanceLogsForEmployee(employee.getId());
        
        int totalShifts = records.size();
        double totalHours = 0.0;
        int completedShifts = 0;
        
        StringBuilder history = new StringBuilder();
        history.append(String.format("%-12s | %-22s | %-22s | %-12s\n", "DATE", "CLOCK IN", "CLOCK OUT", "HOURS"));
        history.append("---------------------------------------------------------------------------------\n");
        
        for (AttendanceRecord r : records) {
            String clockInStr = r.getClockIn() != null ? r.getClockIn().toString() : "N/A";
            String clockOutStr = r.getClockOut() != null ? r.getClockOut().toString() : "ACTIVE SHIFT";
            
            history.append(String.format("%-12s | %-22s | %-22s | %.2f hours\n",
                    r.getLogDate().toString(),
                    clockInStr,
                    clockOutStr,
                    r.getWorkHours()));
            
            if (r.getClockOut() != null) {
                totalHours += r.getWorkHours();
                completedShifts++;
            }
        }
        
        double avgHours = completedShifts > 0 ? (totalHours / completedShifts) : 0.0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================================\n");
        sb.append("                     EMPLOYEE PERFORMANCE STATISTICS & RECORD                    \n");
        sb.append("=================================================================================\n");
        sb.append(String.format("Employee Code: %s\n", employee.getEmployeeCode()));
        sb.append(String.format("Full Name:     %s\n", employee.getFullName()));
        sb.append(String.format("Department:    %s\n", employee.getDepartment()));
        sb.append("---------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Registered Shifts:  %d\n", totalShifts));
        sb.append(String.format("Total Hours Completed:    %.2f hours\n", totalHours));
        sb.append(String.format("Average Shift Length:     %.2f hours\n", avgHours));
        sb.append("=================================================================================\n\n");
        sb.append("LOG HISTORY:\n");
        sb.append(history.toString());
        
        return sb.toString();
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
