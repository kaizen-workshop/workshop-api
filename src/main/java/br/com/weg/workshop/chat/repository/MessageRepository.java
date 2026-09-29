package br.com.weg.workshop.chat.repository;

import br.com.weg.workshop.chat.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            where m.group.id = :groupId
              and (:cursorSentAt is null
                   or m.sentAt < :cursorSentAt
                   or (m.sentAt = :cursorSentAt and m.id < :cursorId))
            order by m.sentAt desc, m.id desc
            """)
    List<Message> findPage(
            @Param("groupId") UUID groupId,
            @Param("cursorSentAt") Instant cursorSentAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
