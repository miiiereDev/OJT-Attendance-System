package org.groupfive.siomai.database;

import org.groupfive.siomai.model.Employee;
import org.groupfive.siomai.model.AttendanceRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CachedDatabaseOperationsTest {

    private static DatabaseOperationsImpl rawDbOps;
    private static CachedDatabaseOperations cachedDbOps;

    @BeforeAll
    public static void setUp() {
        rawDbOps = new DatabaseOperationsImpl();
        cachedDbOps = new CachedDatabaseOperations(rawDbOps);
    }

    @Test
    public void testEmployeeCacheAndInvalidation() throws SQLException {
        // Clear any pre-existing test employee
        Employee existing = rawDbOps.getEmployeeByCode("EMP-CACHE-TEST");
        if (existing != null) {
            rawDbOps.deleteEmployee(existing.getId());
        }

        // Preheat cache
        List<Employee> firstRead = cachedDbOps.getAllEmployees();
        assertNotNull(firstRead);

        // Add employee via cached operations
        Employee emp = new Employee(0, "Cache Tester", "EMP-CACHE-TEST", "Engineering", true);
        cachedDbOps.addEmployee(emp);

        // The read should now reflect the new employee due to invalidation and refresh
        List<Employee> secondRead = cachedDbOps.getAllEmployees();
        assertTrue(secondRead.stream().anyMatch(e -> e.getEmployeeCode().equals("EMP-CACHE-TEST")),
                "New employee should be visible in cache after write invalidation");

        // Clean up
        cachedDbOps.deleteEmployee(emp.getId());
    }

    @Test
    public void testDailyCodeCachingAndDirectMismatchFetch() throws SQLException {
        Employee alice = rawDbOps.getEmployeeByCode("EMP-001");
        assertNotNull(alice);
        int aliceId = alice.getId();
        Date today = new Date(System.currentTimeMillis());

        // Set a daily code via raw DB operations to bypass instant cache set
        rawDbOps.setEmployeeDailyCode(aliceId, "77777", today);

        // Fetch via cache - since we set it directly in DB, it might not be in cache yet
        // A direct fetch will sync it and register it in cache
        String code1 = cachedDbOps.getEmployeeDailyCodeDirect(aliceId, today);
        assertEquals("77777", code1);

        // Now change it directly in MySQL (simulating another instance or Admin reset)
        rawDbOps.setEmployeeDailyCode(aliceId, "88888", today);

        // Fetching standard daily code should still return the cached "77777" due to TTL
        String cachedCode = cachedDbOps.getEmployeeDailyCode(aliceId, today);
        assertEquals("77777", cachedCode, "Standard get should pull from cache");

        // Direct fetch should bypass cache, fetch "88888", and update cache
        String freshCode = cachedDbOps.getEmployeeDailyCodeDirect(aliceId, today);
        assertEquals("88888", freshCode, "Direct get should fetch fresh code and update cache");

        // Subsequent standard reads should now return "88888"
        assertEquals("88888", cachedDbOps.getEmployeeDailyCode(aliceId, today));
    }

    @Test
    public void testManualRefreshCooldown() {
        // Trigger first refresh -> should succeed
        boolean firstTrigger = cachedDbOps.forceRefresh();
        
        // Trigger second refresh immediately -> should fail due to 5-second cooldown
        boolean secondTrigger = cachedDbOps.forceRefresh();
        
        if (firstTrigger) {
            assertFalse(secondTrigger, "Second refresh within 5 seconds should be ignored (cooldown)");
        }
    }
}
