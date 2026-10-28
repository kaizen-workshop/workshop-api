package br.com.weg.workshop.auth.service;

import br.com.weg.workshop.TestcontainersConfiguration;
import br.com.weg.workshop.auth.domain.RefreshToken;
import br.com.weg.workshop.auth.dto.RefreshTokenRequest;
import br.com.weg.workshop.auth.repository.RefreshTokenRepository;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RefreshConcurrencyIntegrationTest {
    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository tokens;
    @Autowired private OpaqueTokenService opaque;
    @Autowired private AuthenticationService service;

    @Test void onlyOneConcurrentRequestCanConsumeRefreshToken() throws Exception {
        String unique = UUID.randomUUID().toString();
        var user = UserEntity.create("Person", unique, unique + "@example.com", null, null, null, "hash", Role.PARTICIPANT);
        user.changePassword("active-hash");
        users.saveAndFlush(user);
        String raw = opaque.create();
        tokens.saveAndFlush(RefreshToken.create(user, opaque.hash(raw), Instant.now().plusSeconds(60)));
        var executor = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var results = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < 4; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        service.refresh(new RefreshTokenRequest(raw));
                        return true;
                    } catch (BadCredentialsException expected) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(30, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
