package br.com.weg.workshop.user.service;
import br.com.weg.workshop.audit.service.AuditService;

import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.dto.CreateUserRequest;
import br.com.weg.workshop.user.dto.UserResponse;
import br.com.weg.workshop.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UserAdministrationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator passwords;
    private final InitialAccessMailService mailService;
    private final AuditService audit;
    public UserAdministrationService(UserRepository users, PasswordEncoder passwordEncoder,
                                     TemporaryPasswordGenerator passwords, InitialAccessMailService mailService,
                                     AuditService audit) {
        this.users = users; this.passwordEncoder = passwordEncoder; this.passwords = passwords;
        this.mailService = mailService; this.audit = audit;
    }
    @Transactional
    public UserResponse create(UUID actorId, CreateUserRequest request) {
        if (users.existsByUsername(request.username())) throw new ConflictException("Username already exists.");
        if (users.existsByEmail(request.email())) throw new ConflictException("Email already exists.");
        String temporaryPassword = passwords.generate();
        UserEntity user = users.save(UserEntity.create(request.name(), request.username(), request.email(),
                request.wegRegistration(), request.phone(), request.profileImage(), passwordEncoder.encode(temporaryPassword), request.role()));
        mailService.send(user.getEmail(), user.getUsername(), temporaryPassword);
        audit.record(actorId, "CREATE", "USER", user.getId(), null,
                "role=" + user.getRole().name() + ",status=" + user.getStatus().name());
        return UserResponse.from(user);
    }
}
