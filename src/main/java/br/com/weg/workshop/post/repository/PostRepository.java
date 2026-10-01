package br.com.weg.workshop.post.repository; import br.com.weg.workshop.audit.dto.PostMetricResponse; import br.com.weg.workshop.post.domain.*; import java.time.Instant; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; public interface PostRepository extends JpaRepository<Post,UUID>{
 @Query(value="select new br.com.weg.workshop.audit.dto.PostMetricResponse(p.id, p.title, p.status, p.createdAt, "
 + "(select count(l) from PostLike l where l.post = p), "
 + "(select count(c) from PostComment c where c.post = p)) "
 + "from Post p where (:status is null or p.status = :status) "
 + "and (cast(:createdFrom as timestamp) is null or p.createdAt >= :createdFrom) "
 + "and (cast(:createdTo as timestamp) is null or p.createdAt < :createdTo)",
 countQuery="select count(p) from Post p where (:status is null or p.status = :status) "
 + "and (cast(:createdFrom as timestamp) is null or p.createdAt >= :createdFrom) "
 + "and (cast(:createdTo as timestamp) is null or p.createdAt < :createdTo)")
 Page<PostMetricResponse> findMetrics(@Param("status") PostStatus status,
     @Param("createdFrom") Instant createdFrom, @Param("createdTo") Instant createdTo, Pageable pageable);
 @Query("select p from Post p where p.status = 'PUBLISHED' order by p.highlight desc, "
 + "case when exists (select ut from UserTheme ut where ut.user.id = :userId and p.workshop is not null and ut.theme.id = p.workshop.theme.id) then 0 else 1 end, "
 + "case when p.workshop is not null and p.workshop.startDate >= current_date and p.workshop.registrationStart <= current_timestamp and p.workshop.registrationEnd >= current_timestamp then 0 else 1 end, p.publishedAt desc, p.id desc")
 Page<Post> findFeed(@Param("userId") UUID userId, Pageable pageable); List<Post> findByStatusAndScheduledAtLessThanEqual(PostStatus status,Instant instant); }
