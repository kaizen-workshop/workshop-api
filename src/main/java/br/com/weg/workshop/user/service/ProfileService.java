package br.com.weg.workshop.user.service;

import br.com.weg.workshop.preference.service.PreferenceService;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.dto.*;
import br.com.weg.workshop.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository users; private final PreferenceService preferences;
    public ProfileService(UserRepository users, PreferenceService preferences) { this.users = users; this.preferences = preferences; }
    @Transactional(readOnly = true) public ProfileResponse get(UUID id) {
        var user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return ProfileResponse.from(user, preferences.selectedThemes(id));
    }
    @Transactional public ProfileResponse update(UUID id, UpdateProfileRequest request) {
        var user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.updateProfile(request.name(), request.phone(), request.profileImage());
        return ProfileResponse.from(user, preferences.selectedThemes(id));
    }
}
