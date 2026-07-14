package com.example.blogproject.Unitarias;

import com.example.blogproject.domain.model.ModerationResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModerationResultTest {

    @Test
    void testAllowFactoryMethod() {
        ModerationResult result = ModerationResult.allow();

        assertTrue(result.isAllowed());
        assertEquals("OK", result.getReasonCode());
        assertEquals("Contenido permitido.", result.getUserMessage());
        assertEquals("La imagen ha pasado la moderación.", result.getDebugMessage());
    }

    @Test
    void testRejectFactoryMethod() {
        ModerationResult result = ModerationResult.reject("EXPLICIT_CONTENT", "Imagen rechazada", "Contenido no apto");

        assertFalse(result.isAllowed());
        assertEquals("EXPLICIT_CONTENT", result.getReasonCode());
        assertEquals("Imagen rechazada", result.getUserMessage());
        assertEquals("Contenido no apto", result.getDebugMessage());
    }

    @Test
    void testConstructor() {
        ModerationResult result = new ModerationResult(true, "CODE", "User msg", "Debug msg");

        assertTrue(result.isAllowed());
        assertEquals("CODE", result.getReasonCode());
        assertEquals("User msg", result.getUserMessage());
        assertEquals("Debug msg", result.getDebugMessage());
    }
}