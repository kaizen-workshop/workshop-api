package br.com.weg.workshop.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void exposesAStablePaginationEnvelope() {
        var page = new PageImpl<>(List.of("item"), PageRequest.of(1, 1), 3);

        var response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("item");
        assertThat(response.number()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.empty()).isFalse();
    }
}
