package br.com.weg.workshop.registration.repository;

import br.com.weg.workshop.registration.domain.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    boolean existsByUserIdAndWorkshopIdAndStatusIn(UUID userId, UUID workshopId, Collection<RegistrationStatus> statuses);

    boolean existsByUserIdAndWorkshopIdAndStatusAndPaymentStatusIn(
            UUID userId,
            UUID workshopId,
            RegistrationStatus status,
            Collection<RegistrationPaymentStatus> paymentStatuses
    );

    long countByWorkshopIdAndStatusIn(UUID workshopId, Collection<RegistrationStatus> statuses);

    @Query("select r from Registration r join r.user u where r.workshop.id = :workshopId and r.status = 'WAITING_LIST' "
            + "and u.status = 'ACTIVE' order by r.registeredAt asc, r.id asc")
    Optional<Registration> findFirstEligibleWaitingListEntry(@Param("workshopId") UUID workshopId);

    @Query("select r from Registration r where r.workshop.id = :workshopId and (:status is null or r.status = :status)")
    Page<Registration> findByWorkshop(@Param("workshopId") UUID workshopId, @Param("status") RegistrationStatus status, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId and r.status in :statuses "
            + "and r.workshop.endDate < :today")
    Page<Registration> findCompletedByUser(@Param("userId") UUID userId,
                                           @Param("statuses") Collection<RegistrationStatus> statuses,
                                           @Param("today") LocalDate today, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId and r.status in :statuses "
            + "and (:from is null or r.workshop.startDate >= :from) and (:to is null or r.workshop.startDate <= :to)")
    Page<Registration> findCalendarByUser(@Param("userId") UUID userId,
                                          @Param("statuses") Collection<RegistrationStatus> statuses,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
