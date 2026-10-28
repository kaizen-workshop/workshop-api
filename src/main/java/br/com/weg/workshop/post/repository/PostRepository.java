package br.com.weg.workshop.post.repository;

import br.com.weg.workshop.audit.dto.PostMetricResponse;
import br.com.weg.workshop.post.domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query(value = "select new br.com.weg.workshop.audit.dto.PostMetricResponse(p.id, p.title, p.status, p.createdAt, "
            + "(select count(l) from PostLike l where l.post = p), "
            + "(select count(c) from PostComment c where c.post = p)) "
            + "from Post p where (:status is null or p.status = :status) "
            + "and (cast(:createdFrom as timestamp) is null or p.createdAt >= :createdFrom) "
            + "and (cast(:createdTo as timestamp) is null or p.createdAt < :createdTo)",
            countQuery = "select count(p) from Post p where (:status is null or p.status = :status) "
                    + "and (cast(:createdFrom as timestamp) is null or p.createdAt >= :createdFrom) "
                    + "and (cast(:createdTo as timestamp) is null or p.createdAt < :createdTo)")
    Page<PostMetricResponse> findMetrics(@Param("status") PostStatus status,
                                         @Param("createdFrom") Instant createdFrom,
                                         @Param("createdTo") Instant createdTo, Pageable pageable);

    // Explicit left join: implicit p.workshop.* paths become inner joins and would hide posts without a workshop.
    @Query(value = "select p from Post p left join p.workshop w where p.status = 'PUBLISHED' "
            + "and (cast(:updatedAfter as timestamp) is null or p.updatedAt > :updatedAfter) "
            + "order by p.highlight desc, "
            + "case when w is not null and exists "
            + "(select ut from UserTheme ut where ut.user.id = :userId and ut.theme = w.theme) then 0 else 1 end, "
            + "case when w is not null and w.startDate >= current_date and w.registrationStart <= current_timestamp "
            + "and w.registrationEnd >= current_timestamp then 0 else 1 end, "
            + "p.publishedAt desc, p.id desc",
            countQuery = "select count(p) from Post p where p.status = 'PUBLISHED' "
                    + "and (cast(:updatedAfter as timestamp) is null or p.updatedAt > :updatedAfter)")
    Page<Post> findFeed(@Param("userId") UUID userId, @Param("updatedAfter") Instant updatedAfter,
                        Pageable pageable);

    List<Post> findByStatusAndScheduledAtLessThanEqual(PostStatus status, Instant instant);

    @Query(value = "select p from Post p where :admin = true or p.createdBy.id = :userId",
            countQuery = "select count(p) from Post p where :admin = true or p.createdBy.id = :userId")
    Page<Post> findManaged(@Param("userId") UUID userId, @Param("admin") boolean admin, Pageable pageable);
}
