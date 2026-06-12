package com.example.blogproject.Unitarias;

import com.example.blogproject.controllers.AuthController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.example.blogproject.models.User;
import com.example.blogproject.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class) // Habilita la integración de Mockito con JUnit 5
class AuthControllerTest {

    @Mock // Crea un mock (simulación) del UserService
    private UserService userService;

    @InjectMocks // Inyecta los mocks creados en el controlador real
    private AuthController authController;

    private MockMvc mockMvc; // Permite hacer peticiones HTTP simuladas
    private MockHttpSession session; // Simula una sesión HTTP
    private User testUser; // Usuario de prueba con rol USER
    private User adminUser; // Usuario de prueba con rol ADMIN

    @BeforeEach // Se ejecuta antes de cada prueba
    void setUp() {
        // Configura el resolutor de vistas para que pueda procesar templates HTML
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // Construye MockMvc configurado con el controlador y el resolutor de vistas
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();

        session = new MockHttpSession(); // Inicializa una sesión vacía

        // Configura un usuario normal para las pruebas
        testUser = new User();
        testUser.setId("123");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("USER");

        // Configura un usuario administrador para las pruebas
        adminUser = new User();
        adminUser.setId("456");
        adminUser.setUsername("admin");
        adminUser.setPassword("admin123");
        adminUser.setRole("ADMIN");
    }

