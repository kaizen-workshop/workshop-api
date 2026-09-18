package br.com.weg.workshop.preference.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserThemeId implements Serializable {
    private UUID user;
    private UUID theme;
    public UserThemeId() { }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserThemeId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(theme, that.theme);
    }

    @Override
    public int hashCode() { return Objects.hash(user, theme); }
}
