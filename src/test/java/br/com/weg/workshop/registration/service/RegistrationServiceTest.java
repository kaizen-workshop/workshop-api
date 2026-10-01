package br.com.weg.workshop.registration.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.notification.service.NotificationService;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock RegistrationRepository registrations;
    @Mock WorkshopRepository workshops;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock EntityManager entityManager;
    @InjectMocks RegistrationService service;

    @Test
    void confirmsFreeRegistrationWhenThereIsCapacity() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        givenRegistrable(user, workshop, 0);
        when(registrations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(user.getId(), workshop.getId());

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.paymentStatus()).isEqualTo("EXEMPT");
        verify(workshops).findByIdForUpdate(workshop.getId());
    }

    @Test
    void placesUserInWaitingListWhenCapacityIsFull() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        givenRegistrable(user, workshop, 1);
        when(registrations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(user.getId(), workshop.getId());

        assertThat(response.status()).isEqualTo("WAITING_LIST");
        assertThat(response.paymentStatus()).isEqualTo("EXEMPT");
        assertThat(response.waitingListPosition()).isEqualTo(1);
    }

    @Test
    void returnsTheSameRegistrationForAnIdempotentRetry() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        UUID key = UUID.randomUUID();
        Registration existing = Registration.create(user, workshop, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT, key);
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(registrations.findByUserIdAndIdempotencyKey(user.getId(), key)).thenReturn(Optional.of(existing));

        var response = service.register(user.getId(), workshop.getId(), key);

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.waitingListPosition()).isNull();
        verifyNoInteractions(workshops);
        verify(registrations, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void rejectsAnIdempotencyKeyReusedForAnotherWorkshop() {
        UserEntity user = activeUser();
        Workshop original = publishedWorkshop(PaymentMethod.FREE, 1);
        Workshop requested = publishedWorkshop(PaymentMethod.FREE, 1);
        UUID key = UUID.randomUUID();
        Registration existing = Registration.create(user, original, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT, key);
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(registrations.findByUserIdAndIdempotencyKey(user.getId(), key)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(user.getId(), requested.getId(), key))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(workshops);
    }

    @Test
    void rejectsASecondValidRegistrationForTheSameWorkshop() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 2);
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(workshops.findByIdForUpdate(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.existsByUserIdAndWorkshopIdAndStatusIn(eq(user.getId()), eq(workshop.getId()), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.register(user.getId(), workshop.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellationPromotesTheFirstEligibleWaitingUser() {
        UserEntity owner = activeUser();
        UserEntity waitingUser = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        Registration confirmed = Registration.create(owner, workshop, RegistrationStatus.CONFIRMED, RegistrationPaymentStatus.EXEMPT);
        Registration waiting = Registration.create(waitingUser, workshop, RegistrationStatus.WAITING_LIST, RegistrationPaymentStatus.EXEMPT);
        when(registrations.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
        when(workshops.findByIdForUpdate(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.findFirstEligibleWaitingListEntry(workshop.getId())).thenReturn(Optional.of(waiting));

        var response = service.cancel(owner.getId(), confirmed.getId());

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(waiting.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(waiting.getPaymentStatus()).isEqualTo(RegistrationPaymentStatus.EXEMPT);
    }

    @Test
    void cancellationRetryReturnsExistingResultWithoutPromotingAgain() {
        UserEntity owner = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        Registration confirmed = Registration.create(owner, workshop, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT);
        UUID key = UUID.randomUUID();
        when(registrations.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
        when(workshops.findByIdForUpdate(workshop.getId())).thenReturn(Optional.of(workshop));

        var first = service.cancelForPayment(owner.getId(), confirmed.getId(), key);
        var retry = service.cancelForPayment(owner.getId(), confirmed.getId(), key);

        assertThat(first.replayed()).isFalse();
        assertThat(retry.replayed()).isTrue();
        assertThat(retry.registration().getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        verify(registrations, times(1)).findFirstEligibleWaitingListEntry(workshop.getId());
    }

    @Test
    void rejectsCancellationKeyUsedForAnotherRegistration() {
        UserEntity owner = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 2);
        Registration confirmed = Registration.create(owner, workshop, RegistrationStatus.CONFIRMED,
                RegistrationPaymentStatus.EXEMPT);
        UUID key = UUID.randomUUID();
        when(registrations.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
        when(workshops.findByIdForUpdate(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.existsByUserIdAndCancellationIdempotencyKey(owner.getId(), key)).thenReturn(true);

        assertThatThrownBy(() -> service.cancelForPayment(owner.getId(), confirmed.getId(), key))
                .isInstanceOf(ConflictException.class);
        assertThat(confirmed.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    void returnsTheUsersLatestRegistrationForAWorkshop() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        Registration registration = Registration.create(user, workshop, RegistrationStatus.CANCELLED,
                RegistrationPaymentStatus.CANCELLED);
        when(registrations.findFirstByUserIdAndWorkshopIdOrderByCreatedAtDesc(user.getId(), workshop.getId()))
                .thenReturn(Optional.of(registration));

        assertThat(service.currentForWorkshop(user.getId(), workshop.getId()).id()).isEqualTo(registration.getId());
    }

    @Test
    void requiresAnActiveUser() {
        UserEntity user = UserEntity.create("User", "user", "user@example.com", null, null, null, "hash", Role.PARTICIPANT);
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.register(user.getId(), workshop.getId()))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(workshops);
    }

    private void givenRegistrable(UserEntity user, Workshop workshop, long occupied) {
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(workshops.findByIdForUpdate(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.existsByUserIdAndWorkshopIdAndStatusIn(eq(user.getId()), eq(workshop.getId()), anyCollection())).thenReturn(false);
        when(registrations.countByWorkshopIdAndStatusIn(eq(workshop.getId()), anyCollection())).thenReturn(occupied);
    }

    private UserEntity activeUser() {
        UserEntity user = UserEntity.create("User", UUID.randomUUID().toString(), UUID.randomUUID() + "@example.com", null, null, null, "hash", Role.PARTICIPANT);
        user.changePassword("hash");
        return user;
    }

    private Workshop publishedWorkshop(PaymentMethod paymentMethod, int capacity) {
        UserEntity creator = activeUser();
        Theme theme = Theme.create("Theme " + UUID.randomUUID(), null);
        Category category = Category.create("Category " + UUID.randomUUID(), null);
        Workshop workshop = Workshop.create(new WorkshopData("Workshop", "Description", null, LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON,
                BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), capacity, paymentMethod, false, null),
                theme, category, creator);
        workshop.publish();
        return workshop;
    }
}
