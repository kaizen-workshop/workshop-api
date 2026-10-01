package br.com.weg.workshop.notification.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification", schema = "workshop")
public class Notification {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(nullable = false, columnDefinition = "TEXT") private String dataJson;
    private Instant readAt;
    @Column(nullable = false) private Instant scheduledAt;
    private Instant deliveredAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Notification() { }

    public static Notification create(UserEntity user, NotificationType type, String title, String message,
                                      String dataJson, Instant scheduledAt) {
        Notification notification = new Notification();
        notification.id = UUID.randomUUID();
        notification.user = user;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.dataJson = dataJson;
        notification.scheduledAt = scheduledAt;
        notification.createdAt = Instant.now();
        notification.updatedAt = notification.createdAt;
        return notification;
    }

    public void markRead() { if (readAt == null) { readAt = Instant.now(); updatedAt = readAt; } }
    public void markDelivered() { if (deliveredAt == null) { deliveredAt = Instant.now(); updatedAt = deliveredAt; } }
    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDataJson() { return dataJson; }
    public Instant getReadAt() { return readAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
