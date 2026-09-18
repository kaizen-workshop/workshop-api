package br.com.weg.workshop.preference.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "theme", schema = "workshop")
public class Theme {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Theme() { }
    public static Theme create(String name, String description) {
        Theme theme = new Theme(); theme.id = UUID.randomUUID(); theme.name = name;
        theme.description = description; theme.active = true; theme.createdAt = Instant.now(); theme.updatedAt = theme.createdAt;
        return theme;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public void update(String name, String description, Boolean active) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (active != null) this.active = active;
        updatedAt = Instant.now();
    }
}
