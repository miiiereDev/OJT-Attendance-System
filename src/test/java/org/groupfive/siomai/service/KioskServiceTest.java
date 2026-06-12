package org.groupfive.siomai.service;

import org.groupfive.siomai.database.DatabaseOperations;
import org.groupfive.siomai.database.DatabaseOperationsImpl;
import org.groupfive.siomai.exception.EmployeeNotFoundException;
import org.groupfive.siomai.exception.InvalidDailyCodeException;
import org.groupfive.siomai.model.Employee;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KioskServiceTest {
    private static DatabaseOperations dbOps;
    private static KioskService kioskService;

    @BeforeAll
    public static void setUp() {
        dbOps = new DatabaseOperationsImpl();
        kioskService = new KioskService(dbOps);
    }

    @Test
    public void testSearchActiveEmployees() throws SQLException, EmployeeNotFoundException {
        // Query Alice (seeded "EMP-001")
        List<Employee> results = kioskService.searchActiveEmployees("Alice");
        assertFalse(results.isEmpty());
        assertEquals("EMP-001", results.get(0).getEmployeeCode());

        // Query code
        List<Employee> results2 = kioskService.searchActiveEmployees("EMP-002");
        assertFalse(results2.isEmpty());
        assertEquals("Bob Jones", results2.get(0).getFullName());

        // Query invalid
        assertThrows(EmployeeNotFoundException.class, () -> {
            kioskService.searchActiveEmployees("NonExistentName");
        });
    }

    @Test
    public void testValidateDailyCode() throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        dbOps.setDailyCode("55555", today);

        // Correct code
        assertDoesNotThrow(() -> kioskService.validateDailyCode("55555"));

        // Incorrect code
        assertThrows(InvalidDailyCodeException.class, () -> kioskService.validateDailyCode("wrong_code"));
    }

    @Test
    public void testProcessAttendance() throws SQLException, EmployeeNotFoundException {
        // Create a temporary employee for clean flow testing
        Employee tempEmp = new Employee(0, "Kiosk Tester", "EMP-TEMP", "QA", true);
        dbOps.addEmployee(tempEmp);

        // First transition: Clock-In
        String clockInResult = kioskService.processAttendance(tempEmp);
        assertTrue(clockInResult.contains("Clock-in Successful"));

        // Second transition: Clock-Out
        String clockOutResult = kioskService.processAttendance(tempEmp);
        assertTrue(clockOutResult.contains("Clock-out Successful"));

        // Third transition: Shift completed
        String completedResult = kioskService.processAttendance(tempEmp);
        assertTrue(completedResult.contains("already completed your shift"));

        // Clean up
        dbOps.deleteEmployee(tempEmp.getId());
    }
}
