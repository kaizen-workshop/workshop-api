package br.com.weg.workshop.shared.error;

public class ConflictException extends RuntimeException {

    private final ErrorCode code;

    public ConflictException(String message) {
        this(message, ErrorCode.CONFLICT);
    }

    /** A conflict clients can tell apart by a stable {@link ErrorCode}; the HTTP status stays 409. */
    public ConflictException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
