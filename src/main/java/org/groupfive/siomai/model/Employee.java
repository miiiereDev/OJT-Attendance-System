package org.groupfive.siomai.model;

/**
 * Subclass of Person representing an Employee.
 * Showcases Inheritance.
 */
public class Employee extends Person {
    private String employeeCode;
    private String department;
    private boolean isActive = true;

    public Employee() {
        super();
    }

    public Employee(int id, String fullName, String employeeCode, String department, boolean isActive) {
        super(id, fullName);
        this.employeeCode = employeeCode;
        this.department = department;
        this.isActive = isActive;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
