package com.example.blogproject.Unitarias;
import com.example.blogproject.web.dto.BlogForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlogFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        BlogForm form = new BlogForm();
        form.setName("Mi Blog");
        form.setDescription("Descripción del blog");

        assertEquals("Mi Blog", form.getName());
        assertEquals("Descripción del blog", form.getDescription());
    }

    @Test
    void testValidationExceedsLimits() {
        BlogForm form = new BlogForm();
        form.setName("a".repeat(101));       // Límite: 100
        form.setDescription("b".repeat(2001)); // Límite: 2000

        Set<ConstraintViolation<BlogForm>> violations = validator.validate(form);
        assertEquals(2, violations.size());
    }
}
