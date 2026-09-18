package br.com.weg.workshop.auth.dto; import jakarta.validation.constraints.NotBlank; public record RefreshTokenRequest(@NotBlank String refreshToken) {}
