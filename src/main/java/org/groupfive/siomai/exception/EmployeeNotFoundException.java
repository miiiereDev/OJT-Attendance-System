package org.groupfive.siomai.exception;

/**
 * Thrown when an employee search yields no results.
 */
public class EmployeeNotFoundException extends AppException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
