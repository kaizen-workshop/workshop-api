package br.com.weg.workshop.registration;

import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RegistrationCalendarQueryIntegrationTest {

    @Autowired RegistrationRepository registrations;

    @Test
    void acceptsEveryCombinationOfOptionalCalendarBounds() {
        var statuses = EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED);
        var userId = UUID.randomUUID();
        var page = PageRequest.of(0, 20);
        var from = LocalDate.of(2026, 10, 1);
        var to = LocalDate.of(2026, 10, 31);

        // PostgreSQL cannot infer the type of an untyped null date parameter, so every
        // combination must plan and execute, not only the unbounded one.
        assertThatCode(() -> {
            registrations.findCalendarByUser(userId, statuses, null, null, page);
            registrations.findCalendarByUser(userId, statuses, from, null, page);
            registrations.findCalendarByUser(userId, statuses, null, to, page);
            registrations.findCalendarByUser(userId, statuses, from, to, page);
        }).doesNotThrowAnyException();
    }
}
