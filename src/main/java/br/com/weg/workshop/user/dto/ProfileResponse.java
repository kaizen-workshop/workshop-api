package br.com.weg.workshop.user.dto;

import br.com.weg.workshop.preference.dto.ThemeResponse;
import br.com.weg.workshop.user.domain.UserEntity;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String name, String username, String email, String phone, String profileImage, List<ThemeResponse> themes) {
    public static ProfileResponse from(UserEntity user, List<ThemeResponse> themes) {
        return new ProfileResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(), user.getPhone(), user.getProfileImage(), themes);
    }
}
