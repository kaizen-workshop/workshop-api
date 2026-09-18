package br.com.weg.workshop.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user", schema = "workshop")
public class UserEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false, unique = true)
    private String email;
    private String wegRegistration;
    private String phone;
    private String profileImage;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    @Column(nullable = false)
    private boolean mustChangePassword;
    private Instant lastLoginAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public static UserEntity create(String name, String username, String email, String wegRegistration, String phone,
                                    String profileImage, String passwordHash, Role role) {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID();
        user.name = name;
        user.username = username;
        user.email = email;
        user.wegRegistration = wegRegistration;
        user.phone = phone;
        user.profileImage = profileImage;
        user.passwordHash = passwordHash;
        user.role = role;
        user.status = UserStatus.PENDING;
        user.mustChangePassword = true;
        user.createdAt = Instant.now();
        user.updatedAt = user.createdAt;
        return user;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getPasswordHash() { return passwordHash; }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        this.updatedAt = this.lastLoginAt;
    }
}
