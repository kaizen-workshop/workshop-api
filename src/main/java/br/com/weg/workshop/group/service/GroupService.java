package br.com.weg.workshop.group.service;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.group.dto.GroupResponse;
import br.com.weg.workshop.group.repository.WorkshopGroupRepository;
import br.com.weg.workshop.registration.domain.RegistrationPaymentStatus;
import br.com.weg.workshop.registration.domain.RegistrationStatus;
import br.com.weg.workshop.registration.repository.RegistrationRepository;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

    private static final EnumSet<RegistrationPaymentStatus> VALID_PAYMENTS = EnumSet.of(
            RegistrationPaymentStatus.PAID,
            RegistrationPaymentStatus.EXEMPT
    );

    private final WorkshopGroupRepository groups;
    private final RegistrationRepository registrations;

    public GroupService(WorkshopGroupRepository groups, RegistrationRepository registrations) {
        this.groups = groups;
        this.registrations = registrations;
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> list(UUID userId, boolean manager, boolean admin, Pageable pageable) {
        Page<WorkshopGroup> result = admin
                ? groups.findAll(pageable)
                : manager
                ? groups.findManagedByUserId(userId, pageable)
                : groups.findAccessibleByUserId(userId, pageable);
        return result.map(GroupResponse::from);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID userId, boolean admin, UUID groupId) {
        return GroupResponse.from(requireAccess(userId, admin, groupId));
    }

    @Transactional(readOnly = true)
    public WorkshopGroup requireAccess(UUID userId, boolean admin, UUID groupId) {
        WorkshopGroup group = groups.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        if (!hasAccess(userId, admin, group)) {
            throw new ResourceNotFoundException("Group not found.");
        }
        return group;
    }

    public boolean isModerator(UUID userId, boolean admin, WorkshopGroup group) {
        return admin || group.getWorkshop().getCreatedBy().getId().equals(userId);
    }

    private boolean hasAccess(UUID userId, boolean admin, WorkshopGroup group) {
        return isModerator(userId, admin, group) || registrations
                .existsByUserIdAndWorkshopIdAndStatusAndPaymentStatusIn(
                        userId,
                        group.getWorkshop().getId(),
                        RegistrationStatus.CONFIRMED,
                        VALID_PAYMENTS
                );
    }
}
