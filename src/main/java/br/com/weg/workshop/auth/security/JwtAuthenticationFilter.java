package br.com.weg.workshop.auth.security;

import br.com.weg.workshop.auth.service.JwtService;
import br.com.weg.workshop.shared.error.ApiErrorFactory;
import br.com.weg.workshop.shared.error.ErrorCode;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.domain.UserStatus;
import br.com.weg.workshop.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final ApiErrorFactory errors;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwt, UserRepository users, ApiErrorFactory errors, ObjectMapper mapper) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims;
        UserEntity user;
        try {
            claims = jwt.parse(header.substring(7));
            UUID userId = UUID.fromString(claims.getSubject());
            user = users.findById(userId).orElse(null);
            Object version = claims.get("tokenVersion");
            if (user == null || !(version instanceof Number number)
                    || number.intValue() != user.getTokenVersion()
                    || !user.getRole().name().equals(claims.get("role", String.class))
                    || user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.INACTIVE) {
                unauthorized(request, response);
                return;
            }
        } catch (JwtException | IllegalArgumentException exception) {
            unauthorized(request, response);
            return;
        }

        if (user.isMustChangePassword() && !request.getRequestURI().equals("/api/v1/auth/change-password")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), errors.create(request, HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN, "Password change is required.", List.of()));
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("authenticatedUserId", user.getId().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), errors.create(request, HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED, "Invalid or expired access token.", List.of()));
    }
}
