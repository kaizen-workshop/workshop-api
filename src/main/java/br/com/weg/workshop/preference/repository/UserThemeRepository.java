package br.com.weg.workshop.preference.repository;

import br.com.weg.workshop.preference.domain.UserTheme;
import br.com.weg.workshop.preference.domain.UserThemeId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserThemeRepository extends JpaRepository<UserTheme, UserThemeId> {
    void deleteByUserId(UUID userId);
    List<UserTheme> findByUserId(UUID userId);
}
