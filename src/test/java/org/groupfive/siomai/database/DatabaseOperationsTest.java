package org.groupfive.siomai.database;

import org.groupfive.siomai.model.Admin;
import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseOperationsTest {

    private static DatabaseOperations dbOps;

    @BeforeAll
    public static void setUp() {
        dbOps = new DatabaseOperationsImpl();
    }

    @Test
    public void testAdminLookup() throws SQLException {
        Admin admin = dbOps.getAdminByUsername("admin");
        assertNotNull(admin);
        assertEquals("admin123", admin.getPassword());
        assertEquals("System Administrator", admin.getFullName());
    }

    @Test
    public void testEmployeeCRUDAndSearch() throws SQLException {
        // Clean up pre-existing test employee if any from failed previous runs
        Employee existing = dbOps.getEmployeeByCode("EMP-999");
        if (existing != null) {
            dbOps.deleteEmployee(existing.getId());
        }

        // Create
        Employee emp = new Employee(0, "Test Developer", "EMP-999", "Engineering", true);
        dbOps.addEmployee(emp);
        assertTrue(emp.getId() > 0, "Generated ID should be positive");

        // Read (by Code)
        Employee fetched = dbOps.getEmployeeByCode("EMP-999");
        assertNotNull(fetched);
        assertEquals("Test Developer", fetched.getFullName());
        assertEquals("Engineering", fetched.getDepartment());

        // Update
        fetched.setFullName("Updated Developer");
        fetched.setDepartment("Research");
        dbOps.updateEmployee(fetched);

        Employee updated = dbOps.getEmployeeById(fetched.getId());
        assertNotNull(updated);
        assertEquals("Updated Developer", updated.getFullName());
        assertEquals("Research", updated.getDepartment());

        // Search
        List<Employee> searchResults = dbOps.searchEmployees("Updated");
        assertFalse(searchResults.isEmpty());
        assertEquals(1, searchResults.stream().filter(e -> e.getEmployeeCode().equals("EMP-999")).count());

        // Delete
        dbOps.deleteEmployee(fetched.getId());
        assertNull(dbOps.getEmployeeById(fetched.getId()));
    }

    @Test
    public void testDailyCodes() throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        dbOps.setDailyCode("98765", today);

        String code = dbOps.getDailyCode(today);
        assertEquals("98765", code);

        // Update it
        dbOps.setDailyCode("54321", today);
        code = dbOps.getDailyCode(today);
        assertEquals("54321", code);
    }

    @Test
    public void testAttendanceRecordFlow() throws SQLException {
        // Fetch Alice (seeded EMP-001)
        Employee alice = dbOps.getEmployeeByCode("EMP-001");
        assertNotNull(alice);

        Date today = new Date(System.currentTimeMillis());

        // Clean any potential pre-existing log for testing
        try (Connection conn = DatabaseConnector.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM attendance_logs WHERE employee_id = ?")) {
            ps.setInt(1, alice.getId());
            ps.executeUpdate();
        }

        // Clock In
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(alice.getId());
        record.setClockIn(new Timestamp(System.currentTimeMillis() - 8 * 3600 * 1000)); // 8 hours ago
        record.setLogDate(today);
        
        dbOps.addAttendanceRecord(record);
        assertTrue(record.getId() > 0);

        // Clock Out
        AttendanceRecord fetchedLog = dbOps.getTodayAttendanceRecord(alice.getId(), today);
        assertNotNull(fetchedLog);
        assertNull(fetchedLog.getClockOut());

        fetchedLog.setClockOut(new Timestamp(System.currentTimeMillis()));
        dbOps.updateAttendanceRecord(fetchedLog);

        AttendanceRecord finalLog = dbOps.getTodayAttendanceRecord(alice.getId(), today);
        assertNotNull(finalLog);
        assertNotNull(finalLog.getClockOut());
        assertTrue(finalLog.getWorkHours() >= 7.9, "Work hours should be calculated around 8 hours");
    }
}
