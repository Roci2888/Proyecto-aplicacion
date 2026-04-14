package com.example.demo;

public class Post {
    private String titulo;
    private String texto;
    private String nombreImagen;

    // Constructor vacío
    public Post() {}

    // Constructor con parámetros
    public Post(String titulo, String texto, String nombreImagen) {
        this.titulo = titulo;
        this.texto = texto;
        this.nombreImagen = nombreImagen;
    }

    // Getters y Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getNombreImagen() {
        return nombreImagen;
    }

    public void setNombreImagen(String nombreImagen) {
        this.nombreImagen = nombreImagen;
    }
}
