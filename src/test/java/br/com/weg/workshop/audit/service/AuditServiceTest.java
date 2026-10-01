package br.com.weg.workshop.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.audit.domain.AdministrativeAudit;
import br.com.weg.workshop.audit.repository.AdministrativeAuditRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    @Mock AdministrativeAuditRepository repository;

    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test void capturesActorChangeAndClientIpFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        UUID actor = UUID.randomUUID();
        UUID entity = UUID.randomUUID();

        new AuditService(repository).record(actor, "PUBLISH", "WORKSHOP", entity, "DRAFT", "PUBLISHED");

        ArgumentCaptor<AdministrativeAudit> capture = ArgumentCaptor.forClass(AdministrativeAudit.class);
        verify(repository).save(capture.capture());
        assertThat(capture.getValue().getUserId()).isEqualTo(actor);
        assertThat(capture.getValue().getEntityId()).isEqualTo(entity);
        assertThat(capture.getValue().getPreviousValue()).isEqualTo("DRAFT");
        assertThat(capture.getValue().getNewValue()).isEqualTo("PUBLISHED");
        assertThat(capture.getValue().getIp()).isEqualTo("192.0.2.10");
    }

    @Test void searchesAuditWithoutRequestAndValidatesTimeWindow() {
        AuditService service = new AuditService(repository);
        UUID actor = UUID.randomUUID();
        service.record(actor, "CREATE", "USER", UUID.randomUUID(), null, "PENDING");
        ArgumentCaptor<AdministrativeAudit> capture = ArgumentCaptor.forClass(AdministrativeAudit.class);
        verify(repository).save(capture.capture());
        assertThat(capture.getValue().getIp()).isNull();

        var page = PageRequest.of(0, 20);
        when(repository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(capture.getValue()), page, 1));
        assertThat(service.search(actor, "CREATE", "USER", null, null, null, page).getContent())
                .singleElement().satisfies(result -> assertThat(result.userId()).isEqualTo(actor));
        Instant now = Instant.now();
        assertThatThrownBy(() -> service.search(null, null, null, null, now, now, page))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
