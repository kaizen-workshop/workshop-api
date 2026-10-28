package br.com.weg.workshop.payment.domain;

import br.com.weg.workshop.registration.domain.Registration;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.workshop.domain.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment", schema = "workshop")
public class Payment {

    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registration_id", nullable = false) private Registration registration;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentMethod method;
    @Column(nullable = false, unique = true) private String externalReference;
    @Column(nullable = false, unique = true) private UUID idempotencyKey;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Payment() { }

    public static Payment create(Registration registration, BigDecimal amount, PaymentMethod method,
                                 String externalReference, UUID idempotencyKey) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID(); payment.registration = registration; payment.amount = amount; payment.method = method;
        payment.status = PaymentStatus.PENDING; payment.externalReference = externalReference; payment.idempotencyKey = idempotencyKey;
        payment.createdAt = Instant.now(); payment.updatedAt = payment.createdAt;
        return payment;
    }

    public void markPaid() { transition(PaymentStatus.PAID); }
    public void markDeclined() { transition(PaymentStatus.DECLINED); }
    public void cancel() { transition(PaymentStatus.CANCELLED); }
    public void refund() { transition(PaymentStatus.REFUNDED); }
    private void transition(PaymentStatus target) {
        boolean valid = switch (status) {
            case PENDING -> target == PaymentStatus.PAID || target == PaymentStatus.DECLINED || target == PaymentStatus.CANCELLED;
            case PAID -> target == PaymentStatus.REFUNDED;
            default -> false;
        };
        if (!valid) throw new ConflictException("Invalid payment status transition.");
        status = target; updatedAt = Instant.now();
    }

    public UUID getId() { return id; } public Registration getRegistration() { return registration; }
    public BigDecimal getAmount() { return amount; } public PaymentStatus getStatus() { return status; }
    public PaymentMethod getMethod() { return method; } public String getExternalReference() { return externalReference; }
    public UUID getIdempotencyKey() { return idempotencyKey; } public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
