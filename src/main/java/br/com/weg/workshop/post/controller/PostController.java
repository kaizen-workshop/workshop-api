package br.com.weg.workshop.post.controller;

import br.com.weg.workshop.post.dto.CreateCommentRequest;
import br.com.weg.workshop.post.dto.PostCommentResponse;
import br.com.weg.workshop.post.dto.PostRequest;
import br.com.weg.workshop.post.dto.PostResponse;
import br.com.weg.workshop.post.dto.SchedulePostRequest;
import br.com.weg.workshop.post.service.PostService;
import br.com.weg.workshop.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(userId(authentication), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    public PostResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        return service.update(userId(authentication), admin(authentication), id, request);
    }

    @PatchMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    public PostResponse schedule(
            @PathVariable UUID id,
            @Valid @RequestBody SchedulePostRequest request,
            Authentication authentication
    ) {
        return service.schedule(userId(authentication), admin(authentication), id, request);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    public PostResponse publish(@PathVariable UUID id, Authentication authentication) {
        return service.publish(userId(authentication), admin(authentication), id);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ARWEG','ADMIN')")
    public PostResponse archive(@PathVariable UUID id, Authentication authentication) {
        return service.archive(userId(authentication), admin(authentication), id);
    }

    @GetMapping("/feed")
    @Operation(summary = "List visible personalized feed posts")
    public PageResponse<PostResponse> feed(
            @RequestParam(required = false) Instant updatedAfter,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.from(service.feed(userId(authentication), updatedAfter, pageable));
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<Void> like(@PathVariable UUID id, Authentication authentication) {
        service.like(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@PathVariable UUID id, Authentication authentication) {
        service.unlike(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<PostCommentResponse> comment(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.comment(userId(authentication), id, idempotencyKey, request));
    }

    @GetMapping("/{id}/comments")
    public PageResponse<PostCommentResponse> comments(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.from(service.comments(userId(authentication), id, pageable));
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public PostCommentResponse editComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return service.editComment(userId(authentication), postId, commentId, request);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        service.deleteComment(userId(authentication), admin(authentication), postId, commentId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
