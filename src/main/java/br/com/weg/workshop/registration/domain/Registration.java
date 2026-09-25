package br.com.weg.workshop.registration.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.workshop.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration", schema = "workshop")
public class Registration {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationPaymentStatus paymentStatus;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    private Instant cancelledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Registration() {
    }

    public static Registration create(UserEntity user, Workshop workshop, RegistrationStatus status,
                                      RegistrationPaymentStatus paymentStatus) {
        Registration registration = new Registration();
        registration.id = UUID.randomUUID();
        registration.user = user;
        registration.workshop = workshop;
        registration.status = status;
        registration.paymentStatus = paymentStatus;
        registration.registeredAt = Instant.now();
        registration.createdAt = registration.registeredAt;
        registration.updatedAt = registration.registeredAt;
        return registration;
    }

    public void cancel() {
        status = RegistrationStatus.CANCELLED;
        paymentStatus = paymentStatus == RegistrationPaymentStatus.EXEMPT ? RegistrationPaymentStatus.EXEMPT
                : RegistrationPaymentStatus.CANCELLED;
        cancelledAt = Instant.now();
        updatedAt = cancelledAt;
    }

    public void promote(RegistrationStatus promotedStatus, RegistrationPaymentStatus promotedPaymentStatus) {
        status = promotedStatus;
        paymentStatus = promotedPaymentStatus;
        updatedAt = Instant.now();
    }

    public void confirmPayment() {
        status = RegistrationStatus.CONFIRMED;
        paymentStatus = RegistrationPaymentStatus.PAID;
        updatedAt = Instant.now();
    }

    public void markPaymentDeclined() {
        paymentStatus = RegistrationPaymentStatus.DECLINED;
        updatedAt = Instant.now();
    }

    public void markPaymentPaid() { paymentStatus = RegistrationPaymentStatus.PAID; updatedAt = Instant.now(); }
    public void markPaymentRefunded() { paymentStatus = RegistrationPaymentStatus.REFUNDED; updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public Workshop getWorkshop() { return workshop; }
    public RegistrationStatus getStatus() { return status; }
    public RegistrationPaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
