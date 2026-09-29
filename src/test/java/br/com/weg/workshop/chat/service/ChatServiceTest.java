package br.com.weg.workshop.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.chat.domain.Message;
import br.com.weg.workshop.chat.dto.MessageRequest;
import br.com.weg.workshop.chat.repository.MessageRepository;
import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.preference.domain.Category;
import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.Role;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.PaymentMethod;
import br.com.weg.workshop.workshop.domain.Workshop;
import br.com.weg.workshop.workshop.domain.WorkshopData;
import br.com.weg.workshop.workshop.domain.WorkshopModality;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messages;

    @Mock
    private GroupService groups;

    @Mock
    private UserRepository users;

    @Mock
    private SimpMessagingTemplate messaging;

    @InjectMocks
    private ChatService service;

    private UserEntity participant;
    private WorkshopGroup group;

    @BeforeEach
    void setUp() {
        participant = user("participant", Role.PARTICIPANT);
        group = group();
    }

    @Test
    void persistsBeforePublishingAnActiveGroupMessage() {
        group.activate();
        when(groups.requireAccess(participant.getId(), false, group.getId())).thenReturn(group);
        when(users.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(messages.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.send(
                participant.getId(),
                false,
                group.getId(),
                new MessageRequest("Hello")
        );

        assertThat(response.content()).isEqualTo("Hello");
        verify(messages).save(any(Message.class));
        verify(messaging).convertAndSend("/topic/groups/" + group.getId(), response);
    }

    @Test
    void rejectsMessagesForAnInactiveGroup() {
        when(groups.requireAccess(participant.getId(), false, group.getId())).thenReturn(group);

        assertThatThrownBy(() -> service.send(
                participant.getId(),
                false,
                group.getId(),
                new MessageRequest("Hello")
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void softDeletesAVisibleMessageDuringModeration() {
        UserEntity author = user("author", Role.PARTICIPANT);
        Message message = Message.create(group, author, "Sensitive content");
        when(groups.requireAccess(participant.getId(), false, group.getId())).thenReturn(group);
        when(groups.isModerator(participant.getId(), false, group)).thenReturn(true);
        when(messages.findById(message.getId())).thenReturn(Optional.of(message));

        var response = service.delete(participant.getId(), false, group.getId(), message.getId());

        assertThat(response.content()).isNull();
        assertThat(response.deletedAt()).isNotNull();
        verify(messaging).convertAndSend("/topic/groups/" + group.getId(), response);
    }

    private WorkshopGroup group() {
        UserEntity owner = user("owner", Role.ARWEG);
        Workshop workshop = Workshop.create(
                new WorkshopData(
                        "Spring", "Workshop", null, LocalDate.now(), LocalDate.now(),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), "Room", WorkshopModality.IN_PERSON,
                        BigDecimal.ZERO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60),
                        10, PaymentMethod.FREE, false, null
                ),
                Theme.create("Java", null),
                Category.create("Technology", null),
                owner
        );
        return WorkshopGroup.create(workshop);
    }

    private UserEntity user(String username, Role role) {
        return UserEntity.create(
                username,
                username,
                username + "@example.com",
                null,
                null,
                null,
                "hash",
                role
        );
    }
}
