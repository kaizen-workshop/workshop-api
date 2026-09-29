package br.com.weg.workshop.notification.service;

import br.com.weg.workshop.notification.domain.*;
import br.com.weg.workshop.notification.dto.*;
import br.com.weg.workshop.notification.push.PushProvider;
import br.com.weg.workshop.notification.repository.*;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import br.com.weg.workshop.user.domain.UserEntity;
import br.com.weg.workshop.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
public class NotificationService {
    private static final TypeReference<Map<String, String>> DATA_TYPE = new TypeReference<>() { };
    private final NotificationRepository notifications;
    private final NotificationDeviceRepository devices;
    private final UserRepository users;
    private final PushProvider push;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notifications, NotificationDeviceRepository devices,
                               UserRepository users, PushProvider push, ObjectMapper objectMapper) {
        this.notifications = notifications; this.devices = devices; this.users = users;
        this.push = push; this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Pageable pageable) {
        return notifications.findByUserId(userId, pageable).map(this::response);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = owned(userId, notificationId);
        notification.markRead();
        return response(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) { notifications.markAllRead(userId, Instant.now()); }

    @Transactional
    public DeviceResponse registerDevice(UUID userId, RegisterDeviceRequest request) {
        UserEntity user = user(userId);
        NotificationDevice device = devices.findByToken(request.token()).map(existing -> {
            existing.register(user, request.platform());
            return existing;
        }).orElseGet(() -> devices.save(NotificationDevice.create(user, request.token(), request.platform())));
        return DeviceResponse.from(device);
    }

    @Transactional
    public void unregisterDevice(UUID userId, UUID deviceId) {
        NotificationDevice device = devices.findById(deviceId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification device not found."));
        device.deactivate();
    }

    @Transactional
    public List<NotificationResponse> createManual(ManualNotificationRequest request) {
        List<UserEntity> recipients = users.findAllById(request.userIds());
        if (recipients.size() != request.userIds().size()) throw new ResourceNotFoundException("User not found.");
        Instant scheduledAt = request.scheduledAt() == null ? Instant.now() : request.scheduledAt();
        List<Notification> created = notifications.saveAll(recipients.stream()
                .map(user -> Notification.create(user, NotificationType.MANUAL, request.title(), request.message(),
                        json(request.data()), scheduledAt)).toList());
        if (!scheduledAt.isAfter(Instant.now())) created.forEach(this::scheduleDelivery);
        return created.stream().map(this::response).toList();
    }

    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String message, Map<String, String> data) {
        Notification notification = notifications.save(Notification.create(user(userId), type, title, message,
                json(data), Instant.now()));
        scheduleDelivery(notification);
    }

    @Scheduled(fixedDelayString = "${app.notifications.delivery-delay-ms:30000}")
    @Transactional
    public void deliverScheduled() {
        notifications.findByDeliveredAtIsNullAndScheduledAtLessThanEqual(Instant.now())
                .forEach(this::scheduleDelivery);
    }

    private void scheduleDelivery(Notification notification) {
        notification.markDelivered();
        Runnable delivery = () -> devices.findByUserIdAndActiveTrue(notification.getUser().getId()).forEach(device ->
                push.send(device.getToken(), notification.getTitle(), notification.getMessage(), data(notification)));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { delivery.run(); }
            });
        } else delivery.run();
    }

    private Notification owned(UUID userId, UUID notificationId) {
        return notifications.findById(notificationId)
                .filter(notification -> notification.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
    }
    private UserEntity user(UUID userId) { return users.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found.")); }
    private NotificationResponse response(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType().name(), notification.getTitle(),
                notification.getMessage(), notification.getReadAt() != null, data(notification), notification.getCreatedAt());
    }
    private Map<String, String> data(Notification notification) {
        try { return objectMapper.readValue(notification.getDataJson(), DATA_TYPE); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Invalid notification data.", exception); }
    }
    private String json(Map<String, String> data) {
        try { return objectMapper.writeValueAsString(data == null ? Map.of() : data); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid notification data.", exception); }
    }
}
