package br.com.weg.workshop.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorFactory {

    public ApiErrorResponse create(
            HttpServletRequest request,
            HttpStatus status,
            ErrorCode code,
            String message,
            List<ApiFieldError> errors
    ) {
        return new ApiErrorResponse(Instant.now(), status.value(), code.name(), message, request.getRequestURI(), errors);
    }
}
