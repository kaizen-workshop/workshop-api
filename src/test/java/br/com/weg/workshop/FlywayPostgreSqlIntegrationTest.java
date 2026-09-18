package br.com.weg.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class FlywayPostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesWorkshopMigration() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from workshop.flyway_schema_history where version = '5' and success = true",
                Integer.class);

        assertThat(appliedMigrations).isEqualTo(1);
    }
}
