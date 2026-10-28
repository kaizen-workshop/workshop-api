package br.com.weg.workshop.group.service;

import br.com.weg.workshop.group.domain.WorkshopGroup;
import br.com.weg.workshop.group.repository.WorkshopGroupRepository;
import br.com.weg.workshop.workshop.domain.Workshop;
import org.springframework.stereotype.Service;

@Service
public class GroupLifecycleService {

    private final WorkshopGroupRepository groups;

    public GroupLifecycleService(WorkshopGroupRepository groups) {
        this.groups = groups;
    }

    public WorkshopGroup createFor(Workshop workshop) {
        return groups.save(WorkshopGroup.create(workshop));
    }

    public void activateFor(Workshop workshop) {
        groupFor(workshop).activate();
    }

    public void deactivateFor(Workshop workshop) {
        groupFor(workshop).deactivate();
    }

    private WorkshopGroup groupFor(Workshop workshop) {
        return groups.findByWorkshopId(workshop.getId())
                .orElseThrow(() -> new IllegalStateException("Workshop group is missing."));
    }
}
