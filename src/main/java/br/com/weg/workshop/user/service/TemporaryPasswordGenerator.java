package br.com.weg.workshop.user.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
class TemporaryPasswordGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private final SecureRandom random = new SecureRandom();
    String generate() {
        StringBuilder password = new StringBuilder(16);
        for (int index = 0; index < 16; index++) password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return password.toString();
    }
}
