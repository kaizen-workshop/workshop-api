package br.com.weg.workshop.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.evaluation.repository.EvaluationRepository;
import java.util.UUID;
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
class EvaluationSummaryQueryIntegrationTest {

    @Autowired EvaluationRepository evaluations;

    @Test
    void aggregateReturnsOneFiveColumnRowEvenWithoutEvaluations() {
        // The service reads the count at index 4. A bare Object[] return type made Spring
        // wrap the row in another array, so that read failed with HTTP 500.
        var rows = evaluations.summarize(UUID.randomUUID());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).hasSize(5);
        assertThat(((Number) rows.get(0)[4]).longValue()).isZero();
    }
}
