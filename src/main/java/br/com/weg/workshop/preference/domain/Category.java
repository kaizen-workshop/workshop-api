package br.com.weg.workshop.preference.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "category", schema = "workshop")
public class Category {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Category() { }
    public static Category create(String name, String description) {
        Category category = new Category(); category.id = UUID.randomUUID(); category.name = name;
        category.description = description; category.active = true; category.createdAt = Instant.now(); category.updatedAt = category.createdAt;
        return category;
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
