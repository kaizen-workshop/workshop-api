package br.com.weg.workshop.auth.service;

/**
 * The authenticated user typed a wrong current password. It is a validation failure, not an authentication
 * failure: answering 401 would make clients treat a valid session as expired.
 */
public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
        super("Current password is incorrect.");
    }
}
