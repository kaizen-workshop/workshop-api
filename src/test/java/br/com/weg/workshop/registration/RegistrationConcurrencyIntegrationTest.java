package br.com.weg.workshop.registration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.preference.repository.*;
import br.com.weg.workshop.registration.dto.RegistrationResponse;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.registration.service.RegistrationService;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RegistrationConcurrencyIntegrationTest {

    @Autowired RegistrationService service;
    @Autowired RegistrationRepository registrations;
    @Autowired WorkshopRepository workshops;
    @Autowired UserRepository users;
    @Autowired ThemeRepository themes;
    @Autowired CategoryRepository categories;

    @Test
    void allowsOnlyOneRegistrationToOccupyTheLastVacancyUnderConcurrency() throws Exception {
        UserEntity creator = saveActiveUser();
        UserEntity firstUser = saveActiveUser();
        UserEntity secondUser = saveActiveUser();
        Theme theme = themes.save(Theme.create("Theme " + UUID.randomUUID(), null));
        Category category = categories.save(Category.create("Category " + UUID.randomUUID(), null));
        Workshop workshop = Workshop.create(new WorkshopData("Concurrent workshop", "Description", null,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(10, 0),
                "Room", WorkshopModality.IN_PERSON, BigDecimal.ZERO, Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600), 1, PaymentMethod.FREE, null), theme, category, creator);
        workshop.publish();
        workshops.saveAndFlush(workshop);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<RegistrationResponse> first = executor.submit(() -> registerWhenReleased(firstUser.getId(), workshop.getId(), ready, start));
            Future<RegistrationResponse> second = executor.submit(() -> registerWhenReleased(secondUser.getId(), workshop.getId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<RegistrationResponse> responses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(responses).extracting(RegistrationResponse::status)
                    .containsExactlyInAnyOrder("CONFIRMED", "WAITING_LIST");
            assertThat(registrations.countByWorkshopIdAndStatusIn(workshop.getId(),
                    Set.of(br.com.weg.workshop.registration.domain.RegistrationStatus.CONFIRMED,
                            br.com.weg.workshop.registration.domain.RegistrationStatus.PENDING))).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private RegistrationResponse registerWhenReleased(UUID userId, UUID workshopId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent registration did not start.");
        }
        return service.register(userId, workshopId);
    }

    private UserEntity saveActiveUser() {
        String suffix = UUID.randomUUID().toString();
        UserEntity user = UserEntity.create("User", "user-" + suffix, suffix + "@example.com", null, null, null,
                "hash", Role.PARTICIPANT);
        user.changePassword("hash");
        return users.save(user);
    }
}
