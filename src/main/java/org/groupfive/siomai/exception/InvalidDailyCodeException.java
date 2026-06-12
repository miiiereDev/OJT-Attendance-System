package org.groupfive.siomai.exception;

/**
 * Thrown when the user provides an invalid daily verification code.
 */
public class InvalidDailyCodeException extends AppException {
    public InvalidDailyCodeException(String message) {
        super(message);
    }
}
