package br.com.weg.workshop.chat.service;

import br.com.weg.workshop.chat.domain.Message;
import br.com.weg.workshop.chat.dto.MessagePageResponse;
import br.com.weg.workshop.chat.dto.MessageRequest;
import br.com.weg.workshop.chat.dto.MessageResponse;
import br.com.weg.workshop.chat.repository.MessageRepository;
import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatService {

    private final MessageRepository messages;
    private final GroupService groups;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;

    public ChatService(
            MessageRepository messages,
            GroupService groups,
            UserRepository users,
            SimpMessagingTemplate messaging
    ) {
        this.messages = messages;
        this.groups = groups;
        this.users = users;
        this.messaging = messaging;
    }

    @Transactional(readOnly = true)
    public MessagePageResponse list(UUID userId, boolean admin, UUID groupId, UUID cursor, int size) {
        groups.requireAccess(userId, admin, groupId);
        Cursor position = cursor(cursor, groupId);
        List<Message> page = messages.findPage(
                groupId,
                position.sentAt(),
                position.id(),
                PageRequest.of(0, size + 1)
        );
        boolean hasMore = page.size() > size;
        List<MessageResponse> content = page.stream().limit(size).map(MessageResponse::from).toList();
        UUID nextCursor = hasMore && !content.isEmpty() ? content.get(content.size() - 1).id() : null;
        return new MessagePageResponse(content, nextCursor, hasMore);
    }

    @Transactional
    public MessageResponse send(UUID userId, boolean admin, UUID groupId, MessageRequest request) {
        return send(userId, admin, groupId, null, request);
    }

    @Transactional
    public MessageResponse send(UUID userId, boolean admin, UUID groupId, UUID clientOperationId,
                                MessageRequest request) {
        WorkshopGroup group = groups.requireAccess(userId, admin, groupId);
        requireActive(group);
        UserEntity author = (clientOperationId == null ? users.findById(userId) : users.findByIdForUpdate(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (clientOperationId != null) {
            Message existing = messages.findByAuthorIdAndClientOperationId(userId, clientOperationId).orElse(null);
            if (existing != null) {
                if (!existing.getGroup().getId().equals(groupId)) {
                    throw new ConflictException("Idempotency-Key was already used for another message.");
                }
                return MessageResponse.from(existing);
            }
        }
        MessageResponse response = MessageResponse.from(messages.save(
                Message.create(group, author, request.content(), clientOperationId)));
        publish(response);
        return response;
    }

    @Transactional
    public MessageResponse edit(
            UUID userId,
            boolean admin,
            UUID groupId,
            UUID messageId,
            MessageRequest request
    ) {
        WorkshopGroup group = groups.requireAccess(userId, admin, groupId);
        requireActive(group);
        Message message = message(groupId, messageId);
        if (!message.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("Message not found.");
        }
        message.edit(request.content());
        MessageResponse response = MessageResponse.from(message);
        publish(response);
        return response;
    }

    @Transactional
    public MessageResponse delete(UUID userId, boolean admin, UUID groupId, UUID messageId) {
        WorkshopGroup group = groups.requireAccess(userId, admin, groupId);
        Message message = message(groupId, messageId);
        if (!message.getAuthor().getId().equals(userId) && !groups.isModerator(userId, admin, group)) {
            throw new ResourceNotFoundException("Message not found.");
        }
        message.delete();
        MessageResponse response = MessageResponse.from(message);
        publish(response);
        return response;
    }

    private Cursor cursor(UUID cursor, UUID groupId) {
        if (cursor == null) return new Cursor(null, null);
        Message message = message(groupId, cursor);
        return new Cursor(message.getSentAt(), message.getId());
    }

    private Message message(UUID groupId, UUID messageId) {
        return messages.findById(messageId)
                .filter(message -> message.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
    }

    private void requireActive(WorkshopGroup group) {
        if (!group.isActive()) throw new ConflictException("Messages cannot be sent to an inactive group.");
    }

    private void publish(MessageResponse response) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliver(response);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliver(response);
            }
        });
    }

    private void deliver(MessageResponse response) {
        messaging.convertAndSend("/topic/groups/" + response.groupId(), response);
    }

    private record Cursor(Instant sentAt, UUID id) {
    }
}
