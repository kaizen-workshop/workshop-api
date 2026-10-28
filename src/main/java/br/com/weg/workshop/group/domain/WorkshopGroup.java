package br.com.weg.workshop.group.domain;

import br.com.weg.workshop.workshop.domain.Workshop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workshop_group", schema = "workshop")
public class WorkshopGroup {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false, unique = true)
    private Workshop workshop;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WorkshopGroup() {
    }

    public static WorkshopGroup create(Workshop workshop) {
        WorkshopGroup group = new WorkshopGroup();
        group.id = UUID.randomUUID();
        group.workshop = workshop;
        group.active = false;
        group.createdAt = Instant.now();
        group.updatedAt = group.createdAt;
        return group;
    }

    public void activate() {
        active = true;
        updatedAt = Instant.now();
    }

    public void deactivate() {
        active = false;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Workshop getWorkshop() { return workshop; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
