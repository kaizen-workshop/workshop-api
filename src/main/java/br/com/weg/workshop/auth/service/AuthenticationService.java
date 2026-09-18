package br.com.weg.workshop.auth.service;
import br.com.weg.workshop.auth.dto.LoginRequest;
import br.com.weg.workshop.auth.dto.TokenResponse;
import br.com.weg.workshop.auth.dto.ChangePasswordRequest;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.domain.UserStatus;
import br.com.weg.workshop.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.time.*;
import java.time.temporal.ChronoUnit;
import br.com.weg.workshop.auth.repository.*;
import br.com.weg.workshop.auth.domain.*;
import br.com.weg.workshop.auth.dto.*;
@Service
public class AuthenticationService {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final RefreshTokenRepository refreshTokens; private final PasswordResetTokenRepository resetTokens; private final OpaqueTokenService opaque; private final PasswordResetMailService resetMail;
 public AuthenticationService(UserRepository users, PasswordEncoder encoder, JwtService jwt, RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens, OpaqueTokenService opaque, PasswordResetMailService resetMail) { this.users=users; this.encoder=encoder; this.jwt=jwt;this.refreshTokens=refreshTokens;this.resetTokens=resetTokens;this.opaque=opaque;this.resetMail=resetMail; }
 @Transactional
 public TokenResponse login(LoginRequest request) {
  UserEntity user=users.findByUsername(request.login()).or(() -> users.findByEmail(request.login())).orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
  if (user.getStatus()==UserStatus.BLOCKED || user.getStatus()==UserStatus.INACTIVE || !encoder.matches(request.password(), user.getPasswordHash())) throw new BadCredentialsException("Invalid credentials.");
  user.recordLogin(); return issue(user);
 }
 @Transactional
 public void changePassword(UUID userId, ChangePasswordRequest request) {
  UserEntity user=users.findById(userId).orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
  if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) throw new BadCredentialsException("Invalid credentials.");
  user.changePassword(encoder.encode(request.newPassword()));
 }
 @Transactional public TokenResponse refresh(RefreshTokenRequest request){var token=refreshTokens.findByTokenHash(opaque.hash(request.refreshToken())).filter(RefreshToken::isUsable).orElseThrow(()->new BadCredentialsException("Invalid refresh token.")); token.revoke(); return issue(token.getUser());}
 @Transactional public void logout(RefreshTokenRequest request){refreshTokens.findByTokenHash(opaque.hash(request.refreshToken())).ifPresent(RefreshToken::revoke);}
 @Transactional public void requestPasswordReset(ForgotPasswordRequest request){users.findByEmail(request.email()).filter(u->u.getStatus()!=UserStatus.BLOCKED&&u.getStatus()!=UserStatus.INACTIVE).ifPresent(u->{String raw=opaque.create();resetTokens.save(PasswordResetToken.create(u,opaque.hash(raw),Instant.now().plus(1,ChronoUnit.HOURS)));resetMail.send(u,raw);});}
 @Transactional public void resetPassword(ResetPasswordRequest request){var token=resetTokens.findByTokenHash(opaque.hash(request.token())).filter(PasswordResetToken::isUsable).orElseThrow(()->new BadCredentialsException("Invalid password reset token."));token.getUser().changePassword(encoder.encode(request.newPassword()));token.use();}
 private TokenResponse issue(UserEntity user){String raw=opaque.create();refreshTokens.save(RefreshToken.create(user,opaque.hash(raw),Instant.now().plus(30,ChronoUnit.DAYS)));return new TokenResponse(jwt.createAccessToken(user.getId(),user.getRole(),user.isMustChangePassword()),raw,"Bearer",user.isMustChangePassword());}
}
