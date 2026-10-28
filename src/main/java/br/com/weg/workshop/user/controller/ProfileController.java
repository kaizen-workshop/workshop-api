package br.com.weg.workshop.user.controller;

import br.com.weg.workshop.user.dto.*;
import br.com.weg.workshop.user.service.ProfileService;
import br.com.weg.workshop.preference.dto.ReplaceThemesRequest;
import br.com.weg.workshop.preference.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {
    private final ProfileService service;
    private final PreferenceService preferences;
    public ProfileController(ProfileService service, PreferenceService preferences) { this.service = service; this.preferences = preferences; }
    @GetMapping @Operation(summary = "Get the authenticated user's profile")
    public ProfileResponse get(Authentication authentication) { return service.get(UUID.fromString(authentication.getName())); }
    @PatchMapping @Operation(summary = "Update permitted fields of the authenticated user's profile")
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) { return service.update(UUID.fromString(authentication.getName()), request); }
    @PutMapping("/themes") @Operation(summary = "Replace the authenticated user's selected themes")
    public ResponseEntity<Void> themes(@Valid @RequestBody ReplaceThemesRequest request, Authentication authentication) {
        preferences.replace(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.noContent().build();
    }
}
