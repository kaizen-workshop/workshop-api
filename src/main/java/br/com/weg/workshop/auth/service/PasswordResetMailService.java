package br.com.weg.workshop.auth.service; import br.com.weg.workshop.user.domain.UserEntity; public interface PasswordResetMailService { void send(UserEntity user,String token); }
