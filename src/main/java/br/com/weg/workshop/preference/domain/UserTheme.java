package br.com.weg.workshop.preference.domain;

import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_theme", schema = "workshop")
@IdClass(UserThemeId.class)
public class UserTheme {
    @Id @ManyToOne @JoinColumn(name = "user_id") private UserEntity user;
    @Id @ManyToOne @JoinColumn(name = "theme_id") private Theme theme;
    protected UserTheme() { }
    public UserTheme(UserEntity user, Theme theme) { this.user = user; this.theme = theme; }
    public Theme getTheme() { return theme; }
}
