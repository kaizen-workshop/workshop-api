package br.com.weg.workshop.notification.repository;

import br.com.weg.workshop.notification.domain.NotificationDevice;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {
    Optional<NotificationDevice> findByToken(String token);
    List<NotificationDevice> findByUserIdAndActiveTrue(UUID userId);

    @Query(value = "select d.* from workshop.notification_device d "
            + "where d.user_id = :userId and d.active = true and not exists ("
            + "select 1 from workshop.notification_device_delivery delivery "
            + "where delivery.notification_id = :notificationId and delivery.device_id = d.id)",
            nativeQuery = true)
    List<NotificationDevice> findPendingDeliveryDevices(@Param("notificationId") UUID notificationId,
                                                        @Param("userId") UUID userId);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "insert into workshop.notification_device_delivery (notification_id, device_id, delivered_at) "
            + "values (:notificationId, :deviceId, :deliveredAt) on conflict do nothing", nativeQuery = true)
    int recordDelivery(@Param("notificationId") UUID notificationId,
                       @Param("deviceId") UUID deviceId,
                       @Param("deliveredAt") Instant deliveredAt);
}
