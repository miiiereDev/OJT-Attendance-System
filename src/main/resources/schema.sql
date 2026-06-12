-- Create Database
CREATE DATABASE IF NOT EXISTS siomai_db;
USE siomai_db;

-- Table 1: Admins (System Administrators)
CREATE TABLE IF NOT EXISTS admins (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Table 2: Employees (Searchable/Verifiable via Kiosk Screen)
CREATE TABLE IF NOT EXISTS employees (
                                         id INT AUTO_INCREMENT PRIMARY KEY,
                                         employee_code VARCHAR(20) UNIQUE NOT NULL, -- e.g. "EMP-2026-001"
    full_name VARCHAR(100) NOT NULL,
    active_daily_code VARCHAR(10) NULL,        -- Assigned by admin, resets/needed daily
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Table 3: Attendance Logs (Saves timestamps and calculated work hours)
CREATE TABLE IF NOT EXISTS attendance_logs (
                                               id INT AUTO_INCREMENT PRIMARY KEY,
                                               employee_id INT NOT NULL,
                                               clock_in TIMESTAMP NULL,
                                               clock_out TIMESTAMP NULL,
                                               log_date DATE NOT NULL,
                                               work_hours DOUBLE DEFAULT 0.0,
                                               FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
    );

-- Seed Default Admin Account (Required by Guideline IV.1)
INSERT INTO admins (username, password, full_name)
VALUES ('admin', 'admin123', 'System Administrator')
    ON DUPLICATE KEY UPDATE username=username;

-- Seed Sample Employees for Kiosk testing
INSERT INTO employees (employee_code, full_name, active_daily_code) VALUES ('EMP-1001', 'Juan Dela Cruz', '12345'), ('EMP-1002', 'Maria Clara', '54321');