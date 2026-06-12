package org.groupfive.siomai.model;

import java.sql.Timestamp;
import java.sql.Date;

/**
 * Domain Logic Class representing an attendance log transaction.
 * Contains helper method to compute elapsed work hours.
 */
public class AttendanceRecord {
    private int id;
    private int employeeId;
    private Timestamp clockIn;
    private Timestamp clockOut;
    private Date logDate;
    private double workHours;

    public AttendanceRecord() {
    }

    public AttendanceRecord(int id, int employeeId, Timestamp clockIn, Timestamp clockOut, Date logDate, double workHours) {
        this.id = id;
        this.employeeId = employeeId;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.logDate = logDate;
        this.workHours = workHours;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Timestamp getClockIn() {
        return clockIn;
    }

    public void setClockIn(Timestamp clockIn) {
        this.clockIn = clockIn;
    }

    public Timestamp getClockOut() {
        return clockOut;
    }

    public void setClockOut(Timestamp clockOut) {
        this.clockOut = clockOut;
        // Automatically compute work hours if both timestamps are present
        this.workHours = calculateWorkHours();
    }

    public Date getLogDate() {
        return logDate;
    }

    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

    public double getWorkHours() {
        return workHours;
    }

    public void setWorkHours(double workHours) {
        this.workHours = workHours;
    }

    /**
     * Computes the elapsed work hours using:
     * hours = (ClockOut Time - ClockIn Time) / (3.6 * 10^6 ms)
     */
    public double calculateWorkHours() {
        if (clockIn == null || clockOut == null) {
            return 0.0;
        }
        long diff = clockOut.getTime() - clockIn.getTime();
        if (diff <= 0) {
            return 0.0;
        }
        // Round to 2 decimal places for clean storage/reporting
        double calculated = diff / 3.6e6;
        return Math.round(calculated * 100.0) / 100.0;
    }
}
