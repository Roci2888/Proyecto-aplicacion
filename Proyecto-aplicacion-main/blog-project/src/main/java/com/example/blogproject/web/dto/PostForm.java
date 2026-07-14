package com.example.blogproject.web.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de la frontera web para la creación de un post (C8).
 * Restricciones de cota; ver nota de alcance en {@link RegisterForm}.
 * El fichero de imagen viaja aparte como {@code MultipartFile}.
 */
public class PostForm {

    @Size(max = 200, message = "El título no puede superar los 200 caracteres.")
    private String title;

    @Size(max = 20000, message = "El contenido no puede superar los 20000 caracteres.")
    private String content;

    @Size(max = 500, message = "El pie de imagen no puede superar los 500 caracteres.")
    private String imageCaption;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageCaption() {
        return imageCaption;
    }

    public void setImageCaption(String imageCaption) {
        this.imageCaption = imageCaption;
    }
}