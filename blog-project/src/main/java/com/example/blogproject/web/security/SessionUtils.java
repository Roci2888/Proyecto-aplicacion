package com.example.blogproject.web.security;

import com.example.blogproject.domain.model.User;
import jakarta.servlet.http.HttpSession;

/**
 * Utilidades para el manejo de la sesión del usuario autenticado.
 *
 */
public final class SessionUtils {

    /** Nombre del atributo bajo el cual se guarda el usuario en la sesión HTTP. */
    public static final String SESSION_USER = "usuario";

    /** Rol con privilegios de administración. */
    public static final String ROLE_ADMIN = "ADMIN";

    private SessionUtils() {
        // Clase de utilidad: no instanciable.
    }

    /**
     * Devuelve el usuario autenticado almacenado en la sesión, o {@code null} si
     * no hay ninguno.
     */
    public static User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(SESSION_USER);
    }

    /** Indica si el usuario existe y tiene rol de administrador. */
    public static boolean isAdmin(User user) {
        return user != null && ROLE_ADMIN.equals(user.getRole());
    }
}