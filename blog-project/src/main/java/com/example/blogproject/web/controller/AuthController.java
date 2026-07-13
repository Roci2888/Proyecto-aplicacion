package com.example.blogproject.web.controller;

import com.example.blogproject.domain.model.User;
import com.example.blogproject.application.service.UserService;
import com.example.blogproject.web.dto.RegisterForm;
import com.example.blogproject.web.security.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private UserService userService;

    /** Devuelve la ruta del panel principal según el rol del usuario. */
    private String homeRedirectFor(User user) {
        return SessionUtils.isAdmin(user) ? "redirect:/admin" : "redirect:/user";
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {

        User user = SessionUtils.getCurrentUser(session);

        if (user != null) {
            model.addAttribute("user", user);
        }

        return "index";
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session) {

        User user = SessionUtils.getCurrentUser(session);

        if (user != null) {
            return homeRedirectFor(user);
        }

        return "register";

    }

    @PostMapping("/auth/register")
    public String registerUser(@Valid @ModelAttribute RegisterForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        // C8: rechazar entradas que superen las cotas antes de tocar la base de datos.
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/register";
        }

        // El formulario nunca vincula campos sensibles del dominio (p. ej. role).
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(form.getPassword());
        try {
            // Verificar si el username ya existe
            if (userService.existsByUsername(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error",
                        "El usuario '" + user.getUsername() + "' ya existe. Por favor elige otro nombre.");
                return "redirect:/register";
            }
            userService.save(user);
            redirectAttributes.addFlashAttribute("success",
                    "¡Registro exitoso! Ahora puedes iniciar sesión.");
            return "redirect:/login?success";
        } catch (Exception e) {
            // C9: registrar el detalle técnico, pero no filtrarlo al usuario.
            logger.error("Error al registrar usuario '{}'", user != null ? user.getUsername() : null, e);
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo completar el registro. Inténtalo de nuevo más tarde.");
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login(HttpSession session) {

        User user = SessionUtils.getCurrentUser(session);

        if (user != null) {
            // 🔥 Si ya está logueado, no mostrar login
            return homeRedirectFor(user);
        }

        return "login";
    }


    @PostMapping("/auth/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.login(username, password);

            if (user == null) {
                session.invalidate();
                redirectAttributes.addFlashAttribute("error",
                        "Usuario o contraseña incorrectos");
                return "redirect:/login?error";
            }

            session.setAttribute(SessionUtils.SESSION_USER, user);

            return homeRedirectFor(user);

        } catch (Exception e) {
            logger.error("Error durante el inicio de sesión para usuario: {}", username, e);

            session.invalidate();
            redirectAttributes.addFlashAttribute("error", "Error interno del servidor");
            return "redirect:/login?error";
        }
    }



    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {

        User user = SessionUtils.getCurrentUser(session);

        if (!SessionUtils.isAdmin(user)) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "admin"; //
    }

    @GetMapping("/admin/users")
    public String adminUsers(HttpSession session, Model model) {

        User user = SessionUtils.getCurrentUser(session);

        if (!SessionUtils.isAdmin(user)) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("users", userService.getAllUsers());

        return "admin-users";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteUser(@PathVariable String id,
                             HttpSession session) {

        User user = SessionUtils.getCurrentUser(session);

        // seguridad
        if (!SessionUtils.isAdmin(user)) {
            return "redirect:/login";
        }

        userService.deleteUser(id);

        return "redirect:/admin";
    }


}