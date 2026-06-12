# OJT Attendance System

A high-performance, modular, and visually stunning **OJT (On-the-Job Training) Attendance Monitoring System** built in Java (Swing) utilizing a layered Architecture. It supports connection pooling (via HikariCP), asynchronous background sync polling, local memory caching, and dual-profile database portability (supporting remote Aiven MySQL over SSL and local SQLite fallback).

---

## 🚀 Key Features
1. **Interactive Launcher**: A splash landing screen to run either the **Employee Kiosk** or **Admin Control Portal**.
2. **Employee Kiosk Mode**: 
   - **Interactive Selector**: Real-time fuzzy filtering dropdown (JComboBox) matching employees by name or code as they type.
   - **Performance Stats Popup**: Securely shows completed shift logs, total hours, and average work hours in a monospaced scrollable pane upon daily code validation.
3. **Admin Control Portal**:
   - **Directory**: Full CRUD operations for employee accounts.
   - **Daily Code Center**: Table showing each active employee's daily code, with an instant manual **Reset Code** action.
   - **Work Hours Report**: Monospaced text summary compile of all employee logs.
4. **Performance Overhaul Layer (Stage 9)**:
   - **HikariCP Connection Pool**: Keeps active MySQL connections recycled, reducing WAN query latency from **~800ms down to ~5ms**.
   - **Memory Caching (CachedDatabaseOperations)**: Serves directory and code reads instantly (<1ms).
   - **Asynchronous Writes**: Clock-in/out updates execute in background worker threads so the UI is 100% responsive.
   - **Sync Indicators**: Color-coded sync status (`● Synced` / `↻ Syncing...`) and timers (`Last Sync: X seconds ago`) on both frames.
   - **Double-Check Sync on Mismatch**: Failed validations check MySQL directly to handle manual admin resets in real-time.
   - **Offline Resilience**: Automatically falls back to local SQLite operations if MySQL goes offline.

---

## 🗄️ Database Structure & Schema

The system automatically initializes the database schema on startup. Below are the definitions for the tables:

### 1. Admins Table (`admins`)
Stores administrator accounts for Portal login.
```sql
-- MySQL Definition
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- SQLite Fallback
CREATE TABLE IF NOT EXISTS admins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL
);
```

### 2. Employees Table (`employees`)
Stores employee directory registration files.
```sql
-- MySQL Definition
CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- SQLite Fallback
CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_code TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    department TEXT NOT NULL,
    is_active INTEGER DEFAULT 1
);
```

### 3. Attendance Logs Table (`attendance_logs`)
Records employee clock-in and clock-out timestamps, log dates, and work hours.
```sql
-- MySQL Definition
CREATE TABLE IF NOT EXISTS attendance_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    clock_in TIMESTAMP NULL,
    clock_out TIMESTAMP NULL,
    log_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    work_hours DOUBLE DEFAULT 0.0,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- SQLite Fallback
CREATE TABLE IF NOT EXISTS attendance_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    clock_in TEXT NULL,
    clock_out TEXT NULL,
    log_date TEXT NOT NULL DEFAULT (date('now')),
    work_hours REAL DEFAULT 0.0,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);
```

### 4. Employee Daily Codes Table (`employee_daily_codes`)
Generates unique 5-digit verification codes per employee per day.
```sql
-- MySQL Definition
CREATE TABLE IF NOT EXISTS employee_daily_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    validation_code VARCHAR(10) NOT NULL,
    generated_date DATE NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE(employee_id, generated_date)
);

-- SQLite Fallback
CREATE TABLE IF NOT EXISTS employee_daily_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    validation_code TEXT NOT NULL,
    generated_date TEXT NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE(employee_id, generated_date)
);
```

---

## 🔍 SQL Queries by Operation

Below are the exact SQL queries executed by `DatabaseOperationsImpl` for every functional requirement in the application.

### 1. Admin Authentication
* **Get Admin Profile**: Used during Admin login authentication to match the username.
  ```sql
  SELECT * FROM admins WHERE username = ?;
  ```

### 2. Employee Registry Directory (CRUD)
* **Insert New Employee**: Adds a new record to the employee table.
  ```sql
  INSERT INTO employees (employee_code, full_name, department, is_active) 
  VALUES (?, ?, ?, ?);
  ```
* **Retrieve Employee List**: Queries all employees in the directory.
  ```sql
  SELECT * FROM employees;
  ```
* **Retrieve Employee by ID**: Selects a specific employee detail using their database key.
  ```sql
  SELECT * FROM employees WHERE id = ?;
  ```
* **Retrieve Employee by Unique Code**: Checks if an employee code is already taken or retrieves details.
  ```sql
  SELECT * FROM employees WHERE employee_code = ?;
  ```
* **Fuzzy Directory Search**: Used in real-time search filtering in both Kiosk and Admin lists.
  ```sql
  SELECT * FROM employees WHERE full_name LIKE ? OR employee_code LIKE ?;
  ```
* **Update Employee Details**: Edits basic details and toggles active status.
  ```sql
  UPDATE employees SET employee_code = ?, full_name = ?, department = ?, is_active = ? 
  WHERE id = ?;
  ```
* **Delete Employee**: Permanently removes an employee account.
  ```sql
  DELETE FROM employees WHERE id = ?;
  ```

### 3. Daily Verification Codes (Per-Employee)
* **Get Employee Daily Code**: Reads the generated code for a specific employee today.
  ```sql
  SELECT validation_code FROM employee_daily_codes 
  WHERE employee_id = ? AND generated_date = ?;
  ```
* **Check Code Record Existence**: Used to decide whether to run an `INSERT` or an `UPDATE`.
  ```sql
  SELECT id FROM employee_daily_codes 
  WHERE employee_id = ? AND generated_date = ?;
  ```
