package br.com.weg.workshop.auth.dto; import jakarta.validation.constraints.NotBlank; public record ResetPasswordRequest(@NotBlank String token,@NotBlank String newPassword) {}
