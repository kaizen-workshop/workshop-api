package br.com.weg.workshop.chat;

import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.chat.repository.MessageRepository;
import java.time.Instant;
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
class MessageCursorQueryIntegrationTest {

    @Autowired MessageRepository messages;

    @Test
    void pagesWithAndWithoutACursor() {
        var groupId = UUID.randomUUID();
        var page = PageRequest.of(0, 21);

        // The first page sends no cursor. PostgreSQL cannot infer the type of an untyped
        // null timestamp parameter, which made opening any chat fail with HTTP 500.
        assertThatCode(() -> {
            messages.findPage(groupId, null, null, page);
            messages.findPage(groupId, Instant.now(), UUID.randomUUID(), page);
        }).doesNotThrowAnyException();
    }
}
