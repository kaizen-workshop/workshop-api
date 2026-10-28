package br.com.weg.workshop.post;

import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.post.repository.PostRepository;
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
class PostManagedQueryIntegrationTest {

    @Autowired PostRepository posts;

    @Test
    void listsManagedPostsForAuthorsAndAdmins() {
        var page = PageRequest.of(0, 20);
        assertThatCode(() -> {
            posts.findManaged(UUID.randomUUID(), false, page);
            posts.findManaged(UUID.randomUUID(), true, page);
        }).doesNotThrowAnyException();
    }
}
