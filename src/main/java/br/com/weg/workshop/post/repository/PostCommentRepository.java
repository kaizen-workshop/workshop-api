package br.com.weg.workshop.post.repository;

import br.com.weg.workshop.post.domain.PostComment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    Page<PostComment> findByPostId(UUID postId, Pageable pageable);
    Optional<PostComment> findByUserIdAndClientOperationId(UUID userId, UUID clientOperationId);
}
