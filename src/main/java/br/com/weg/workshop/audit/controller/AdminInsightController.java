package br.com.weg.workshop.audit.controller;

import br.com.weg.workshop.audit.dto.AuditResponse;
import br.com.weg.workshop.audit.dto.PostMetricResponse;
import br.com.weg.workshop.audit.dto.WorkshopMetricResponse;
import br.com.weg.workshop.audit.service.AuditService;
import br.com.weg.workshop.audit.service.MetricService;
import br.com.weg.workshop.post.domain.PostStatus;
import br.com.weg.workshop.workshop.domain.WorkshopStatus;
import br.com.weg.workshop.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminInsightController {
    private final MetricService metrics;
    private final AuditService audit;

    public AdminInsightController(MetricService metrics, AuditService audit) {
        this.metrics = metrics;
        this.audit = audit;
    }

    @GetMapping("/metrics/workshops")
    @Operation(summary = "List workshop metrics", description = "ADMIN only. Filters use an inclusive start and exclusive end time.")
    public PageResponse<WorkshopMetricResponse> workshopMetrics(
            @RequestParam(required = false) WorkshopStatus status,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(metrics.workshops(status, createdFrom, createdTo, pageable));
    }

    @GetMapping("/metrics/posts")
    @Operation(summary = "List post metrics", description = "ADMIN only. Filters use an inclusive start and exclusive end time.")
    public PageResponse<PostMetricResponse> postMetrics(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(metrics.posts(status, createdFrom, createdTo, pageable));
    }

    @GetMapping("/audit")
    @Operation(summary = "Search administrative audit history", description = "ADMIN only. Filters use an inclusive start and exclusive end time.")
    public PageResponse<AuditResponse> audit(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return PageResponse.from(audit.search(userId, action, entity, entityId, from, to, pageable));
    }
}
