package br.com.weg.workshop.auth.domain;
import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="refresh_token", schema="workshop")
public class RefreshToken {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private UserEntity user;
 @Column(name="token_hash",nullable=false,unique=true) private String tokenHash; @Column(name="expires_at",nullable=false) private Instant expiresAt; @Column(name="revoked_at") private Instant revokedAt; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected RefreshToken(){}
 public static RefreshToken create(UserEntity user,String hash,Instant expiresAt){var t=new RefreshToken();t.id=UUID.randomUUID();t.user=user;t.tokenHash=hash;t.expiresAt=expiresAt;t.createdAt=Instant.now();return t;}
 public boolean isUsable(){return revokedAt==null && expiresAt.isAfter(Instant.now());} public void revoke(){revokedAt=Instant.now();} public UserEntity getUser(){return user;} public String getTokenHash(){return tokenHash;}
}
