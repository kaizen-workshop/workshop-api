package br.com.weg.workshop.user.dto;

import br.com.weg.workshop.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 50) String wegRegistration,
        @Size(max = 30) String phone,
        @Size(max = 500) String profileImage,
        @NotNull Role role
) {
}
