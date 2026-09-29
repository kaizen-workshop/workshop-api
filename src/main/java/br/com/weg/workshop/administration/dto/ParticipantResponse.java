package br.com.weg.workshop.administration.dto;

import br.com.weg.workshop.registration.domain.Registration;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
        UUID registrationId,
        UUID userId,
        String name,
        String email,
        String wegRegistration,
        String registrationStatus,
        String paymentStatus,
        String attendanceStatus,
        Instant registeredAt,
        Instant attendanceMarkedAt,
        UUID attendanceMarkedBy
) {
    public static ParticipantResponse from(Registration registration) {
        return new ParticipantResponse(
                registration.getId(),
                registration.getUser().getId(),
                registration.getUser().getName(),
                registration.getUser().getEmail(),
                registration.getUser().getWegRegistration(),
                registration.getStatus().name(),
                registration.getPaymentStatus().name(),
                registration.getAttendanceStatus() == null ? null : registration.getAttendanceStatus().name(),
                registration.getRegisteredAt(),
                registration.getAttendanceMarkedAt(),
                registration.getAttendanceMarkedBy() == null ? null : registration.getAttendanceMarkedBy().getId()
        );
    }
}
