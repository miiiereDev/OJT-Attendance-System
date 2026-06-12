Rapid Development Plan: Java Attendance Monitoring System

This document maps out a phased, rapid-iteration development plan for a solo developer. It is designed to be easily parsed by an AI assistant or executed step-by-step using a CLI wrapper (like Antigravity).

🛠️ Tech Stack & Architecture

Language: Java (JDK 17 or higher)

Build Tool: Maven (Simpler to set up and manage dependencies via pom.xml)

Database: MySQL (Hosted on Aiven/Railway free tier) or SQLite (Local file fallback)

UI Framework: JOptionPane (Simple dialogs, compliant with the PDF requirements)

Installer: Inno Setup (To compile the JVM and JAR into a clean Windows installer)

🗄️ Step 1: Database Schema Integration (Do This First)

Run this SQL script on your database host. It supports the modified logic:

admins: Has credentials for secure login. Seeded with admin / admin123 (per guideline IV.1).

employees: No login credentials, just a searchable registry.

daily_codes: Stores the rolling validation code required to clock in.

attendance_logs: Connects employees to timestamps.

-- Drop tables if they exist to allow clean resets during testing
DROP TABLE IF EXISTS attendance_logs;
DROP TABLE IF EXISTS daily_codes;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS admins;

-- 1. Admin Accounts Table (Credentials-based)
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- Seed Default Admin Account (Required by Guideline IV.1)
INSERT INTO admins (username, password, full_name) 
VALUES ('admin', 'admin123', 'System Administrator');

-- 2. Registered Employees Table (Searchable, no credentials)
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE NOT NULL, -- e.g., "EMP-1001"
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Seed some test employees for quick verification
INSERT INTO employees (employee_code, full_name, department) VALUES
('EMP-001', 'Alice Smith', 'Engineering'),
('EMP-002', 'Bob Jones', 'Human Resources'),
('EMP-003', 'Charlie Brown', 'Design');

-- 3. Daily Verification Codes Table
CREATE TABLE daily_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    validation_code VARCHAR(10) NOT NULL,
    generated_date DATE UNIQUE NOT NULL DEFAULT (CURRENT_DATE)
);

-- Seed an initial code for today's testing
INSERT INTO daily_codes (validation_code, generated_date) 
VALUES ('12345', CURRENT_DATE)
ON DUPLICATE KEY UPDATE validation_code='12345';

-- 4. Attendance Logs Table (Connected to Employees)
CREATE TABLE attendance_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    clock_in TIMESTAMP NULL,
    clock_out TIMESTAMP NULL,
    log_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    work_hours DOUBLE DEFAULT 0.0,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);


🧬 Step 2: OOP Structural Design (The 5 Required Classes)

To fully satisfy Section V and VI of the "Final Project – Object-Oriented Programming with Database Integration.pdf" guidelines, we will use the following OOP structure:

Person (Abstract Class): Base class containing protected/private fields: id, fullName. It uses Encapsulation (getters/setters).

Admin (Subclass of Person): Extends Person. Adds username, password.

Employee (Subclass of Person): Extends Person. Adds employeeCode, department.

DatabaseConnector (Utility Class): Encapsulates JDBC drivers and handles connection states using clean standard try-catch blocks (Exception Handling).

AttendanceRecord (Domain Logic Class): Represents a log transaction. Contains helper methods to compute elapsed work hours using:


$$hours = \frac{\text{ClockOut Time} - \text{ClockIn Time}}{3.6 \times 10^6 \text{ ms}}$$

DatabaseOperations (Interface): Declares abstract CRUD signatures to achieve Abstraction.

AttendanceApp (Main Class): Drives the user menus using JOptionPane.

🚀 Step-by-Step Execution Plan

📍 Phase A: Infrastructure & Initialization

[ ] A1. Create a Maven project in IntelliJ.

[ ] A2. Configure pom.xml with dependencies for the MySQL JDBC driver (mysql-connector-j).

[ ] A3. Create the DatabaseConnector class. Test the connection in a quick main method to ensure it successfully talks to your tiny database host.

[ ] A4. Implement the custom exception classes (e.g., InvalidDailyCodeException, EmployeeNotFoundException) to maximize exception-handling grades.

📍 Phase B: Core OOP Domain Objects

[ ] B1. Implement Person abstract class with encapsulated variables.

[ ] B2. Implement Admin and Employee subclasses extending Person (Inheritance).

[ ] B3. Write overloaded constructors for database mapping vs. transient runtime instance creation (Polymorphism Overloading).

📍 Phase C: The Kiosk Flow (Employee Interaction)

[ ] C1. Implement the Search mechanism. The kiosk displays a prompt asking for the Employee's Name or Code.

[ ] C2. Query the database: SELECT * FROM employees WHERE full_name LIKE ? OR employee_code = ?.

[ ] C3. Display the matched employee(s) in a JOptionPane dropdown selector.

[ ] C4. Prompt for the Daily Code. Validate it against the daily_codes table for the current date.

[ ] C5. Handle Clock-In / Clock-Out logic:

If no log exists for the employee today $\rightarrow$ Create log with clock_in = NOW().

If clock-in exists but no clock-out $\rightarrow$ Update log with clock_out = NOW(), compute final decimal work_hours, and save.

If both exist $\rightarrow$ Alert employee they have completed their shifts for the day.

📍 Phase D: The Admin Panel (Admin Authentication & CRUD)

[ ] D1. Implement the Login Module. Validate the Admin against the admins table.

[ ] D2. Display the Admin Main Menu upon successful login:

Option 1: Add Employee (Validates fields, inserts record).

Option 2: View Employees (Outputs tabular text dynamically into a scrollable JOptionPane).

Option 3: Search Employee (Filters records by keyword).

Option 4: Update Employee Details (Confirms existence first, then modifies).

Option 5: Delete Employee (Asks for strict confirmation before executing cascade delete).

Option 6: Daily Validation Code Generator (Generates/updates a random 5-digit daily code for the current date).

Option 7: Generate Work Hours Report (Shows aggregated hours grouped by employee).

📍 Phase E: Polish, Packaging & Presentation Prep

[ ] E1. Double-check input validations (e.g., empty string prevention, numeric-only validation checks).

[ ] E2. Implement the Maven Shade/Assembly plugin to output a single, executable fat JAR.

[ ] E3. Load the JAR into Inno Setup to compile a clean desktop installation wizard.

[ ] E4. Record a swift screen walkthrough demonstrating each CRUD function to easily compile the required 5-10 minute presentation video.