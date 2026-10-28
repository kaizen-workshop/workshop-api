package br.com.weg.workshop.notification.dto;

import br.com.weg.workshop.notification.domain.NotificationDevice;
import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(UUID id, String platform, boolean active, Instant createdAt, Instant updatedAt) {
    public static DeviceResponse from(NotificationDevice device) {
        return new DeviceResponse(device.getId(), device.getPlatform().name(), device.isActive(),
                device.getCreatedAt(), device.getUpdatedAt());
    }
}
