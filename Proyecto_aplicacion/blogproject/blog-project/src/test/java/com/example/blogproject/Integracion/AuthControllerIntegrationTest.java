package com.example.blogproject.Integracion;

import com.example.blogproject.models.User;
import com.example.blogproject.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Limpia la colección de usuarios antes de cada prueba
        userRepository.deleteAll();
    }

    @Test
    void registerUser_ShouldSaveUserInDBAndRedirect() throws Exception {
        // Ejecutamos la petición POST real
        mockMvc.perform(post("/auth/register")
                        .param("username", "nuevousuario")
                        .param("password", "secure123")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success"));

        // VERIFICACIÓN: El usuario debe existir en MongoDB
        boolean userExists = userRepository.findByUsername("nuevousuario").isPresent();
        assertTrue(userExists, "El usuario debería haberse guardado en MongoDB");

        // Verificar datos guardados
        User savedUser = userRepository.findByUsername("nuevousuario").get();
        assertEquals("nuevousuario", savedUser.getUsername());
        assertEquals("secure123", savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());

        System.out.println("Usuario guardado con ID: " + savedUser.getId());
    }

    @Test
    void registerUser_ShouldFail_WhenUserAlreadyExists() throws Exception {
        // Crear usuario existente
        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setPassword("password123");
        existingUser.setRole("USER");
        userRepository.save(existingUser);

        // Intentar registrar el mismo usuario
        mockMvc.perform(post("/auth/register")
                        .param("username", "testuser")
                        .param("password", "otraClave")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        // Verificar que no hay duplicados
        long count = userRepository.count();
        assertEquals(1, count, "No debería haber usuarios duplicados en MongoDB");
    }

    @Test
    void loginUser_ShouldSucceedWithRealCredentials() throws Exception {
        // Crear usuario en MongoDB
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRole("USER");
        userRepository.save(user);

        // Probar login
        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    @Test
    void loginUser_ShouldFailWithWrongCredentials() throws Exception {
        // Crear usuario en MongoDB
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRole("USER");
        userRepository.save(user);

        // Probar login con contraseña incorrecta
        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "clave_incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // Verificar que NO hay sesión activa después del login fallido
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void showRegister_ShouldShowRegisterForm_WhenNoUserLoggedIn() throws Exception {
        // When & Then
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}