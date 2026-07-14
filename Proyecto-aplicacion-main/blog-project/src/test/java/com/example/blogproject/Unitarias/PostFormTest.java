package com.example.blogproject.Unitarias;
import com.example.blogproject.web.dto.PostForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PostFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGettersAndSetters() {
        PostForm form = new PostForm();
        form.setTitle("Título");
        form.setContent("Contenido");
        form.setImageCaption("Pie de imagen");

        assertEquals("Título", form.getTitle());
        assertEquals("Contenido", form.getContent());
        assertEquals("Pie de imagen", form.getImageCaption());
    }

    @Test
    void testValidationExceedsLimits() {
        PostForm form = new PostForm();
        form.setTitle("a".repeat(201));        // Límite: 200
        form.setContent("b".repeat(20001));    // Límite: 20000
        form.setImageCaption("c".repeat(501)); // Límite: 500

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);
        assertEquals(3, violations.size());
    }
}
