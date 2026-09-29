package br.com.weg.workshop.notification.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_device", schema = "workshop")
public class NotificationDevice {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Column(nullable = false, unique = true, length = 500) private String token;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DevicePlatform platform;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected NotificationDevice() { }

    public static NotificationDevice create(UserEntity user, String token, DevicePlatform platform) {
        NotificationDevice device = new NotificationDevice();
        device.id = UUID.randomUUID();
        device.user = user;
        device.token = token;
        device.platform = platform;
        device.active = true;
        device.createdAt = Instant.now();
        device.updatedAt = device.createdAt;
        return device;
    }

    public void register(UserEntity user, DevicePlatform platform) {
        this.user = user;
        this.platform = platform;
        this.active = true;
        this.updatedAt = Instant.now();
    }
    public void deactivate() { active = false; updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getToken() { return token; }
    public DevicePlatform getPlatform() { return platform; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
