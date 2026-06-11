package com.example.blogproject.controllers;

import com.example.blogproject.models.User;
import com.example.blogproject.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // Página principal
    @GetMapping("/")
    public String index(HttpSession session, Model model) {// pasar datos a la vista

        // Intenta recuperar el objeto "usuario" de la sesión actual
        User user = (User) session.getAttribute("usuario");

        // Si el usuario ya inició sesión previamente (el objeto no es nulo)...
        if (user != null) {
            // ...añade los datos del usuario al HTML (ej. "Bienvenido, Juan")
            model.addAttribute("user", user);
        }

        // Si no está logueado, se salta el 'if' anterior y carga la vista "index" como usuario anónimo
        return "index";
    }

    // Mostrar formulario de registro
    @GetMapping("/register")
    public String showRegister(HttpSession session) { // verifica estado de la sesion

        // Recuperar el objeto "usuario" almacenado en la sesión y lo convierte a la clase User
        User user = (User) session.getAttribute("usuario");

        // Si el usuario ya inició sesión (no es nulo)...
        if (user != null) {
            // ... y si su rol es "ADMIN"
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin"; // Lo redirige al panel de administración
            } else { //Si tiene otro rol (usuario común)
                return "redirect:/user"; // Lo redirige a la vista de usuario estándar
            }
        }

        // Si no había ninguna sesión activa carga la vista "register"
        return "register";
    }

    // Registrar nuevo usuario
    @PostMapping("/auth/register")
    public String registerUser(@ModelAttribute User user) { // Vincula los datos del formulario con un objeto User
        userService.save(user); // Llama al servicio para guardar el nuevo usuario en la base de datos
        return "redirect:/login?success"; // Redirige al login enviando un parámetro en la URL indicando éxito
    }

    // Mostrar login
    @GetMapping("/login") // Responde a peticiones HTTP GET en la ruta /login
    public String login(HttpSession session) { // Recibe la sesión para comprobar si el usuario ya está autenticado

        // Recupera el usuario de la sesión actual
        User user = (User) session.getAttribute("usuario");

        // Si el usuario ya está logueado...
        if (user != null) {
            // 🔥 Si ya está logueado, evita mostrar el login y lo redirige según su rol:
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin"; // Redirige al panel de admin
            } else {
                return "redirect:/user"; // Redirige al panel de usuario
            }
        }

        // Si no está logueado, muestra la vista (HTML) de la pantalla de inicio de sesión "login"
        return "login";
    }

    // Iniciar sesión
    @PostMapping("/auth/login")
    public String loginUser(@RequestParam String username, // Captura el parámetro "username" enviado desde el formulario
                            @RequestParam String password, // Captura el parámetro "password" enviado desde el formulario
                            HttpSession session) { // Recibe la sesión para poder guardar al usuario si se autentica con éxito

        // Llama al servicio de usuarios para validar las credenciales en la base de datos
        User user = userService.login(username, password);

        // Si las credenciales son incorrectas o el usuario no existe (el servicio devuelve null)...
        if (user == null) {
            return "redirect:/login?error"; // Redirige de vuelta al login mostrando un mensaje de error por URL
        }

        // Si las credenciales son correctas, guarda el objeto usuario en la sesión bajo el nombre "usuario"
        session.setAttribute("usuario", user);

        // Redirige al usuario a su panel correspondiente según el rol que tenga asignado
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin"; // Si es admin, va a la sección de administración
        } else {
            return "redirect:/user"; // Si no, va a su sección de usuario
        }
    }

    // Panel de administrador
    @GetMapping("/admin") // Responde a peticiones HTTP GET en la ruta /admin
    public String admin(HttpSession session, Model model) { // Recibe la sesión y el "Model" (para enviar datos a la vista)

        // Recupera al usuario que está intentando ingresar
        User user = (User) session.getAttribute("usuario");

        // Control de seguridad manual: si no hay sesión O el usuario no tiene el rol de "ADMIN"
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login"; // Bloquea el acceso y lo redirige al inicio de sesión
        }

        // Agrega el objeto del administrador al modelo para poder pintar sus datos (ej. su nombre) en el HTML
        model.addAttribute("user", user);

        // Renderiza la plantilla HTML exclusiva para administradores llamada "admin"
        return "admin";
    }

    // Lista de usuarios (admin)
    @GetMapping("/admin/users") // Responde a peticiones HTTP GET en la ruta /admin/users
    public String adminUsers(HttpSession session, Model model) { // Recibe la sesión y el modelo para los datos

        // Recupera al usuario de la sesión para validar su identidad
        User user = (User) session.getAttribute("usuario");

        // Control de seguridad: restringe el acceso solo si es una sesión válida de administrador
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login"; // Redirige al login si no está autorizado
        }

        // Pasa los datos del administrador actual a la vista
        model.addAttribute("user", user);
        // Pasa la lista completa de todos los usuarios registrados (obtenida del servicio) a la vista
        model.addAttribute("users", userService.getAllUsers());

        // Carga la vista HTML "admin-users" donde se mostrará la tabla con la lista de usuarios
        return "admin-users";
    }

    // Cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) { // Recibe la sesión activa
        session.invalidate(); // Destruye por completo la sesión actual borrando todos sus datos guardados
        return "redirect:/login"; // Redirige al usuario limpio de vuelta a la pantalla de login
    }

    // Eliminar usuario (admin)
    @PostMapping("/admin/delete/{id}") // Responde a peticiones POST en la ruta /admin/delete/ pasándole el ID dinámicamente
    public String deleteUser(@PathVariable String id, // Captura el {id} que viene directamente en la URL del navegador
                             HttpSession session) { // Recibe la sesión para validar permisos antes de borrar

        // Recupera al usuario que realiza la petición
        User user = (User) session.getAttribute("usuario");

        // Validación de seguridad estricta en el servidor
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login"; // Si no es admin, no le permite borrar y lo manda al login
        }

        // Si pasa la seguridad, invoca al servicio para eliminar de la base de datos al usuario con el ID capturado
        userService.deleteUser(id);

        // Redirige de vuelta al panel principal de administración para refrescar los datos reflejados
        return "redirect:/admin";
    }

}