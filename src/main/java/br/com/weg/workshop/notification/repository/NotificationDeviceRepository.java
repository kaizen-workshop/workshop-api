package br.com.weg.workshop.notification.repository;

import br.com.weg.workshop.notification.domain.NotificationDevice;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {
    Optional<NotificationDevice> findByToken(String token);
    List<NotificationDevice> findByUserIdAndActiveTrue(UUID userId);
}