* **Update Code**: Overwrites the verification code (such as during manual reset).
  ```sql
  UPDATE employee_daily_codes SET validation_code = ? 
  WHERE employee_id = ? AND generated_date = ?;
  ```
* **Insert Code**: Saves a newly generated code.
  ```sql
  INSERT INTO employee_daily_codes (employee_id, validation_code, generated_date) 
  VALUES (?, ?, ?);
  ```

### 4. Attendance Clocking Transactions
* **Get Today's Shift Log**: Checks if an employee has already clocked in today.
  ```sql
  SELECT * FROM attendance_logs WHERE employee_id = ? AND log_date = ?;
  ```
* **Clock-In**: Logs initial arrival timestamp.
  ```sql
  INSERT INTO attendance_logs (employee_id, clock_in, clock_out, log_date, work_hours) 
  VALUES (?, ?, ?, ?, ?);
  ```
* **Clock-Out**: Registers departure timestamp and saves calculated decimal work hours.
  ```sql
  UPDATE attendance_logs SET clock_out = ?, work_hours = ? 
  WHERE id = ?;
  ```

### 5. Administrative Reporting & Statistics
* **Compile Total Work Hours**: Aggregates total logged hours per employee for reports.
  ```sql
  SELECT SUM(work_hours) AS total_hrs FROM attendance_logs WHERE employee_id = ?;
  ```
* **Retrieve Employee History**: Compiles a chronological log list for the "View Stats" modal.
  ```sql
  SELECT * FROM attendance_logs WHERE employee_id = ? 
  ORDER BY log_date DESC, clock_in DESC;
  ```
* **Retrieve Logs for Specific Date**: Filters attendance reports by date.
  ```sql
  SELECT * FROM attendance_logs WHERE log_date = ?;
  ```

---


## 📂 Class-by-Class Documentation

### 1. Database & Infrastructure Layer
- **`DatabaseConnector.java`**: Manages SQL connection pools using HikariCP for MySQL, or sets up a local SQLite connection fallback. Runs schema initializations once upon connection pool setup.
- **`DatabaseOperations.java`**: Interface specifying abstract CRUD operations (Abstraction principle).
- **`DatabaseOperationsImpl.java`**: Implements raw JDBC calls. Handles database updates, queries, and SQLite date parsing helpers.
- **`CachedDatabaseOperations.java`**: **Cache Decorator** wrapping the raw database operations. Manages in-memory maps, background synchronization thread pool (ScheduledExecutorService), async write workers (`CompletableFuture.runAsync()`), manual refresh throttles, and mismatch double-checks.

### 2. Business Services Layer
- **`AdminService.java`**: Implements administrative logic. Handles password check, work hours report compiles, and manual employee code resets.
- **`KioskService.java`**: Coordinates employee kiosk operations. Auto-generates random 5-digit verification codes daily, validates codes, processes clock-in/out transitions, and compiles performance metrics.

### 3. OOP Core Domain Models
- **`Person.java`**: Abstract base class representing encapsulations of common attributes (`id`, `fullName`).
- **`Admin.java`**: Subclass representing administrative portal credentials.
- **`Employee.java`**: Subclass representing active/inactive employees.
- **`AttendanceRecord.java`**: Domain model mapping log rows. Includes the business logic equation calculating decimal hours between clock-in and clock-out.

### 4. User Interface Layer
- **`AttendanceApp.java`**: Application entry point. Instantiates `LauncherFrame` to boot the application.
- **`LauncherFrame.java`**: Dual-portal dashboard designed with deep slate CSS theme mimicking modern visual cards.
- **`KioskFrame.java`**: Full-screen kiosk terminal containing fuzzy search input, JComboBox selector, daily code input, clock actions, View Stats modal, and reactive Sync status/timer headers.
- **`AdminFrame.java`**: Dashboard holding tables for Employee Directory CRUD operations, Daily Code Center resets, and work hours text report scroll pane.

### 5. Custom Exceptions
- **`AppException.java`**: Base unchecked application exception.
- **`EmployeeNotFoundException.java`**: Thrown when search filters return no matching active employees.
- **`InvalidDailyCodeException.java`**: Thrown when daily code input fails verification.

---

## 🔒 Security & Connection Setup

To prevent database connection credentials and passwords from leaking in public repositories, a `.gitignore` rule is configured:
- `/db.properties` at the root folder is ignored by git.
- The compiled version of the app reads the default connection configuration packaged inside the JAR at `src/main/resources/db.properties`.

### Custom Setup:
To configure your own remote database:
1. Copy the connection settings from `db.properties.example`.
2. Save it as `db.properties` at the root folder.
3. Edit the fields to supply your credentials:
   ```properties
   # Enable/Disable MySQL (setting false falls back to SQLite)
   db.use_mysql=true

   # Connection Details
   db.mysql.host=YOUR_DATABASE_HOST
   db.mysql.port=YOUR_DATABASE_PORT
   db.mysql.database=YOUR_DATABASE_NAME
   db.mysql.username=YOUR_DATABASE_USER
   db.mysql.password=YOUR_DATABASE_PASSWORD
   ```

---

## 🛠️ Build & Run Instructions

### Prerequisites
- JDK 21 or higher installed.
- Apache Maven installed (or run through your IDE).

### 1. Compile & Test
To clean build and run the JUnit 5 test suite:
```bash
mvn clean test
```

### 2. Package Executable JAR
To compile, verify, and bundle the dependencies into a single shaded Fat JAR:
```bash
mvn package -DskipTests
```
This produces a ready-to-run JAR at `target/siomai-1.0-SNAPSHOT.jar`.

### 3. Run the Application
Execute the JAR file using java command:
```bash
java -jar target/siomai-1.0-SNAPSHOT.jar
```
