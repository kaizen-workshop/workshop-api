package br.com.weg.workshop.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.payment.domain.*;
import br.com.weg.workshop.payment.gateway.PaymentGateway;
import br.com.weg.workshop.payment.repository.*;
import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.registration.service.RegistrationService;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.workshop.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository payments;
    @Mock PaymentEventRepository events;
    @Mock RegistrationRepository registrations;
    @Mock RegistrationService registrationService;
    @Mock PaymentGateway gateway;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, events, registrations, registrationService, gateway, "America/Sao_Paulo");
    }

    @Test
    void createsPendingPaymentIdempotently() {
        Registration registration = registration(false, LocalDate.now().plusDays(5));
        UUID key = UUID.randomUUID();
        when(registrations.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(payments.findByRegistrationId(registration.getId())).thenReturn(Optional.empty());
        when(gateway.createPayment(any())).thenReturn("simulated-reference");
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(registration.getUser().getId(), registration.getId(), key);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.externalReference()).isEqualTo("simulated-reference");
        verify(events).save(any(PaymentEvent.class));
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingPayment() {
        Registration registration = registration(false, LocalDate.now().plusDays(5));
        UUID key = UUID.randomUUID();
        Payment payment = Payment.create(registration, BigDecimal.TEN, PaymentMethod.PIX, "reference", key);
        when(registrations.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.of(payment));

        var response = service.create(registration.getUser().getId(), registration.getId(), key);

        assertThat(response.id()).isEqualTo(payment.getId());
        verifyNoInteractions(gateway);
    }

    @Test
    void successfulSimulationConfirmsRegistration() {
        Registration registration = registration(false, LocalDate.now().plusDays(5));
        Payment payment = Payment.create(registration, BigDecimal.TEN, PaymentMethod.PIX, "reference", UUID.randomUUID());
        when(payments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        service.simulatePaid(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(registration.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.PAID);
    }

    @Test
    void refundsPaidChampionshipEvenInsideFortyEightHours() {
        Registration registration = registration(true, LocalDate.now().plusDays(1));
        registration.cancel();
        Payment payment = paidPayment(registration);
        when(registrationService.cancelForPayment(registration.getUser().getId(), registration.getId())).thenReturn(registration);
        when(payments.findByRegistrationId(registration.getId())).thenReturn(Optional.of(payment));

        service.cancelRegistration(registration.getUser().getId(), registration.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(registration.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.REFUNDED);
        verify(gateway).refundPayment("reference", BigDecimal.TEN);
    }

    @Test
    void doesNotRefundRegularWorkshopInsideFortyEightHours() {
        Registration registration = registration(false, LocalDate.now().plusDays(1));
        registration.cancel();
        Payment payment = paidPayment(registration);
        when(registrationService.cancelForPayment(registration.getUser().getId(), registration.getId())).thenReturn(registration);
        when(payments.findByRegistrationId(registration.getId())).thenReturn(Optional.of(payment));

        service.cancelRegistration(registration.getUser().getId(), registration.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(registration.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.PAID);
        verify(gateway, never()).refundPayment(anyString(), any());
    }

    private Payment paidPayment(Registration registration) {
        Payment payment = Payment.create(registration, BigDecimal.TEN, PaymentMethod.PIX, "reference", UUID.randomUUID());
        payment.markPaid();
        return payment;
    }

    private Registration registration(boolean championship, LocalDate startDate) {
        UserEntity user = UserEntity.create("User", UUID.randomUUID().toString(), UUID.randomUUID() + "@example.com", null, null, null,
                "hash", Role.PARTICIPANT);
        user.changePassword("hash");
        UserEntity creator = UserEntity.create("Creator", UUID.randomUUID().toString(), UUID.randomUUID() + "@example.com", null, null, null,
                "hash", Role.ARWEG);
        Theme theme = Theme.create("Theme " + UUID.randomUUID(), null);
        Category category = Category.create("Category " + UUID.randomUUID(), null);
        Workshop workshop = Workshop.create(new WorkshopData("Workshop", "Description", null, startDate, startDate,
                LocalTime.now().plusHours(2).withNano(0), LocalTime.now().plusHours(3).withNano(0), "Room", WorkshopModality.IN_PERSON,
                BigDecimal.TEN, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 10, PaymentMethod.PIX, championship, null),
                theme, category, creator);
        workshop.publish();
        return Registration.create(user, workshop, RegistrationStatus.PENDING, RegistrationPaymentStatus.PENDING);
    }
}
