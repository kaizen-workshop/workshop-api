package br.com.weg.workshop.post.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_comment", schema = "workshop")
public class PostComment {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private UUID clientOperationId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PostComment() {
    }

    public static PostComment create(Post post, UserEntity user, String content) {
        return create(post, user, content, null);
    }

    public static PostComment create(Post post, UserEntity user, String content, UUID clientOperationId) {
        PostComment comment = new PostComment();
        comment.id = UUID.randomUUID();
        comment.post = post;
        comment.user = user;
        comment.content = content;
        comment.clientOperationId = clientOperationId;
        comment.createdAt = Instant.now();
        comment.updatedAt = comment.createdAt;
        return comment;
    }

    public void edit(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Post getPost() { return post; }
    public UserEntity getUser() { return user; }
    public String getContent() { return content; }
    public UUID getClientOperationId() { return clientOperationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
