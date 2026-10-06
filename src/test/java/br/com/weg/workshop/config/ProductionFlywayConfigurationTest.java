package br.com.weg.workshop.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProductionFlywayConfigurationTest {

    @Test
    void productionDoesNotLoadDevelopmentDemoData() throws IOException {
        String production = new ClassPathResource("application-prod.yml")
                .getContentAsString(StandardCharsets.UTF_8);
        String development = new ClassPathResource("application-dev.yml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(production).contains("locations: classpath:db/migration").doesNotContain("classpath:db/dev");
        assertThat(development).contains("classpath:db/migration,classpath:db/dev");
        assertThat(Files.exists(Path.of("src/main/resources/db/migration/V20__seed_demo_data.sql"))).isFalse();
        assertThat(new ClassPathResource("db/dev/V20__seed_demo_data.sql").exists()).isTrue();
    }
}
