package br.com.weg.workshop.administration.dto;

import br.com.weg.workshop.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A payment of a managed workshop, with the participant it belongs to. */
public record WorkshopPaymentResponse(
        UUID paymentId,
        UUID registrationId,
        String participantName,
        String participantEmail,
        BigDecimal amount,
        String status,
        String method,
        Instant createdAt
) {
    public static WorkshopPaymentResponse from(Payment payment) {
        var registration = payment.getRegistration();
        return new WorkshopPaymentResponse(
                payment.getId(),
                registration.getId(),
                registration.getUser().getName(),
                registration.getUser().getEmail(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getMethod().name(),
                payment.getCreatedAt()
        );
    }
}
