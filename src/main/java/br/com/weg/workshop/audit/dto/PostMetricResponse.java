package br.com.weg.workshop.audit.dto;

import br.com.weg.workshop.post.domain.PostStatus;
import java.time.Instant;
import java.util.UUID;

public record PostMetricResponse(UUID id, String title, PostStatus status, Instant createdAt,
                                 long likes, long comments) { }
