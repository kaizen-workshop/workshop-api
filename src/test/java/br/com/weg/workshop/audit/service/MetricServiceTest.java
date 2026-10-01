package br.com.weg.workshop.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.audit.dto.PostMetricResponse;
import br.com.weg.workshop.audit.dto.WorkshopMetricResponse;
import br.com.weg.workshop.post.domain.PostStatus;
import br.com.weg.workshop.post.repository.PostRepository;
import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {
    @Mock WorkshopRepository workshops;
    @Mock PostRepository posts;

    @Test void appliesFiltersAndReturnsPagedCounts() {
        MetricService service = new MetricService(workshops, posts);
        var page = PageRequest.of(0, 10);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        var workshop = new WorkshopMetricResponse(UUID.randomUUID(), "Java", WorkshopStatus.PUBLISHED,
                from, 4, 3, 2);
        var post = new PostMetricResponse(UUID.randomUUID(), "News", PostStatus.PUBLISHED, from, 5, 1);
        when(workshops.findMetrics(WorkshopStatus.PUBLISHED, from, to, page))
                .thenReturn(new PageImpl<>(List.of(workshop), page, 1));
        when(posts.findMetrics(PostStatus.PUBLISHED, from, to, page))
                .thenReturn(new PageImpl<>(List.of(post), page, 1));

        assertThat(service.workshops(WorkshopStatus.PUBLISHED, from, to, page).getContent())
                .containsExactly(workshop);
        assertThat(service.posts(PostStatus.PUBLISHED, from, to, page).getContent())
                .containsExactly(post);
        verify(workshops).findMetrics(WorkshopStatus.PUBLISHED, from, to, page);
        verify(posts).findMetrics(PostStatus.PUBLISHED, from, to, page);
        assertThatThrownBy(() -> service.posts(null, to, from, page))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
