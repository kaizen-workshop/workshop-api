package br.com.weg.workshop.payment.dto;

import br.com.weg.workshop.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID registrationId, BigDecimal amount, String status, String method,
                              String externalReference, Instant createdAt, Instant updatedAt) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getRegistration().getId(), payment.getAmount(), payment.getStatus().name(),
                payment.getMethod().name(), payment.getExternalReference(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
