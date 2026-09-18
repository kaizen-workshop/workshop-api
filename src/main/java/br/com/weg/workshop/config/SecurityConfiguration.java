package br.com.weg.workshop.config;

import br.com.weg.workshop.shared.error.ApiErrorFactory;
import br.com.weg.workshop.shared.error.ApiErrorResponse;
import br.com.weg.workshop.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiErrorFactory apiErrorFactory, ObjectMapper objectMapper)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response,
                                apiErrorFactory.create(request, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                                        "Authentication is required.", List.of()),
                                objectMapper))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response,
                                apiErrorFactory.create(request, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                                        "Access is denied.", List.of()),
                                objectMapper)))
                .build();
    }

    private void writeError(HttpServletResponse response, ApiErrorResponse error, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
