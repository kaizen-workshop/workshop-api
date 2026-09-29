package br.com.weg.workshop.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.weg.workshop.notification.domain.*;
import br.com.weg.workshop.notification.dto.RegisterDeviceRequest;
import br.com.weg.workshop.notification.push.PushProvider;
import br.com.weg.workshop.notification.repository.*;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock NotificationDeviceRepository devices;
    @Mock UserRepository users;
    @Mock PushProvider push;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications, devices, users, push, new ObjectMapper());
    }

    @Test
    void persistsAnAutomaticNotificationBeforePushDelivery() {
        UserEntity user = user("participant");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationDevice device = NotificationDevice.create(user, "push-token", DevicePlatform.ANDROID);
        when(devices.findByUserIdAndActiveTrue(user.getId())).thenReturn(List.of(device));

        service.notify(user.getId(), NotificationType.REGISTRATION_CREATED, "Registration", "Created",
                Map.of("registrationId", "registration-id"));

        verify(notifications).save(any(Notification.class));
        verify(push).send("push-token", "Registration", "Created",
                Map.of("registrationId", "registration-id"));
    }

    @Test
    void onlyTheOwnerCanMarkANotificationAsRead() {
        UserEntity owner = user("owner");
        Notification notification = Notification.create(owner, NotificationType.MANUAL, "Title", "Message", "{}",
                Instant.now());
        when(notifications.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markRead(UUID.randomUUID(), notification.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(service.markRead(owner.getId(), notification.getId()).read()).isTrue();
    }

    @Test
    void registeringTheSameTokenReassignsAndReactivatesTheDevice() {
        UserEntity previous = user("previous");
        UserEntity current = user("current");
        NotificationDevice device = NotificationDevice.create(previous, "push-token", DevicePlatform.ANDROID);
        device.deactivate();
        when(users.findById(current.getId())).thenReturn(Optional.of(current));
        when(devices.findByToken("push-token")).thenReturn(Optional.of(device));

        var response = service.registerDevice(current.getId(),
                new RegisterDeviceRequest("push-token", DevicePlatform.IOS));

        assertThat(response.active()).isTrue();
        assertThat(response.platform()).isEqualTo("IOS");
        assertThat(device.getUser().getId()).isEqualTo(current.getId());
        verify(devices, never()).save(any());
    }

    private UserEntity user(String username) {
        return UserEntity.create(username, username, username + "@example.com", null, null, null, "hash",
                Role.PARTICIPANT);
    }
}
