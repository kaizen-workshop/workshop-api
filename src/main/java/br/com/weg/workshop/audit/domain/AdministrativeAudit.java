package br.com.weg.workshop.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "administrative_audit", schema = "workshop")
public class AdministrativeAudit {
    @Id private UUID id;
    @Column(name = "user_id") private UUID userId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String entity;
    @Column(name = "entity_id", nullable = false) private UUID entityId;
    @Column(name = "previous_value", columnDefinition = "TEXT") private String previousValue;
    @Column(name = "new_value", columnDefinition = "TEXT") private String newValue;
    @Column(name = "occurred_at", nullable = false) private Instant timestamp;
    private String ip;

    protected AdministrativeAudit() { }

    public AdministrativeAudit(UUID userId, String action, String entity, UUID entityId,
                               String previousValue, String newValue, String ip) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.timestamp = Instant.now();
        this.ip = ip;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getAction() { return action; }
    public String getEntity() { return entity; }
    public UUID getEntityId() { return entityId; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public Instant getTimestamp() { return timestamp; }
    public String getIp() { return ip; }
}
