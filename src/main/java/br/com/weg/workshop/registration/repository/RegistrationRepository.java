package br.com.weg.workshop.registration.repository;

import br.com.weg.workshop.registration.domain.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    boolean existsByUserIdAndWorkshopIdAndStatusIn(UUID userId, UUID workshopId, Collection<RegistrationStatus> statuses);

    Optional<Registration> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    boolean existsByUserIdAndCancellationIdempotencyKey(UUID userId, UUID cancellationIdempotencyKey);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Registration r where r.id = :id")
    Optional<Registration> findByIdForUpdate(@Param("id") UUID id);

    Optional<Registration> findFirstByUserIdAndWorkshopIdOrderByCreatedAtDesc(UUID userId, UUID workshopId);

    @Query(value = "select count(*) from workshop.registration r where r.workshop_id = :workshopId "
            + "and r.status = 'WAITING_LIST' and (r.registered_at, r.id) < (:registeredAt, :registrationId)",
            nativeQuery = true)
    long countWaitingAhead(@Param("workshopId") UUID workshopId, @Param("registeredAt") java.time.Instant registeredAt,
                           @Param("registrationId") UUID registrationId);

    boolean existsByUserIdAndWorkshopIdAndStatusAndPaymentStatusIn(
            UUID userId,
            UUID workshopId,
            RegistrationStatus status,
            Collection<RegistrationPaymentStatus> paymentStatuses
    );

    long countByWorkshopIdAndStatusIn(UUID workshopId, Collection<RegistrationStatus> statuses);

    @Query(value = "select r.* from workshop.registration r join workshop.app_user u on u.id = r.user_id "
            + "where r.workshop_id = :workshopId and r.status = 'WAITING_LIST' and u.status = 'ACTIVE' "
            + "order by r.registered_at asc, r.id asc limit 1 for update", nativeQuery = true)
    Optional<Registration> findFirstEligibleWaitingListEntry(@Param("workshopId") UUID workshopId);

    @Query("select r from Registration r where r.workshop.id = :workshopId and (:status is null or r.status = :status)")
    Page<Registration> findByWorkshop(@Param("workshopId") UUID workshopId, @Param("status") RegistrationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "attendanceMarkedBy"})
    @Query("select r from Registration r where r.workshop.id = :workshopId "
            + "and (:registrationStatus is null or r.status = :registrationStatus) "
            + "and (:paymentStatus is null or r.paymentStatus = :paymentStatus) "
            + "and (:attendanceStatus is null or r.attendanceStatus = :attendanceStatus)")
    Page<Registration> findParticipants(@Param("workshopId") UUID workshopId,
                                        @Param("registrationStatus") RegistrationStatus registrationStatus,
                                        @Param("paymentStatus") RegistrationPaymentStatus paymentStatus,
                                        @Param("attendanceStatus") AttendanceStatus attendanceStatus,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"user", "attendanceMarkedBy"})
    @Query("select r from Registration r where r.workshop.id = :workshopId "
            + "and (:registrationStatus is null or r.status = :registrationStatus) "
            + "and (:paymentStatus is null or r.paymentStatus = :paymentStatus) "
            + "and (:attendanceStatus is null or r.attendanceStatus = :attendanceStatus)")
    List<Registration> findParticipants(@Param("workshopId") UUID workshopId,
                                        @Param("registrationStatus") RegistrationStatus registrationStatus,
                                        @Param("paymentStatus") RegistrationPaymentStatus paymentStatus,
                                        @Param("attendanceStatus") AttendanceStatus attendanceStatus,
                                        Sort sort);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "workshop"})
    @Query("select r from Registration r where r.id in :ids")
    List<Registration> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    @Query("select count(r) from Registration r where (:admin = true or r.workshop.createdBy.id = :managerId) "
            + "and (:registrationStatus is null or r.status = :registrationStatus) "
            + "and (:attendanceStatus is null or r.attendanceStatus = :attendanceStatus)")
    long countManaged(@Param("managerId") UUID managerId, @Param("admin") boolean admin,
                      @Param("registrationStatus") RegistrationStatus registrationStatus,
                      @Param("attendanceStatus") AttendanceStatus attendanceStatus);

    @Query("select r from Registration r where r.user.id = :userId and r.status in :statuses "
            + "and r.workshop.endDate < :today")
    Page<Registration> findCompletedByUser(@Param("userId") UUID userId,
                                           @Param("statuses") Collection<RegistrationStatus> statuses,
                                           @Param("today") LocalDate today, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId "
            + "and r.status in :statuses and r.workshop.startDate > :today")
    Page<Registration> findFutureByUser(@Param("userId") UUID userId,
                                        @Param("statuses") Collection<RegistrationStatus> statuses,
                                        @Param("today") LocalDate today, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId and r.status = 'CONFIRMED' "
            + "and r.workshop.startDate <= :today and r.workshop.endDate >= :today")
    Page<Registration> findInProgressByUser(@Param("userId") UUID userId,
                                            @Param("today") LocalDate today, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId "
            + "and (r.status in ('CANCELLED', 'REFUNDED') or r.workshop.status = 'CANCELLED')")
    Page<Registration> findCancelledByUser(@Param("userId") UUID userId, Pageable pageable);

    Page<Registration> findByUserIdAndStatus(UUID userId, RegistrationStatus status, Pageable pageable);

    @Query("select r from Registration r where r.user.id = :userId and r.status in :statuses "
            + "and (cast(:from as date) is null or r.workshop.startDate >= :from) "
            + "and (cast(:to as date) is null or r.workshop.startDate <= :to)")
    Page<Registration> findCalendarByUser(@Param("userId") UUID userId,
                                          @Param("statuses") Collection<RegistrationStatus> statuses,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
