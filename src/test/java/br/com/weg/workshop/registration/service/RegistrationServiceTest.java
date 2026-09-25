package br.com.weg.workshop.registration.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.registration.domain.*;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
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
    }

    @Test
    void rejectsASecondValidRegistrationForTheSameWorkshop() {
        UserEntity user = activeUser();
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 2);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
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
    void requiresAnActiveUser() {
        UserEntity user = UserEntity.create("User", "user", "user@example.com", null, null, null, "hash", Role.PARTICIPANT);
        Workshop workshop = publishedWorkshop(PaymentMethod.FREE, 1);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.register(user.getId(), workshop.getId()))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(workshops);
    }

    private void givenRegistrable(UserEntity user, Workshop workshop, long occupied) {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
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
                BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), capacity, paymentMethod, null),
                theme, category, creator);
        workshop.publish();
        return workshop;
    }
}
