package org.groupfive.siomai.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
    private static HikariDataSource hikariDataSource = null;
    private static boolean sqliteInitialized = false;

    static {
        // Load properties from the file system (root working directory)
        File file = new File(PROPERTIES_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Warning: Could not load db.properties from file system: " + e.getMessage());
            }
        } else {
            System.out.println("Info: " + PROPERTIES_FILE + " not found at project root. Using default local SQLite database configuration.");
        }
    }

    private static synchronized HikariDataSource getDataSource() throws SQLException {
        if (hikariDataSource == null) {
            String host = properties.getProperty("db.mysql.host", "localhost");
            String port = properties.getProperty("db.mysql.port", "3306");
            String name = properties.getProperty("db.mysql.database", "siomai");
            String user = properties.getProperty("db.mysql.username", "root");
            String pass = properties.getProperty("db.mysql.password", "");
            String useSSL = properties.getProperty("db.mysql.use_ssl", "false");
            String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&trustServerCertificate=true&allowPublicKeyRetrieval=true", host, port, name, useSSL);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool tuning
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000); // 5 mins
            config.setConnectionTimeout(10000); // 10 secs
            
            hikariDataSource = new HikariDataSource(config);

            // Initialize DB tables once on pool creation
            try (Connection conn = hikariDataSource.getConnection()) {
                initializeMySQLDatabase(conn);
            }
        }
        return hikariDataSource;
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
        return getDataSource().getConnection();
    }

    private static Connection getSQLiteConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: SQLite JDBC Driver class not found: " + e.getMessage());
        }

        String url = "jdbc:sqlite:" + SQLITE_DB_NAME;
        Connection conn = DriverManager.getConnection(url);

        // Always enable foreign keys for every SQLite connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        synchronized (DatabaseConnector.class) {
            if (!sqliteInitialized) {
                initializeSQLiteDatabase(conn);
                sqliteInitialized = true;
            }
        }
        return conn;
    }

    private static void initializeSQLiteDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Admins Table
            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "full_name TEXT NOT NULL" +
                    ");");

            // 2. Employees Table
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employee_code TEXT UNIQUE NOT NULL," +
                    "full_name TEXT NOT NULL," +
                    "department TEXT NOT NULL," +
                    "is_active INTEGER DEFAULT 1" +
                    ");");

            // 3. Daily Verification Codes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_codes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "validation_code TEXT NOT NULL," +
                    "generated_date TEXT UNIQUE NOT NULL DEFAULT (date('now'))" +
                    ");");

            // Heuristic check: only seed if the admins table is empty (fresh database creation)
            boolean needsSeeding = false;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admins")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    needsSeeding = true;
                }
            }

            if (needsSeeding) {
                // Seed Default Admin
                stmt.execute("INSERT INTO admins (id, username, password, full_name) " +
                        "VALUES (1, 'admin', 'admin123', 'System Administrator');");

                // Seed Test Employees
                stmt.execute("INSERT INTO employees (id, employee_code, full_name, department) VALUES " +
                        "(1, 'EMP-001', 'Alice Smith', 'Engineering')," +
                        "(2, 'EMP-002', 'Bob Jones', 'Human Resources')," +
                        "(3, 'EMP-003', 'Charlie Brown', 'Design');");

                // Seed initial code for today
                stmt.execute("INSERT INTO daily_codes (id, validation_code, generated_date) " +
                        "VALUES (1, '12345', date('now'));");
            }

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

            // 5. Employee Daily Codes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS employee_daily_codes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employee_id INTEGER NOT NULL," +
                    "validation_code TEXT NOT NULL," +
                    "generated_date TEXT NOT NULL," +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE," +
                    "UNIQUE(employee_id, generated_date)" +
                    ");");
        }
    }

    private static boolean checkIfMySQLTableExists(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM admins LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void initializeMySQLDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Admins Table
            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "full_name VARCHAR(100) NOT NULL" +
                    ");");

            // 2. Employees Table
            stmt.execute("CREATE TABLE IF NOT EXISTS employees (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "employee_code VARCHAR(20) UNIQUE NOT NULL," +
                    "full_name VARCHAR(100) NOT NULL," +
                    "department VARCHAR(50) NOT NULL," +
                    "is_active BOOLEAN DEFAULT TRUE" +
                    ");");

            // 3. Daily Verification Codes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_codes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "validation_code VARCHAR(10) NOT NULL," +
                    "generated_date DATE UNIQUE NOT NULL DEFAULT (CURRENT_DATE)" +
                    ");");

            // Heuristic check: only seed if the admins table is empty (fresh database creation)
            boolean needsSeeding = false;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admins")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    needsSeeding = true;
                }
            }

            if (needsSeeding) {
                // Seed Default Admin
                stmt.execute("INSERT INTO admins (id, username, password, full_name) " +
                        "VALUES (1, 'admin', 'admin123', 'System Administrator');");

                // Seed Test Employees
                stmt.execute("INSERT INTO employees (id, employee_code, full_name, department) VALUES " +
                        "(1, 'EMP-001', 'Alice Smith', 'Engineering')," +
                        "(2, 'EMP-002', 'Bob Jones', 'Human Resources')," +
                        "(3, 'EMP-003', 'Charlie Brown', 'Design');");

                // Seed initial code for today
                stmt.execute("INSERT INTO daily_codes (id, validation_code, generated_date) " +
                        "VALUES (1, '12345', CURRENT_DATE);");
            }

            // 4. Attendance Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "employee_id INT NOT NULL," +
                    "clock_in TIMESTAMP NULL," +
                    "clock_out TIMESTAMP NULL," +
                    "log_date DATE NOT NULL DEFAULT (CURRENT_DATE)," +
                    "work_hours DOUBLE DEFAULT 0.0," +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE" +
                    ");");

            // 5. Employee Daily Codes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS employee_daily_codes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "employee_id INT NOT NULL," +
                    "validation_code VARCHAR(10) NOT NULL," +
                    "generated_date DATE NOT NULL," +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE," +
                    "UNIQUE(employee_id, generated_date)" +
                    ");");
        }
    }
}
