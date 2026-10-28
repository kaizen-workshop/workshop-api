package br.com.weg.workshop.auth.service;

import br.com.weg.workshop.auth.domain.*;
import br.com.weg.workshop.auth.dto.*;
import br.com.weg.workshop.auth.repository.*;
import br.com.weg.workshop.preference.repository.UserThemeRepository;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.domain.UserStatus;
import br.com.weg.workshop.user.repository.UserRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.of(30, ChronoUnit.DAYS);
    private static final Duration RESET_TOKEN_VALIDITY = Duration.of(1, ChronoUnit.HOURS);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final OpaqueTokenService opaque;
    private final PasswordResetMailService resetMail;
    private final UserThemeRepository preferences;

    public AuthenticationService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                                 RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
                                 OpaqueTokenService opaque, PasswordResetMailService resetMail,
                                 UserThemeRepository preferences) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.opaque = opaque;
        this.resetMail = resetMail;
        this.preferences = preferences;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserEntity user = users.findByUsername(request.login())
                .or(() -> users.findByEmail(request.login()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
        if (isBlockedOrInactive(user) || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials.");
        }
        user.recordLogin();
        return issue(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        user.changePassword(encoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokens.findByTokenHash(opaque.hash(request.refreshToken()))
                .filter(RefreshToken::isUsable)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));
        UserEntity user = token.getUser();
        if (isBlockedOrInactive(user)) {
            throw new BadCredentialsException("Invalid refresh token.");
        }
        token.revoke();
        return issue(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokens.findByTokenHash(opaque.hash(request.refreshToken())).ifPresent(RefreshToken::revoke);
    }

    /** Always completes silently, so the response never reveals whether the e-mail exists. */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        users.findByEmail(request.email())
                .filter(user -> !isBlockedOrInactive(user))
                .ifPresent(user -> {
                    String rawToken = opaque.create();
                    resetTokens.save(PasswordResetToken.create(
                            user, opaque.hash(rawToken), Instant.now().plus(RESET_TOKEN_VALIDITY)));
                    resetMail.send(user, rawToken);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokens.findByTokenHash(opaque.hash(request.token()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new BadCredentialsException("Invalid password reset token."));
        UserEntity user = token.getUser();
        if (isBlockedOrInactive(user)) {
            throw new BadCredentialsException("Invalid password reset token.");
        }
        user.changePassword(encoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        token.use();
    }

    private static boolean isBlockedOrInactive(UserEntity user) {
        return user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.INACTIVE;
    }

    private TokenResponse issue(UserEntity user) {
        String rawRefreshToken = opaque.create();
        refreshTokens.save(RefreshToken.create(
                user, opaque.hash(rawRefreshToken), Instant.now().plus(REFRESH_TOKEN_VALIDITY)));
        boolean requiresOnboarding = !user.isMustChangePassword() && !preferences.existsByUserId(user.getId());
        String accessToken = jwt.createAccessToken(user.getId(), user.getRole(), user.isMustChangePassword(),
                requiresOnboarding, user.getTokenVersion());
        return new TokenResponse(accessToken, rawRefreshToken, "Bearer", user.isMustChangePassword());
    }
}
