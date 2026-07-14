package com.example.blogproject.Unitarias;

import com.example.blogproject.web.dto.RegisterForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        RegisterForm form = new RegisterForm();
        form.setUsername("admin");
        form.setPassword("123456");

        assertEquals("admin", form.getUsername());
        assertEquals("123456", form.getPassword());
    }

    @Test
    void testValidationSuccess() {
        RegisterForm form = new RegisterForm();
        form.setUsername("usuarioValido");
        form.setPassword("password123");

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidationFailureExceedsSize() {
        RegisterForm form = new RegisterForm();
        form.setUsername("a".repeat(51)); // Supera los 50 caracteres
        form.setPassword("b".repeat(101)); // Supera los 100 caracteres

        Set<ConstraintViolation<RegisterForm>> violations = validator.validate(form);
        assertEquals(2, violations.size());
    }
}
