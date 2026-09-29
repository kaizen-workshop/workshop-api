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
                "select count(*) from workshop.flyway_schema_history where version = '14' and success = true",
                Integer.class);

        assertThat(appliedMigrations).isEqualTo(1);
        Integer attendanceColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and table_name = 'registration' and column_name in "
                        + "('attendance_status', 'attendance_marked_at', 'attendance_marked_by')",
                Integer.class);
        assertThat(attendanceColumns).isEqualTo(3);
        Integer idempotencyColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'workshop' "
                        + "and table_name = 'registration' and column_name = 'idempotency_key' and is_nullable = 'NO'",
                Integer.class);
        assertThat(idempotencyColumns).isEqualTo(1);
    }
}
