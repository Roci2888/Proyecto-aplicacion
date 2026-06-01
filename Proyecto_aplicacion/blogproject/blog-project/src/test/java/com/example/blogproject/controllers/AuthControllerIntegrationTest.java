package com.example.blogproject.controllers;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import com.example.blogproject.models.User;
import com.example.blogproject.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private MockHttpSession session;

    // ✅ Clase concreta interna
    private static class TestUser extends User {
        private String id;
        private String username;
        private String password;
        private String role;

        @Override
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @Override
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @Override
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @Override
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        userRepository.deleteAll();  // Limpiar base de datos
    }

    @Test
    void registerUser_ShouldNotCreateDuplicateUser() throws Exception {
        // ✅ CORREGIDO: Crear usuario duplicado correctamente
        createAndSaveUser("duplicate", "pass", "USER");

        // Intentar registrar el mismo usuario nuevamente
        mockMvc.perform(post("/auth/register")
                        .param("username", "duplicate")
                        .param("password", "newpass")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }


    private void createAndSaveUser(String username, String password, String role) {
        TestUser user = new TestUser();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        userRepository.save(user);
    }

    @Test
    void loginUser_ShouldAuthenticateAndRedirectToUserPanel() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    @Test
    void loginUser_ShouldAuthenticateAdminAndRedirectToAdminPanel() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginUser_ShouldFail_WhenInvalidCredentials() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void showRegister_ShouldShowRegisterForm_WhenNoUserLoggedIn() throws Exception {
        // When & Then
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void index_ShouldShowIndexWithoutUser_WhenNoUserLoggedIn() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))  // ← URI corregida: "/" en lugar de uriTemplate: "/"
                .andExpect(status().isOk())  // ← CORREGIDO: isOk() en lugar de isok()
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("user"));
    }
}