package br.com.weg.workshop.registration.dto;

import br.com.weg.workshop.registration.domain.Registration;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(UUID id, UUID userId, UUID workshopId, String status, String paymentStatus,
                                   Instant registeredAt, Instant cancelledAt, Instant createdAt, Instant updatedAt) {
    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(registration.getId(), registration.getUser().getId(), registration.getWorkshop().getId(),
                registration.getStatus().name(), registration.getPaymentStatus().name(), registration.getRegisteredAt(),
                registration.getCancelledAt(), registration.getCreatedAt(), registration.getUpdatedAt());
    }
}
