package br.com.weg.workshop.user.controller;

import br.com.weg.workshop.user.dto.CreateUserRequest;
import br.com.weg.workshop.user.dto.UserResponse;
import br.com.weg.workshop.user.service.UserAdministrationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserAdministrationService service;
    public AdminUserController(UserAdministrationService service) { this.service = service; }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + response.id())).body(response);
    }
}
