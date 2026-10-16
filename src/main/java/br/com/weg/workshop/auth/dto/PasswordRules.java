package br.com.weg.workshop.auth.dto;

/** Minimum strength accepted for passwords chosen by users (change and reset). */
public final class PasswordRules {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;
    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).*$";
    public static final String MESSAGE = "A senha deve ter de 8 a 128 caracteres, com letras e números.";

    private PasswordRules() { }
}
