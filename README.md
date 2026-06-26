# 🥟 OJT Attendance System

A high-performance modular **OJT Attendance Monitoring System** built in Java (Swing) utilizing a layered architecture. It supports connection pooling (via HikariCP), asynchronous background sync polling, local memory caching, and dual-profile database portability (supporting remote MySQL/Aiven MySQL over SSL and local SQLite fallback).

---

## ✨ Key Features
1. **Interactive Launcher**: A splash landing screen to run either the **Employee Kiosk** or **Admin Control Portal**.
2. **Employee Kiosk Mode**: 
   - **Interactive Selector**: Real-time fuzzy filtering dropdown (JComboBox) matching employees by name or code as they type.
   - **Performance Stats Popup**: Securely shows completed shift logs, total hours, and average work hours in a monospaced scrollable pane upon daily code validation.
3. **Admin Control Portal**:
   - **Directory**: Full CRUD operations for employee accounts.
   - **Daily Code Center**: Table showing each active employee's daily code, with an instant manual **Reset Code** action.
   - **Work Hours Report**: Monospaced text summary compile of all employee logs.
4. **Performance Overhaul Layer**:
   - **HikariCP Connection Pool**: Keeps active MySQL connections recycled, reducing WAN query latency from **~800ms down to ~5ms**.
   - **Memory Caching (CachedDatabaseOperations)**: Serves directory and code reads instantly (<1ms).
   - **Asynchronous Writes**: Clock-in/out updates execute in background worker threads so the UI is 100% responsive.
   - **Sync Indicators**: Color-coded sync status (`● Synced` / `↻ Syncing...`) and timers (`Last Sync: X seconds ago`) on both frames.
   - **Double-Check Sync on Mismatch**: Failed validations check MySQL directly to handle manual admin resets in real-time.
   - **Offline Resilience**: Automatically falls back to local SQLite operations if MySQL goes offline.

---

## 🛠️ Database Setup Guide

> [!IMPORTANT]
> **Default Administrative Credentials:**
> - **Username:** `admin`
> - **Password:** `admin123`
> 
> *(These credentials are automatically seeded upon SQLite auto-creation or executing `schema.sql` on MySQL.)*

The application supports dual profiles: **SQLite** (local file-based, zero configuration) and **MySQL** (local or cloud-hosted).

