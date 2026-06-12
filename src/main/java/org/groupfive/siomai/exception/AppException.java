package org.groupfive.siomai.exception;

/**
 * Base custom exception for the Attendance Monitoring System.
 */
public class AppException extends Exception {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
