package br.com.weg.workshop.administration.dto;

import br.com.weg.workshop.registration.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttendanceUpdateRequest(
        @NotNull UUID registrationId,
        @NotNull AttendanceStatus status
) {
}
