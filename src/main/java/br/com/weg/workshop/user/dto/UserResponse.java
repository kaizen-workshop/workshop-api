package br.com.weg.workshop.user.dto;

import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.domain.UserStatus;
import java.util.UUID;

public record UserResponse(UUID id, String name, String username, String email, Role role, UserStatus status,
                           boolean mustChangePassword) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getStatus(), user.isMustChangePassword());
    }
}
