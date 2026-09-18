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
@Service
public class AuthenticationService {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
 public AuthenticationService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.encoder=encoder; this.jwt=jwt; }
 @Transactional
 public TokenResponse login(LoginRequest request) {
  UserEntity user=users.findByUsername(request.login()).or(() -> users.findByEmail(request.login())).orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
  if (user.getStatus()==UserStatus.BLOCKED || user.getStatus()==UserStatus.INACTIVE || !encoder.matches(request.password(), user.getPasswordHash())) throw new BadCredentialsException("Invalid credentials.");
  user.recordLogin(); return new TokenResponse(jwt.createAccessToken(user.getId(), user.getRole(), user.isMustChangePassword()), "Bearer", user.isMustChangePassword());
 }
 @Transactional
 public void changePassword(UUID userId, ChangePasswordRequest request) {
  UserEntity user=users.findById(userId).orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
  if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) throw new BadCredentialsException("Invalid credentials.");
  user.changePassword(encoder.encode(request.newPassword()));
 }
}
