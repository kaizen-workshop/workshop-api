package br.com.weg.workshop.user.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class SmtpInitialAccessMailService implements InitialAccessMailService {
    private final JavaMailSender mailSender;
    SmtpInitialAccessMailService(JavaMailSender mailSender) { this.mailSender = mailSender; }
    public void send(String recipient, String username, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject("Workshop API initial access");
        message.setText("Username: " + username + "\nTemporary password: " + temporaryPassword);
        mailSender.send(message);
    }
}
