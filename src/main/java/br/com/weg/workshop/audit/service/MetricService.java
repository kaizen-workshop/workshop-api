package br.com.weg.workshop.audit.service;

import br.com.weg.workshop.audit.dto.PostMetricResponse;
import br.com.weg.workshop.audit.dto.WorkshopMetricResponse;
import br.com.weg.workshop.post.domain.PostStatus;
import br.com.weg.workshop.post.repository.PostRepository;
import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricService {
    private final WorkshopRepository workshops;
    private final PostRepository posts;

    public MetricService(WorkshopRepository workshops, PostRepository posts) {
        this.workshops = workshops;
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public Page<WorkshopMetricResponse> workshops(WorkshopStatus status, Instant createdFrom,
                                                   Instant createdTo, Pageable pageable) {
        validatePeriod(createdFrom, createdTo);
        return workshops.findMetrics(status, createdFrom, createdTo, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostMetricResponse> posts(PostStatus status, Instant createdFrom,
                                          Instant createdTo, Pageable pageable) {
        validatePeriod(createdFrom, createdTo);
        return posts.findMetrics(status, createdFrom, createdTo, pageable);
    }

    private void validatePeriod(Instant from, Instant to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("The end of the metric period must be after its start.");
        }
    }
}
