package br.com.weg.workshop.group.controller;

import br.com.weg.workshop.group.dto.GroupResponse;
import br.com.weg.workshop.group.service.GroupService;
import br.com.weg.workshop.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groups;

    public GroupController(GroupService groups) {
        this.groups = groups;
    }

    @GetMapping
    @Operation(summary = "List groups accessible to the authenticated user")
    public PageResponse<GroupResponse> list(
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable,
            Authentication authentication
    ) {
        return PageResponse.from(
                groups.list(userId(authentication), manager(authentication), admin(authentication), pageable));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get an accessible workshop group")
    public GroupResponse get(@PathVariable UUID groupId, Authentication authentication) {
        return groups.get(userId(authentication), admin(authentication), groupId);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean manager(Authentication authentication) {
        return hasRole(authentication, "ROLE_ARWEG") || admin(authentication);
    }

    private boolean admin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(role::equals);
    }
}
