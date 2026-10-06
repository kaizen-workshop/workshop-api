package br.com.weg.workshop.evaluation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.evaluation.dto.CreateEvaluationRequest;
import br.com.weg.workshop.evaluation.domain.Evaluation;
import br.com.weg.workshop.evaluation.repository.EvaluationRepository;
import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
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
class EvaluationServiceTest {
    @Mock EvaluationRepository evaluations;
    @Mock RegistrationRepository registrations;
    @Mock UserRepository users;
    @Mock WorkshopRepository workshops;
    @InjectMocks EvaluationService service;

    @Test
    void createsEvaluationForConfirmedParticipantAfterCompletion() {
        UserEntity user = activeUser(); Workshop workshop = completedWorkshop();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.existsByUserIdAndWorkshopIdAndStatusIn(eq(user.getId()), eq(workshop.getId()), anyCollection())).thenReturn(true);
        when(evaluations.existsByUserIdAndWorkshopId(user.getId(), workshop.getId())).thenReturn(false);
        when(evaluations.save(any())).thenAnswer(call -> call.getArgument(0));

        var response = service.create(user.getId(), workshop.getId(), request());

        assertThat(response.workshopId()).isEqualTo(workshop.getId());
        assertThat(response.rating()).isEqualTo((short) 5);
    }

    @Test
    void rejectsSecondEvaluationForSameWorkshop() {
        UserEntity user = activeUser(); Workshop workshop = completedWorkshop();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));
        when(registrations.existsByUserIdAndWorkshopIdAndStatusIn(eq(user.getId()), eq(workshop.getId()), anyCollection())).thenReturn(true);
        when(evaluations.existsByUserIdAndWorkshopId(user.getId(), workshop.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create(user.getId(), workshop.getId(), request())).isInstanceOf(ConflictException.class);
        verify(evaluations, never()).save(any());
    }

    @Test
    void replaysAnEvaluationWithTheSameIdempotencyKey() {
        UserEntity user = activeUser(); Workshop workshop = completedWorkshop(); UUID key = UUID.randomUUID();
        Evaluation evaluation = Evaluation.create(user, workshop, (short) 5, "Great", (short) 5,
                (short) 4, (short) 5, key);
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(evaluations.findByUserIdAndClientOperationId(user.getId(), key)).thenReturn(Optional.of(evaluation));

        var response = service.create(user.getId(), workshop.getId(), key, request());

        assertThat(response.id()).isEqualTo(evaluation.getId());
        verify(evaluations, never()).save(any());
    }

    private CreateEvaluationRequest request() { return new CreateEvaluationRequest((short) 5, "Great", (short) 5, (short) 4, (short) 5); }
    private UserEntity activeUser() { UserEntity user = UserEntity.create("User", UUID.randomUUID().toString(), UUID.randomUUID() + "@example.com", null, null, null, "hash", Role.PARTICIPANT); user.changePassword("hash"); return user; }
    private Workshop completedWorkshop() {
        UserEntity creator = activeUser(); Theme theme = Theme.create("Theme " + UUID.randomUUID(), null); Category category = Category.create("Category " + UUID.randomUUID(), null);
        Workshop workshop = Workshop.create(new WorkshopData("Workshop", "Description", null, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON, BigDecimal.ZERO, Instant.now().minusSeconds(300), Instant.now().minusSeconds(60), 10, PaymentMethod.FREE, false, null), theme, category, creator);
        workshop.publish(); return workshop;
    }
}
