package br.com.weg.workshop.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.group.repository.WorkshopGroupRepository;
import br.com.weg.workshop.preference.domain.Category;
import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.registration.domain.RegistrationPaymentStatus;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.workshop.domain.PaymentMethod;
import br.com.weg.workshop.workshop.domain.Workshop;
import br.com.weg.workshop.workshop.domain.WorkshopData;
import br.com.weg.workshop.workshop.domain.WorkshopModality;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private WorkshopGroupRepository groups;

    @Mock
    private RegistrationRepository registrations;

    @InjectMocks
    private GroupService service;

    @Test
    void permitsAConfirmedParticipantWithSettledPayment() {
        UserEntity participant = user("participant", Role.PARTICIPANT);
        WorkshopGroup group = group();
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));
        when(registrations.existsByUserIdAndWorkshopIdAndStatusAndPaymentStatusIn(
                participant.getId(),
                group.getWorkshop().getId(),
                RegistrationStatus.CONFIRMED,
                java.util.EnumSet.of(RegistrationPaymentStatus.PAID, RegistrationPaymentStatus.EXEMPT)
        )).thenReturn(true);

        assertThat(service.get(participant.getId(), false, group.getId()).id()).isEqualTo(group.getId());
    }

    @Test
    void hidesAGroupFromAUserWithoutValidMembership() {
        UserEntity participant = user("participant", Role.PARTICIPANT);
        WorkshopGroup group = group();
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.get(participant.getId(), false, group.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void permitsTheWorkshopOwnerToModerateWithoutARegistration() {
        WorkshopGroup group = group();
        UUID ownerId = group.getWorkshop().getCreatedBy().getId();
        when(groups.findById(group.getId())).thenReturn(Optional.of(group));

        var response = service.get(ownerId, false, group.getId());
        assertThat(response.canModerate()).isTrue();
        assertThat(response.canSendMessages()).isFalse();
    }

    private WorkshopGroup group() {
        UserEntity owner = user("owner", Role.ARWEG);
        Theme theme = Theme.create("Java", null);
        Category category = Category.create("Technology", null);
        Workshop workshop = Workshop.create(
                new WorkshopData(
                        "Spring", "Workshop", null, LocalDate.now(), LocalDate.now(),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON,
                        BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60),
                        10, PaymentMethod.FREE, false, null
                ),
                theme,
                category,
                owner
        );
        return WorkshopGroup.create(workshop);
    }

    private UserEntity user(String username, Role role) {
        return UserEntity.create(
                username,
                username,
                username + "@example.com",
                null,
                null,
                null,
                "hash",
                role
        );
    }
}
