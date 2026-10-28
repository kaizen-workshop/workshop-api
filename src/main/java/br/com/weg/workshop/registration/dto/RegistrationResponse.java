package br.com.weg.workshop.registration.dto;

import br.com.weg.workshop.registration.domain.Registration;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(UUID id, UUID userId, UUID workshopId, String status, String paymentStatus,
                                   Long waitingListPosition, Instant registeredAt, Instant cancelledAt,
                                   Instant createdAt, Instant updatedAt) {
    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(registration.getId(), registration.getUser().getId(), registration.getWorkshop().getId(),
                registration.getStatus().name(), registration.getPaymentStatus().name(), null, registration.getRegisteredAt(),
                registration.getCancelledAt(), registration.getCreatedAt(), registration.getUpdatedAt());
    }

    public static RegistrationResponse from(Registration registration, Long waitingListPosition) {
        RegistrationResponse response = from(registration);
        return new RegistrationResponse(response.id(), response.userId(), response.workshopId(), response.status(),
                response.paymentStatus(), waitingListPosition, response.registeredAt(), response.cancelledAt(),
                response.createdAt(), response.updatedAt());
    }
}
