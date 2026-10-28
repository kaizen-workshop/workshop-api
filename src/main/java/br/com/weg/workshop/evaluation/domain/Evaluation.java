package br.com.weg.workshop.evaluation.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.workshop.domain.Workshop;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation", schema = "workshop", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "workshop_id"}))
public class Evaluation {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "workshop_id", nullable = false) private Workshop workshop;
    @Column(nullable = false) private short rating;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(nullable = false) private short contentRating;
    @Column(nullable = false) private short instructorRating;
    @Column(nullable = false) private short organizationRating;
    private UUID clientOperationId;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Evaluation() { }
    public static Evaluation create(UserEntity user, Workshop workshop, short rating, String comment, short contentRating, short instructorRating, short organizationRating) {
        return create(user, workshop, rating, comment, contentRating, instructorRating, organizationRating, null);
    }
    public static Evaluation create(UserEntity user, Workshop workshop, short rating, String comment, short contentRating, short instructorRating, short organizationRating, UUID clientOperationId) {
        Evaluation evaluation = new Evaluation(); evaluation.id = UUID.randomUUID(); evaluation.user = user; evaluation.workshop = workshop;
        evaluation.rating = rating; evaluation.comment = comment; evaluation.contentRating = contentRating; evaluation.instructorRating = instructorRating; evaluation.organizationRating = organizationRating;
        evaluation.clientOperationId = clientOperationId;
        evaluation.createdAt = Instant.now(); evaluation.updatedAt = evaluation.createdAt; return evaluation;
    }
    public UUID getId() { return id; } public UserEntity getUser() { return user; } public Workshop getWorkshop() { return workshop; }
    public short getRating() { return rating; } public String getComment() { return comment; } public short getContentRating() { return contentRating; }
    public short getInstructorRating() { return instructorRating; } public short getOrganizationRating() { return organizationRating; }
    public UUID getClientOperationId() { return clientOperationId; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
