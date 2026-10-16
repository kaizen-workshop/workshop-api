package br.com.weg.workshop.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordRulesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"fraca", "1234567", "somenteletras", "1234567890"})
    void rejectsWeakPasswordsOnChangeAndReset(String weak) {
        assertThat(validator.validate(new ChangePasswordRequest("current", weak))).isNotEmpty();
        assertThat(validator.validate(new ResetPasswordRequest("token", weak))).isNotEmpty();
    }

    @Test
    void acceptsPasswordWithLettersAndDigits() {
        assertThat(validator.validate(new ChangePasswordRequest("current", "Workshop@2026!"))).isEmpty();
        assertThat(validator.validate(new ResetPasswordRequest("token", "senhaforte1"))).isEmpty();
    }
}
