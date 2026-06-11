package com.example.blogproject.controllers;

import com.example.blogproject.models.User;
import com.example.blogproject.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {

        User user = (User) session.getAttribute("usuario");

        if (user != null) {
            model.addAttribute("user", user);
        }

        return "index";
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session) {

        User user = (User) session.getAttribute("usuario");

        if (user != null) {
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin";
            } else {
                return "redirect:/user";
            }
        }

        return "register";

    }

    @PostMapping("/auth/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
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
            redirectAttributes.addFlashAttribute("error",
                    "Error al registrar usuario: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login(HttpSession session) {

        User user = (User) session.getAttribute("usuario");

        if (user != null) {
            // 🔥 Si ya está logueado, no mostrar login
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin";
            } else {
                return "redirect:/user";
            }
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

            session.setAttribute("usuario", user);

            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin";
            } else {return "redirect:/user";
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.invalidate();
            redirectAttributes.addFlashAttribute("error", "Error interno del servidor");
            return "redirect:/login?error";
        }
    }



    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {

        User user = (User) session.getAttribute("usuario");

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "admin"; //
    }

    @GetMapping("/admin/users")
    public String adminUsers(HttpSession session, Model model) {

        User user = (User) session.getAttribute("usuario");

        if (user == null || !"ADMIN".equals(user.getRole())) {
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

        User user = (User) session.getAttribute("usuario");

        // seguridad
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        userService.deleteUser(id);

        return "redirect:/admin";
    }


}