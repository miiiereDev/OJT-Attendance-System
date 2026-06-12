package org.groupfive.siomai.database;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectorTest {

    @Test
    public void testGetConnectionAndInitialization() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConnector.getConnection()) {
                assertNotNull(conn, "Database connection should not be null");
                assertFalse(conn.isClosed(), "Database connection should be open");

                // Verify tables were created and default admin seeded
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM admins WHERE username = 'admin'")) {
                    assertTrue(rs.next(), "Admin user should exist in the admins table");
                    assertEquals("admin123", rs.getString("password"), "Admin password should match default");
                    assertEquals("System Administrator", rs.getString("full_name"), "Admin name should match default");
                }

                // Verify employees table exists and has seeded test records
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM employees")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getInt("cnt") >= 3, "Employees table should contain seeded employee records");
                }
            }
        });
    }
}
