package org.groupfive.siomai.service;

import org.groupfive.siomai.database.DatabaseOperations;
import org.groupfive.siomai.database.DatabaseOperationsImpl;
import org.groupfive.siomai.model.Employee;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest {
    private static DatabaseOperations dbOps;
    private static AdminService adminService;

    @BeforeAll
    public static void setUp() {
        dbOps = new DatabaseOperationsImpl();
        adminService = new AdminService(dbOps);
    }

    @Test
    public void testAuthentication() throws SQLException {
        // Correct admin credentials seeded
        assertTrue(adminService.authenticate("admin", "admin123"));

        // Incorrect credentials
        assertFalse(adminService.authenticate("admin", "wrongpassword"));
        assertFalse(adminService.authenticate("nonexistent", "somepass"));
        assertFalse(adminService.authenticate("", ""));
    }

    @Test
    public void testEmployeeCRUDAndValidations() throws SQLException {
        // Setup details
        String code = "EMP-TEST-ADMIN";
        String name = "Admin CRUD Tester";
        String dept = "QA Department";

        // Add
        assertDoesNotThrow(() -> adminService.addEmployee(code, name, dept));

        // Attempt duplicate add
        assertThrows(IllegalArgumentException.class, () -> adminService.addEmployee(code, name, dept));

        // Read & verify
        Employee emp = dbOps.getEmployeeByCode(code);
        assertNotNull(emp);
        assertEquals(name, emp.getFullName());

        // Update
        assertDoesNotThrow(() -> adminService.updateEmployee(emp.getId(), code, "Updated Name", "DevOps", false));
        Employee updated = dbOps.getEmployeeById(emp.getId());
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getFullName());
        assertEquals("DevOps", updated.getDepartment());
        assertFalse(updated.isActive());

        // Delete
        assertDoesNotThrow(() -> adminService.deleteEmployee(emp.getId()));
        assertNull(dbOps.getEmployeeById(emp.getId()));
    }

    @Test
    public void testEmployeeDailyCodes() throws SQLException {
        Employee alice = dbOps.getEmployeeByCode("EMP-001");
        assertNotNull(alice);
        int aliceId = alice.getId();

        String code1 = adminService.getEmployeeCodeForToday(aliceId);
        assertNotNull(code1);
        assertEquals(5, code1.length(), "Code should be exactly 5 digits long");

        String code2 = adminService.getEmployeeCodeForToday(aliceId);
        assertEquals(code1, code2, "Subsequent fetches today should return the same code");

        String resetCode = adminService.resetEmployeeCode(aliceId);
        assertNotNull(resetCode);
        assertEquals(5, resetCode.length());

        String fetchedCode = adminService.getEmployeeCodeForToday(aliceId);
        assertEquals(resetCode, fetchedCode, "Fetched code after reset should match reset code");
    }

    @Test
    public void testGenerateWorkHoursReport() throws SQLException {
        String report = adminService.generateWorkHoursReport();
        assertNotNull(report);
        assertTrue(report.contains("CODE"));
        assertTrue(report.contains("TOTAL WORK HOURS"));
        assertTrue(report.contains("EMP-001")); // should contain seeded employees
    }
}
