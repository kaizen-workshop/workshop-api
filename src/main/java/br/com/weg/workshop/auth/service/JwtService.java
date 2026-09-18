package br.com.weg.workshop.auth.service;

import br.com.weg.workshop.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String secret;
    private final long accessTokenMinutes;

    public JwtService(@Value("${app.security.jwt.secret:}") String secret,
                      @Value("${app.security.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.secret = secret;
        this.accessTokenMinutes = accessTokenMinutes;
    }
    public String createAccessToken(UUID userId, Role role, boolean mustChangePassword) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).claim("role", role.name())
                .claim("mustChangePassword", mustChangePassword).issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))).signWith(key()).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload(); }
    private SecretKey key() {
        if (secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured.");
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