### Option 1: SQLite (Zero Configuration Quick Start)
By default, the application is pre-configured to use **SQLite** as its fallback database. 
1. Run the application (see [Build & Run](#-build--run-instructions)).
2. An `attendance.db` file will be created automatically in the root folder.
3. Tables and test data are automatically initialized. No setup or external server required!

---

### Option 2: Local MySQL Setup (Recommended for Development)
For local MySQL database hosting (such as XAMPP MySQL, Docker MySQL, or standard MySQL Server), follow these steps:

#### Step 2.1: Run the Database Initialization Script
You need to create the database schema and seed the initial tables. We recommend using **MySQL Workbench** for ease of use:
1. Open **MySQL Workbench** and connect to your local MySQL connection.
2. Go to **File** > **Open SQL Script...** and select the [schema.sql](./schema.sql) file located in the root of the project.
3. Click the **Execute** button (the lightning bolt icon ⚡) or press `Ctrl+Shift+Enter` to run the entire script.
4. This script will automatically:
   - Create a database schema named `siomai`.
   - Setup all necessary tables (`admins`, `employees`, `attendance_logs`, `daily_codes`, `employee_daily_codes`).
   - Seed a default Admin account (`admin` / `admin123`) and 3 test employees.

#### Step 2.2: Configure db.properties
1. Copy [db.properties.example](./db.properties.example) and save it as `db.properties` in the root folder.
2. Edit `db.properties` with the following configuration:
   ```properties
   # Enable MySQL connection profile
   db.use_mysql=true

   # Connection Details
   db.mysql.host=localhost
   db.mysql.port=3306
   db.mysql.database=siomai
   db.mysql.username=root
   db.mysql.password=YOUR_LOCAL_PASSWORD_HERE

   # SSL Configuration (Set to false for local setups without SSL)
   db.mysql.use_ssl=false
   ```

---

### Option 3: Remote/Cloud MySQL Setup (Aiven, AWS RDS, etc.)
To set up a remote cloud database with SSL enabled:
1. Create a MySQL database instance on your cloud provider (e.g., Aiven MySQL).
2. Execute the [schema.sql](./schema.sql) DDL script on the cloud database.
3. Copy `db.properties.example` to `db.properties` at the root folder and configure:
   ```properties
   db.use_mysql=true
   db.mysql.host=YOUR_CLOUD_HOST
   db.mysql.port=YOUR_CLOUD_PORT
   db.mysql.database=siomai
   db.mysql.username=YOUR_CLOUD_USERNAME
   db.mysql.password=YOUR_CLOUD_PASSWORD
   db.mysql.use_ssl=true
   ```

---

## ⚙️ Configuration File Reference (`db.properties`)

| Property Name | Default Value | Description |
| :--- | :--- | :--- |
| `db.use_mysql` | `false` | Set to `true` to connect to MySQL; `false` falls back to SQLite. |
| `db.mysql.host` | `localhost` | Host address of the MySQL server. |
| `db.mysql.port` | `3306` | Port number of the MySQL server. |
| `db.mysql.database` | `siomai` | The MySQL database name. |
| `db.mysql.username` | `root` | Database username. |
| `db.mysql.password` | `""` | Database password. |
| `db.mysql.use_ssl` | `false` | Toggles MySQL SSL connection parameters. |

---

## 🐣 Newbie Onboarding Guide (Step-by-Step)

If you are new to Git, Java development, or terminal commands, follow these exact steps to get the project running on your machine:

### Step 1: Install Prerequisites
1. **Java Development Kit (JDK 21):** Download and install JDK 21 (e.g., from [Eclipse Temurin](https://adoptium.net/) or Oracle).
2. **Git:** Download and install [Git](https://git-scm.com/).

### Step 2: Clone the Repository
Open your terminal (**Command Prompt** or **PowerShell** on Windows, **Terminal** on macOS/Linux) and run the following command to download the code to your machine:
```bash
git clone https://github.com/miiiereDev/OJT-Attendance-System.git
```

### Step 3: Open the Project Directory
You must navigate into the folder where the project files are located. **All subsequent commands (like building or running) must be executed inside this folder:**
```bash
cd "OJT-Attendance-System"
```
*(Tip: On Windows, you can open this folder in File Explorer, hold `Shift`, right-click on an empty space, and select **"Open PowerShell window here"** or **"Open in Terminal"**).*

### Step 4: Run the Application
- If you want a quick start with zero setup, read **[Option 1: SQLite](#option-1-sqlite-zero-configuration-quick-start)**.
- If you want local database storage, follow **[Option 2: Local MySQL](#option-2-local-mysql-setup-recommended-for-development)**.

---

## 🚀 Build & Run Instructions

### Prerequisites
- **JDK 21** or higher.
- (Optional) IntelliJ IDEA or another Java IDE.

> [!NOTE]
> The project includes a Maven Wrapper (`mvnw` / `mvnw.cmd`). You do not need to install Apache Maven manually on your machine.

### 1. Compile & Run Tests
Clean compiler artifacts and run the JUnit 5 test suite:

- **On Windows (cmd/PowerShell):**
  ```cmd
  .\mvnw.cmd clean test
  ```
- **On Linux / macOS:**
  ```bash
  ./mvnw clean test
  ```

### 2. Package Executable JAR
Compile, verify, and bundle the dependencies into a single shaded Fat JAR:

- **On Windows (cmd/PowerShell):**
  ```cmd
  .\mvnw.cmd package -DskipTests
  ```
- **On Linux / macOS:**
  ```bash
  ./mvnw package -DskipTests
  ```

This produces a ready-to-run JAR at `target/siomai-1.0-SNAPSHOT.jar`.

### 3. Launch the Application
Execute the JAR file using the `java` command:
```bash
java -jar target/siomai-1.0-SNAPSHOT.jar
```

---

## 📂 Class-by-Class Documentation

### 1. Database & Infrastructure Layer
- **[DatabaseConnector.java](./src/main/java/org/groupfive/siomai/database/DatabaseConnector.java)**: Manages SQL connection pools using HikariCP for MySQL, or sets up a local SQLite connection fallback. Runs schema initializations exactly once per application boot (safeguarded via synchronization and boolean flag).
- **[DatabaseOperations.java](./src/main/java/org/groupfive/siomai/database/DatabaseOperations.java)**: Interface specifying abstract CRUD operations (Abstraction principle).
- **[DatabaseOperationsImpl.java](./src/main/java/org/groupfive/siomai/database/DatabaseOperationsImpl.java)**: Implements raw JDBC calls. Handles database updates, queries, and SQLite date parsing helpers.
- **[CachedDatabaseOperations.java](./src/main/java/org/groupfive/siomai/database/CachedDatabaseOperations.java)**: Cache Decorator wrapping the raw database operations. Manages in-memory maps, background synchronization thread pool (ScheduledExecutorService), async write workers (`CompletableFuture.runAsync()`), manual refresh throttles, and mismatch double-checks.

### 2. Business Services Layer
- **[AdminService.java](./src/main/java/org/groupfive/siomai/service/AdminService.java)**: Implements administrative logic. Handles password check, work hours report compiles, and manual employee code resets.
- **[KioskService.java](./src/main/java/org/groupfive/siomai/service/KioskService.java)**: Coordinates employee kiosk operations. Auto-generates random 5-digit verification codes daily, validates codes, processes clock-in/out transitions, and compiles performance metrics.

### 3. OOP Core Domain Models
- **[Person.java](./src/main/java/org/groupfive/siomai/model/Person.java)**: Abstract base class representing encapsulations of common attributes (`id`, `fullName`).
- **[Admin.java](./src/main/java/org/groupfive/siomai/model/Admin.java)**: Subclass representing administrative portal credentials.
- **[Employee.java](./src/main/java/org/groupfive/siomai/model/Employee.java)**: Subclass representing active/inactive employees.
- **[AttendanceRecord.java](./src/main/java/org/groupfive/siomai/model/AttendanceRecord.java)**: Domain model mapping log rows. Includes the business logic equation calculating decimal hours between clock-in and clock-out.

### 4. User Interface Layer
- **[AttendanceApp.java](./src/main/java/org/groupfive/siomai/AttendanceApp.java)**: Application entry point. Instantiates `LauncherFrame` to boot the application.
- **[LauncherFrame.java](./src/main/java/org/groupfive/siomai/ui/LauncherFrame.java)**: Dual-portal dashboard designed with deep slate CSS theme mimicking modern visual cards.
- **[KioskFrame.java](./src/main/java/org/groupfive/siomai/ui/KioskFrame.java)**: Full-screen kiosk terminal containing fuzzy search input, JComboBox selector, daily code input, clock actions, View Stats modal, and reactive Sync status/timer headers.
- **[AdminFrame.java](./src/main/java/org/groupfive/siomai/ui/AdminFrame.java)**: Dashboard holding tables for Employee Directory CRUD operations, Daily Code Center resets, and work hours text report scroll pane.

### 5. Custom Exceptions
- **[AppException.java](./src/main/java/org/groupfive/siomai/exception/AppException.java)**: Base unchecked application exception.
- **[EmployeeNotFoundException.java](./src/main/java/org/groupfive/siomai/exception/EmployeeNotFoundException.java)**: Thrown when search filters return no matching active employees.
- **[InvalidDailyCodeException.java](./src/main/java/org/groupfive/siomai/exception/InvalidDailyCodeException.java)**: Thrown when daily code input fails verification.

---

## 🗃️ Database Schema & Query Reference

### 1. Database Schema
The database uses a relational schema. Below is a layout of each table, matching both SQLite and MySQL definitions:

#### Table: `admins`
Stores administrative portal login credentials.
| Column | Type (MySQL / SQLite) | Key / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` / `INTEGER` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier for the administrator |
| `username` | `VARCHAR(50)` / `TEXT` | `UNIQUE NOT NULL` | Administrator login username |
| `password` | `VARCHAR(255)` / `TEXT` | `NOT NULL` | Text password |
| `full_name` | `VARCHAR(100)` / `TEXT` | `NOT NULL` | Full name of the administrator |

#### Table: `employees`
Contains all student trainees/employees under monitoring.
| Column | Type (MySQL / SQLite) | Key / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` / `INTEGER` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier for the employee |
| `employee_code` | `VARCHAR(20)` / `TEXT` | `UNIQUE NOT NULL` | Business/school code (e.g. `EMP-001`) |
| `full_name` | `VARCHAR(100)` / `TEXT` | `NOT NULL` | Full name of the employee |
| `department` | `VARCHAR(50)` / `TEXT` | `NOT NULL` | Assigned department |
| `is_active` | `BOOLEAN` / `INTEGER` | `DEFAULT TRUE / 1` | Status showing whether employee account is active |

#### Table: `daily_codes`
Stores the global daily validation verification codes required to clock-in/out or view stats.
| Column | Type (MySQL / SQLite) | Key / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` / `INTEGER` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier for the code record |
| `validation_code` | `VARCHAR(10)` / `TEXT` | `NOT NULL` | The 5-digit verification code string |
| `generated_date` | `DATE` / `TEXT` | `UNIQUE NOT NULL DEFAULT CURRENT_DATE` | The specific day the code is generated for |

#### Table: `attendance_logs`
Saves time card sessions showing daily clock-in and clock-out logs.
| Column | Type (MySQL / SQLite) | Key / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` / `INTEGER` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier for the attendance log |
| `employee_id` | `INT` / `INTEGER` | `NOT NULL`, `FOREIGN KEY REFERENCES employees(id) ON DELETE CASCADE` | Associated employee ID |
| `clock_in` | `TIMESTAMP` / `TEXT` | `NULL` | Timestamp when the employee clocked in |
| `clock_out` | `TIMESTAMP` / `TEXT` | `NULL` | Timestamp when the employee clocked out |
| `log_date` | `DATE` / `TEXT` | `NOT NULL DEFAULT CURRENT_DATE` | Calendar day of logging |
| `work_hours` | `DOUBLE` / `REAL` | `DEFAULT 0.0` | Total hours computed for the shift log session |

#### Table: `employee_daily_codes`
Tracks specific daily codes generated individually per-employee (used if an administrator manually resets a student's daily code).
| Column | Type (MySQL / SQLite) | Key / Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` / `INTEGER` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier for the override code |
| `employee_id` | `INT` / `INTEGER` | `NOT NULL`, `FOREIGN KEY REFERENCES employees(id) ON DELETE CASCADE` | Associated employee ID |
| `validation_code` | `VARCHAR(10)` / `TEXT` | `NOT NULL` | The custom verification code |
| `generated_date` | `DATE` / `TEXT` | `NOT NULL` | Target date for the override code |
| *Constraint* | `UNIQUE(employee_id, generated_date)` | Unique index combination | Ensures only one daily override code exists per day |

---

### 2. Backend SQL Query Catalog
All backend operations are defined in `DatabaseOperations` and implemented inside `DatabaseOperationsImpl`. The table below lists the SQL queries used and their matching Java method:

| Target Function | SQL Query | Purpose |
| :--- | :--- | :--- |
| `getAdminByUsername` | `SELECT * FROM admins WHERE username = ?` | Fetches admin account by username for portal login checks. |
| `addEmployee` | `INSERT INTO employees (employee_code, full_name, department, is_active) VALUES (?, ?, ?, ?)` | Saves a new employee record into the database. |
| `getAllEmployees` | `SELECT * FROM employees` | Fetches the complete list of employees to load in directories and dropdown selectors. |
| `getEmployeeById` | `SELECT * FROM employees WHERE id = ?` | Retrieves an employee by database primary key. |
| `getEmployeeByCode` | `SELECT * FROM employees WHERE employee_code = ?` | Retrieves an employee by their unique code identifier. |
| `searchEmployees` | `SELECT * FROM employees WHERE full_name LIKE ? OR employee_code LIKE ?` | Performs live fuzzy filtering searches. |
| `updateEmployee` | `UPDATE employees SET employee_code = ?, full_name = ?, department = ?, is_active = ? WHERE id = ?` | Persists edited employee details. |
| `deleteEmployee` | `DELETE FROM employees WHERE id = ?` | Removes employee from directory (deletes related logs/daily codes automatically). |
| `getDailyCode` | `SELECT validation_code FROM daily_codes WHERE generated_date = ?` | Retrieves the system validation code generated for a specific date. |
| `setDailyCode` (Check) | `SELECT id FROM daily_codes WHERE generated_date = ?` | Verifies if a global validation code already exists for today. |
| `setDailyCode` (Update) | `UPDATE daily_codes SET validation_code = ? WHERE generated_date = ?` | Overwrites the validation code for a specific date. |
| `setDailyCode` (Insert) | `INSERT INTO daily_codes (validation_code, generated_date) VALUES (?, ?)` | Inserts a new global validation code. |
| `getTodayAttendanceRecord` | `SELECT * FROM attendance_logs WHERE employee_id = ? AND log_date = ?` | Fetches today's log record for clock-in/out checks. |
| `addAttendanceRecord` | `INSERT INTO attendance_logs (employee_id, clock_in, clock_out, log_date, work_hours) VALUES (?, ?, ?, ?, ?)` | Starts a new attendance session (clock-in). |
| `updateAttendanceRecord` | `UPDATE attendance_logs SET clock_out = ?, work_hours = ? WHERE id = ?` | Updates and completes an attendance session (clock-out). |
| `getAttendanceLogsForDate` | `SELECT * FROM attendance_logs WHERE log_date = ?` | Compiles daily attendance report. |
| `getEmployeeDailyCode` | `SELECT validation_code FROM employee_daily_codes WHERE employee_id = ? AND generated_date = ?` | Checks if a custom override daily code exists for a student. |
| `setEmployeeDailyCode` (Check) | `SELECT id FROM employee_daily_codes WHERE employee_id = ? AND generated_date = ?` | Checks if a student override daily code exists. |
| `setEmployeeDailyCode` (Update) | `UPDATE employee_daily_codes SET validation_code = ? WHERE employee_id = ? AND generated_date = ?` | Overwrites a student's custom override daily code. |
| `setEmployeeDailyCode` (Insert) | `INSERT INTO employee_daily_codes (employee_id, validation_code, generated_date) VALUES (?, ?, ?)` | Inserts a new custom override daily code for a student. |
| `getAttendanceLogsForEmployee` | `SELECT * FROM attendance_logs WHERE employee_id = ? ORDER BY log_date DESC, clock_in DESC` | Gathers an employee's historic records to compute shift performance statistics. |

