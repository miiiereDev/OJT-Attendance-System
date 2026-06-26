-- OJT Attendance System Database Schema (MySQL)
-- Database Name: siomai

CREATE DATABASE IF NOT EXISTS siomai;
USE siomai;

-- 1. Admins Table
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- Seed Default Admin (only if empty)
INSERT IGNORE INTO admins (id, username, password, full_name) 
VALUES (1, 'admin', 'admin123', 'System Administrator');

-- 2. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Seed Initial Test Employees (only if empty)
INSERT IGNORE INTO employees (id, employee_code, full_name, department) VALUES 
(1, 'EMP-001', 'Alice Smith', 'Engineering'),
(2, 'EMP-002', 'Bob Jones', 'Human Resources'),
(3, 'EMP-003', 'Charlie Brown', 'Design');

-- 3. Daily Verification Codes Table
CREATE TABLE IF NOT EXISTS daily_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    validation_code VARCHAR(10) NOT NULL,
    generated_date DATE UNIQUE NOT NULL DEFAULT (CURRENT_DATE)
);

-- Seed initial verification code for today
INSERT IGNORE INTO daily_codes (id, validation_code, generated_date) 
VALUES (1, '12345', CURRENT_DATE);

-- 4. Attendance Logs Table
CREATE TABLE IF NOT EXISTS attendance_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    clock_in TIMESTAMP NULL,
    clock_out TIMESTAMP NULL,
    log_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    work_hours DOUBLE DEFAULT 0.0,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 5. Employee Daily Codes Table
CREATE TABLE IF NOT EXISTS employee_daily_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    validation_code VARCHAR(10) NOT NULL,
    generated_date DATE NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE(employee_id, generated_date)
);
