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
    public String registerUser(@ModelAttribute User user) {
        userService.save(user);
        return "redirect:/login?success";
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
                            HttpSession session) {

        User user = userService.login(username, password);

        if (user == null) {
            return "redirect:/login?error";
        }

        session.setAttribute("usuario", user);

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin";
        } else {
            return "redirect:/user";
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