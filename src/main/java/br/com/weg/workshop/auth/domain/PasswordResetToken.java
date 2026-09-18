package br.com.weg.workshop.auth.domain;
import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="password_reset_token",schema="workshop")
public class PasswordResetToken {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private UserEntity user; @Column(name="token_hash",nullable=false,unique=true) private String tokenHash; @Column(name="expires_at",nullable=false) private Instant expiresAt; @Column(name="used_at") private Instant usedAt; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected PasswordResetToken(){} public static PasswordResetToken create(UserEntity u,String h,Instant e){var t=new PasswordResetToken();t.id=UUID.randomUUID();t.user=u;t.tokenHash=h;t.expiresAt=e;t.createdAt=Instant.now();return t;} public boolean isUsable(){return usedAt==null&&expiresAt.isAfter(Instant.now());} public void use(){usedAt=Instant.now();} public UserEntity getUser(){return user;}
}
