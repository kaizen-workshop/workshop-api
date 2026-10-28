package br.com.weg.workshop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.weg.workshop.user.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "this-is-a-test-secret-with-at-least-thirty-two-bytes";

    @Test
    void createsAndParsesSignedAccessToken() {
        JwtService service = new JwtService(SECRET, 15);
        UUID userId = UUID.randomUUID();
        String token = service.createAccessToken(userId, Role.ADMIN, false, true, 3);

        assertThat(service.parse(token).getSubject()).isEqualTo(userId.toString());
        assertThat(service.parse(token).get("role", String.class)).isEqualTo("ADMIN");
        assertThat(service.parse(token).get("requiresOnboarding", Boolean.class)).isTrue();
        assertThat(service.parse(token).get("tokenVersion", Integer.class)).isEqualTo(3);
    }

    @Test
    void rejectsTamperedAccessToken() {
        JwtService service = new JwtService(SECRET, 15);
        String token = service.createAccessToken(UUID.randomUUID(), Role.ADMIN, false);

        assertThatThrownBy(() -> service.parse(token + "tampered")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsExpiredAccessToken() {
        JwtService service = new JwtService(SECRET, -1);
        String token = service.createAccessToken(UUID.randomUUID(), Role.ADMIN, false);

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(RuntimeException.class);
    }
}
