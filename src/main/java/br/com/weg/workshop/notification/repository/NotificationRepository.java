package br.com.weg.workshop.notification.repository;

import br.com.weg.workshop.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
    List<Notification> findByDeliveredAtIsNullAndScheduledAtLessThanEqual(Instant now);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
