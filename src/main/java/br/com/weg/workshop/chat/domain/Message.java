package br.com.weg.workshop.chat.domain;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.shared.error.ConflictException;
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
@Table(name = "chat_message", schema = "workshop")
public class Message {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private WorkshopGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    private Instant editedAt;
    private Instant deletedAt;

    protected Message() {
    }

    public static Message create(WorkshopGroup group, UserEntity author, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID();
        message.group = group;
        message.author = author;
        message.content = content;
        message.sentAt = Instant.now();
        return message;
    }

    public void edit(String content) {
        if (deletedAt != null) throw new ConflictException("Deleted messages cannot be edited.");
        this.content = content;
        editedAt = Instant.now();
    }

    public void delete() {
        if (deletedAt == null) deletedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public WorkshopGroup getGroup() { return group; }
    public UserEntity getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getSentAt() { return sentAt; }
    public Instant getEditedAt() { return editedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
