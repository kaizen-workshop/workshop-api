package br.com.weg.workshop.evaluation.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.evaluation.domain.ParticipantWorkshopFilter;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ParticipantWorkshopServiceTest {
    @Mock private RegistrationRepository registrations;
    @InjectMocks private ParticipantWorkshopService service;

    @Test
    void selectsWaitingListHistoryWithoutMixingOtherStatuses() {
        UUID userId = UUID.randomUUID();
        PageRequest page = PageRequest.of(0, 20);
        when(registrations.findByUserIdAndStatus(userId, RegistrationStatus.WAITING_LIST, page))
                .thenReturn(Page.empty(page));

        service.history(userId, ParticipantWorkshopFilter.WAITING_LIST, page);

        verify(registrations).findByUserIdAndStatus(userId, RegistrationStatus.WAITING_LIST, page);
    }

    @Test
    void selectsInProgressHistoryUsingTheCurrentDate() {
        UUID userId = UUID.randomUUID();
        PageRequest page = PageRequest.of(0, 20);
        when(registrations.findInProgressByUser(eq(userId), any(LocalDate.class), eq(page)))
                .thenReturn(Page.empty(page));

        service.history(userId, ParticipantWorkshopFilter.IN_PROGRESS, page);

        verify(registrations).findInProgressByUser(eq(userId), any(LocalDate.class), eq(page));
    }
}
