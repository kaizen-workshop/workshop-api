package br.com.weg.workshop.auth.controller;
import br.com.weg.workshop.auth.dto.LoginRequest;
import br.com.weg.workshop.auth.dto.TokenResponse;
import br.com.weg.workshop.auth.dto.ChangePasswordRequest;
import br.com.weg.workshop.auth.dto.RefreshTokenRequest;
import br.com.weg.workshop.auth.dto.ForgotPasswordRequest;
import br.com.weg.workshop.auth.dto.ResetPasswordRequest;
import br.com.weg.workshop.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthenticationService authenticationService;
 public AuthController(AuthenticationService authenticationService) { this.authenticationService=authenticationService; }
 @PostMapping("/login") public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) { return ResponseEntity.ok(authenticationService.login(request)); }
 @PostMapping("/change-password") public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) { authenticationService.changePassword(UUID.fromString(authentication.getName()), request); return ResponseEntity.noContent().build(); }
 @PostMapping("/refresh") public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){return ResponseEntity.ok(authenticationService.refresh(request));}
 @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){authenticationService.logout(request);return ResponseEntity.noContent().build();}
 @PostMapping("/forgot-password") public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){authenticationService.requestPasswordReset(request);return ResponseEntity.accepted().build();}
 @PostMapping("/reset-password") public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){authenticationService.resetPassword(request);return ResponseEntity.noContent().build();}
}
