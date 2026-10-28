package br.com.weg.workshop.auth.dto;
import jakarta.validation.constraints.*;
public record ChangePasswordRequest(@NotBlank String currentPassword,
        @NotBlank @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH, message = PasswordRules.MESSAGE)
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE) String newPassword) {}
