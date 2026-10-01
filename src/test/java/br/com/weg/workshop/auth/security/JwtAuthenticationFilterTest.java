package br.com.weg.workshop.auth.security;

import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.shared.error.ApiErrorFactory;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users,
            new ApiErrorFactory(), new ObjectMapper().findAndRegisterModules());

    @Test void rejectsBlockedAccountWithPreviouslyValidToken() throws Exception {
        var user = account();
        user.block();
        assertRejected(user, user.getTokenVersion());
    }

    @Test void rejectsTokenIssuedBeforePasswordChange() throws Exception {
        var user = account();
        user.changePassword("new-hash");
        assertRejected(user, 0);
    }

    @Test void applicationFailureIsNotReportedAsInvalidToken() throws Exception {
        var user = account();
        user.changePassword("new-hash");
        prepare(user, user.getTokenVersion());
        assertThatThrownBy(() -> filter.doFilter(request(), new MockHttpServletResponse(),
                (req, res) -> { throw new IllegalArgumentException("application failure"); }))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("application failure");
    }

    private void assertRejected(UserEntity user, int version) throws Exception {
        prepare(user, version);
        var response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (req, res) -> { throw new AssertionError("Must reject before controller"); });
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED").doesNotContain("hash", "tokenVersion");
    }

    private void prepare(UserEntity user, int version) {
        when(jwt.parse("token")).thenReturn(Jwts.claims().subject(user.getId().toString())
                .add("role", "PARTICIPANT").add("tokenVersion", version).build());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private UserEntity account() {
        return UserEntity.create("Person", "person", "person@example.com", null, null, null, "hash", Role.PARTICIPANT);
    }
}
