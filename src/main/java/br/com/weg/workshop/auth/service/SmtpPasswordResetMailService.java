package br.com.weg.workshop.auth.service;

import br.com.weg.workshop.user.domain.UserEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class SmtpPasswordResetMailService implements PasswordResetMailService {

    private final JavaMailSender mail;

    SmtpPasswordResetMailService(JavaMailSender mail) {
        this.mail = mail;
    }

    public void send(UserEntity user, String token) {
        var message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Redefinição de senha");
        message.setText("Olá, " + user.getName() + ".\n\n"
                + "Use o código abaixo na tela de recuperação de senha do aplicativo. "
                + "Ele vale por 1 hora e só pode ser usado uma vez:\n\n"
                + token + "\n\n"
                + "Se você não pediu a redefinição, ignore este e-mail.");
        mail.send(message);
    }
}
