package org.groupfive.siomai.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class to manage connection states for MySQL and SQLite databases.
 * Defaults to SQLite if MySQL configuration is not available or fails.
 */
public class DatabaseConnector {
    private static final String PROPERTIES_FILE = "db.properties";
    private static final String SQLITE_DB_NAME = "attendance.db";
    private static Properties properties = new Properties();

    static {
        // Load properties file if it exists
        File file = new File(PROPERTIES_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Warning: Could not load db.properties: " + e.getMessage());
            }
        }
    }

    /**
     * Obtains a connection to the database.
     * Tries MySQL first if configured, otherwise falls back to SQLite.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String useMySQLStr = properties.getProperty("db.use_mysql", "false");
        boolean useMySQL = Boolean.parseBoolean(useMySQLStr);

        if (useMySQL) {
            try {
                return getMySQLConnection();
            } catch (SQLException e) {
                System.err.println("MySQL connection failed, falling back to local SQLite: " + e.getMessage());
            }
        }

        return getSQLiteConnection();
    }

    private static Connection getMySQLConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }

        String host = properties.getProperty("db.mysql.host", "mysql-siomaidb-employee-attendance-db.e.aivencloud.com");
        String port = properties.getProperty("db.mysql.port", "13057");
        String name = properties.getProperty("db.mysql.database", "defaultdb");
        String user = properties.getProperty("db.mysql.username", "root");
        String pass = properties.getProperty("db.mysql.password", "");

        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=true&trustServerCertificate=true&allowPublicKeyRetrieval=true", host, port, name);
        return DriverManager.getConnection(url, user, pass);
    }

    private static Connection getSQLiteConnection() throws SQLException {
        try {
            Class.forName("org.xerial.sqlite-jdbc");
        } catch (ClassNotFoundException e) {
            // org.xerial:sqlite-jdbc may not need explicit Class.forName in newer JDBC versions, but good practice
        }

        String url = "jdbc:sqlite:" + SQLITE_DB_NAME;
        boolean dbExists = new File(SQLITE_DB_NAME).exists();
        Connection conn = DriverManager.getConnection(url);

        if (!dbExists) {
            initializeSQLiteDatabase(conn);
        }
        return conn;
    }

    private static void initializeSQLiteDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Enable foreign key support in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Admins Table
            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "full_name TEXT NOT NULL" +
                    ");");

            // Seed Default Admin (only if empty)
            stmt.execute("INSERT OR IGNORE INTO admins (id, username, password, full_name) " +
                    "VALUES (1, 'admin', 'admin123', 'System Administrator');");

            // 2. Employees Table
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employee_code TEXT UNIQUE NOT NULL," +
                    "full_name TEXT NOT NULL," +
                    "department TEXT NOT NULL," +
                    "is_active INTEGER DEFAULT 1" +
                    ");");

            // Seed Test Employees (only if empty)
            stmt.execute("INSERT OR IGNORE INTO employees (id, employee_code, full_name, department) VALUES " +
                    "(1, 'EMP-001', 'Alice Smith', 'Engineering')," +
                    "(2, 'EMP-002', 'Bob Jones', 'Human Resources')," +
                    "(3, 'EMP-003', 'Charlie Brown', 'Design');");

            // 3. Daily Verification Codes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_codes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "validation_code TEXT NOT NULL," +
                    "generated_date TEXT UNIQUE NOT NULL DEFAULT (date('now'))" +
                    ");");

            // Seed initial code for today
            stmt.execute("INSERT OR IGNORE INTO daily_codes (id, validation_code, generated_date) " +
                    "VALUES (1, '12345', date('now'));");

            // 4. Attendance Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employee_id INTEGER NOT NULL," +
                    "clock_in TEXT NULL," +
                    "clock_out TEXT NULL," +
                    "log_date TEXT NOT NULL DEFAULT (date('now'))," +
                    "work_hours REAL DEFAULT 0.0," +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE" +
                    ");");
        }
    }
}
