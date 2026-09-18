package br.com.weg.workshop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.auth.domain.RefreshToken;
import br.com.weg.workshop.auth.dto.*;
import br.com.weg.workshop.auth.repository.PasswordResetTokenRepository;
import br.com.weg.workshop.auth.repository.RefreshTokenRepository;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwt;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordResetTokenRepository resetTokens;
    @Mock private OpaqueTokenService opaque;
    @Mock private PasswordResetMailService resetMail;
    @InjectMocks private AuthenticationService service;

    @Test
    void loginByEmailIssuesAccessAndRefreshTokens() {
        UserEntity user = user();
        when(users.findByUsername("person@example.com")).thenReturn(Optional.empty());
        when(users.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(opaque.create()).thenReturn("refresh-token");
        when(opaque.hash("refresh-token")).thenReturn("refresh-hash");
        when(jwt.createAccessToken(user.getId(), user.getRole(), true)).thenReturn("access-token");

        TokenResponse result = service.login(new LoginRequest("person@example.com", "password"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.mustChangePassword()).isTrue();
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void refreshRotatesAValidRefreshToken() {
        UserEntity user = user();
        RefreshToken existing = RefreshToken.create(user, "old-hash", Instant.now().plusSeconds(60));
        when(opaque.hash("old-token")).thenReturn("old-hash");
        when(refreshTokens.findByTokenHash("old-hash")).thenReturn(Optional.of(existing));
        when(opaque.create()).thenReturn("new-token");
        when(opaque.hash("new-token")).thenReturn("new-hash");
        when(jwt.createAccessToken(user.getId(), user.getRole(), true)).thenReturn("access-token");

        TokenResponse result = service.refresh(new RefreshTokenRequest("old-token"));

        assertThat(result.refreshToken()).isEqualTo("new-token");
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset(new ForgotPasswordRequest("missing@example.com"));

        verifyNoInteractions(resetTokens, resetMail);
    }

    @Test
    void blockedUserCannotLogin() {
        UserEntity user = user();
        user.block();
        when(users.findByUsername("person")).thenReturn(Optional.of(user));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.login(new LoginRequest("person", "password")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(refreshTokens);
    }

    private UserEntity user() {
        return UserEntity.create("Person", "person", "person@example.com", null, null, null, "hash", Role.PARTICIPANT);
    }
}
