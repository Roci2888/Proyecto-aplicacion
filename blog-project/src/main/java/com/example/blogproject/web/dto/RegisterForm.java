package com.example.blogproject.web.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de la frontera web para el registro de usuarios (C8).
 *
 * <p>Se usa en lugar de vincular la entidad {@code User} directamente en el
 * controlador: así el formulario nunca expone/rellena campos sensibles del
 * dominio (p. ej. {@code role}) desde la petición.
 *
 * <p><b>Nota de alcance:</b> las restricciones son de <em>cota</em>
 * ({@link Size} máximo) para acotar entradas maliciosas/desbordadas sin alterar
 * el comportamiento existente (el sistema aún acepta valores vacíos). Endurecer a
 * {@code @NotBlank} es un cambio funcional (Fase 4) que requiere actualizar los
 * tests que hoy verifican que se aceptan campos vacíos.
 */
public class RegisterForm {

    @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres.")
    private String username;

    @Size(max = 100, message = "La contraseña no puede superar los 100 caracteres.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}