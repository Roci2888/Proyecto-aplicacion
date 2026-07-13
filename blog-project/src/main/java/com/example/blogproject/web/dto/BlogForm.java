package com.example.blogproject.web.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de la frontera web para la creación de un blog (C8).
 * Restricciones de cota; ver nota de alcance en {@link RegisterForm}.
 */
public class BlogForm {

    @Size(max = 100, message = "El nombre del blog no puede superar los 100 caracteres.")
    private String name;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres.")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}