    /**
     * Prueba que la página principal se muestre correctamente
     * cuando NO hay un usuario en sesión
     */
    @Test
    void index_ShouldReturnIndexView_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/")) // Realiza petición GET a la raíz
                .andExpect(status().isOk()) // Verifica respuesta HTTP 200 OK
                .andExpect(view().name("index")) // Verifica que retorna vista "index"
                .andExpect(model().attributeDoesNotExist("user")); // Verifica que no hay atributo "user"
    }

    /**
     * Prueba que la página principal muestre el usuario
     * cuando hay un usuario en sesión
     */
    @Test
    void index_ShouldReturnIndexViewWithUser_WhenUserInSession() throws Exception {
        session.setAttribute("usuario", testUser); // Simula usuario en sesión

        mockMvc.perform(get("/").session(session)) // Petición con sesión simulada
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("user")) // Verifica que existe atributo "user"
                .andExpect(model().attribute("user", testUser)); // Verifica que es el usuario correcto
    }

    /**
     * Prueba que se muestre el formulario de registro
     * cuando NO hay usuario en sesión
     */
    @Test
    void showRegister_ShouldReturnRegisterView_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    /**
     * Prueba que si un ADMIN intenta acceder al registro,
     * sea redirigido al panel de administración
     */
    @Test
    void showRegister_ShouldRedirectToAdmin_WhenAdminInSession() throws Exception {
        session.setAttribute("usuario", adminUser);

        mockMvc.perform(get("/register").session(session))
                .andExpect(status().is3xxRedirection()) // Verifica redirección HTTP 3xx
                .andExpect(redirectedUrl("/admin")); // Verifica URL de redirección
    }

    /**
     * Prueba que si un usuario normal intenta acceder al registro,
     * sea redirigido a su panel de usuario
     */
    @Test
    void showRegister_ShouldRedirectToUser_WhenUserInSession() throws Exception {
        session.setAttribute("usuario", testUser);

        mockMvc.perform(get("/register").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    /**
     * Prueba el registro exitoso de un nuevo usuario
     */
    @Test
    void registerUser_ShouldSaveUserAndRedirectToLogin() throws Exception {
        // Configura el mock para que retorne el usuario cuando se llame a save()
        when(userService.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register") // Petición POST para registrar
                        .param("username", "newuser")
                        .param("password", "newpass123")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success")); // Redirige a login con mensaje éxito

        // Verifica que el metodo save() fue llamado exactamente una vez
        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * Prueba el registro cuando el usuario ya existe
     */
    @Test
    void registerUser_ShouldRedirectToRegister_WhenUserAlreadyExists() throws Exception {
        // Simula que el servicio lanza excepción por usuario existente
        when(userService.save(any(User.class))).thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/auth/register")
                        .param("username", "existinguser")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register")); // Redirige de vuelta al registro

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * Prueba mostrar login cuando NO hay usuario en sesión
     */
    @Test
    void login_ShouldReturnLoginView_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    /**
     * Prueba que un ADMIN que intenta ver login sea redirigido a su panel
     */
    @Test
    void login_ShouldRedirectToAdmin_WhenAdminInSession() throws Exception {
        session.setAttribute("usuario", adminUser);

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    /**
     * Prueba que un usuario normal que intenta ver login sea redirigido a su panel
     */
    @Test
    void login_ShouldRedirectToUser_WhenUserInSession() throws Exception {
        session.setAttribute("usuario", testUser);

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    /**
     * Prueba login exitoso para usuario normal
     */
    @Test
    void loginUser_ShouldRedirectToUserPanel_WhenValidCredentials() throws Exception {
        when(userService.login("testuser", "password123")).thenReturn(testUser);

        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        verify(userService, times(1)).login("testuser", "password123");
    }

    /**
     * Prueba login exitoso para administrador
     */
    @Test
    void loginUser_ShouldRedirectToAdminPanel_WhenAdminCredentials() throws Exception {
        when(userService.login("admin", "admin123")).thenReturn(adminUser);

        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userService, times(1)).login("admin", "admin123");
    }

    /**
     * Prueba login con credenciales inválidas
     */
    @Test
    void loginUser_ShouldRedirectToLoginWithError_WhenInvalidCredentials() throws Exception {
        // Simula que el login retorna null (credenciales inválidas)
        when(userService.login("wronguser", "wrongpass")).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .param("username", "wronguser")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error")); // Redirige con parámetro error

        verify(userService, times(1)).login("wronguser", "wrongpass");
    }

    /**
     * Prueba acceso al panel de administrador con usuario ADMIN
     */
    @Test
    void admin_ShouldReturnAdminView_WhenAdminInSession() throws Exception {
        session.setAttribute("usuario", adminUser);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", adminUser));
    }

    /**
     * Prueba que sin sesión no se pueda acceder al panel admin
     */
    @Test
    void admin_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Prueba que un usuario normal NO pueda acceder al panel admin
     */
    @Test
    void admin_ShouldRedirectToLogin_WhenRegularUserInSession() throws Exception {
        session.setAttribute("usuario", testUser);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Prueba acceso a la lista de usuarios para administrador
     */
    @Test
    void adminUsers_ShouldReturnAdminUsersView_WhenAdminInSession() throws Exception {
        session.setAttribute("usuario", adminUser);
        when(userService.getAllUsers()).thenReturn(java.util.Arrays.asList(testUser, adminUser));

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("users")) // Verifica lista de usuarios
                .andExpect(model().attribute("user", adminUser));

        verify(userService, times(1)).getAllUsers();
    }

    /**
     * Prueba que sin sesión no se pueda acceder a lista de usuarios
     */
    @Test
    void adminUsers_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Prueba que usuario normal no pueda acceder a lista de usuarios
     */
    @Test
    void adminUsers_ShouldRedirectToLogin_WhenRegularUserInSession() throws Exception {
        session.setAttribute("usuario", testUser);

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /**
     * Prueba que un admin pueda eliminar un usuario
     */
    @Test
    void deleteUser_ShouldDeleteUserAndRedirectToAdmin_WhenAdminInSession() throws Exception {
        session.setAttribute("usuario", adminUser);
        doNothing().when(userService).deleteUser("123"); // Simula eliminación exitosa

        mockMvc.perform(post("/admin/delete/123").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userService, times(1)).deleteUser("123");
    }

    /**
     * Prueba que sin sesión no se pueda eliminar usuario
     */
    @Test
    void deleteUser_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        mockMvc.perform(post("/admin/delete/123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).deleteUser(anyString()); // Verifica que NO se llamó al servicio
    }

    /**
     * Prueba que un usuario normal no pueda eliminar usuarios
     */
    @Test
    void deleteUser_ShouldRedirectToLogin_WhenRegularUserInSession() throws Exception {
        session.setAttribute("usuario", testUser);

        mockMvc.perform(post("/admin/delete/123").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).deleteUser(anyString());
    }

    /**
     * Prueba que se manejen errores de base de datos durante el login
     */
    @Test
    void loginUser_ShouldHandleException() throws Exception {
        // Simula error de base de datos
        when(userService.login(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/auth/login")
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error")); // Redirige con error
    }

    /**
     * Prueba el registro con datos inválidos (usuario nulo)
     */
    @Test
    void registerUser_ShouldHandleNullUser() throws Exception {
        when(userService.save(any(User.class))).thenThrow(new IllegalArgumentException("User cannot be null"));

        mockMvc.perform(post("/auth/register")
                        .param("username", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    /**
     * Prueba el acceso a admin con sesión nula (borde)
     */
    @Test
    void admin_ShouldHandleNullSession() throws Exception {
        mockMvc.perform(get("/admin")) // Sin proporcionar sesión
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}