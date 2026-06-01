package com.example.blogproject.controllers;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import com.example.blogproject.services.BlogService;
import com.example.blogproject.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class) // Habilita la integración de Mockito con JUnit 5
class BlogControllerTest {

    @Mock // Crea un mock del servicio de blogs
    private BlogService blogService;

    @Mock // Crea un mock del servicio de posts
    private PostService postService;

    @InjectMocks // Inyecta los mocks en el controlador real
    private BlogController blogController;

    private MockMvc mockMvc; // Permite hacer peticiones HTTP simuladas
    private MockHttpSession session; // Simula una sesión HTTP
    private User testUser; // Usuario de prueba
    private Blog testBlog; // Blog de prueba
    private List<Post> testPosts; // Lista de posts de prueba

    @BeforeEach // Se ejecuta antes de cada prueba
    void setUp() {
        // Configura el resolutor de vistas para procesar templates HTML
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // Construye MockMvc con el controlador y el resolutor de vistas
        mockMvc = MockMvcBuilders.standaloneSetup(blogController)
                .setViewResolvers(viewResolver)
                .build();

        session = new MockHttpSession(); // Inicializa una sesión vacía

        // Configura un usuario de prueba
        testUser = new User();
        testUser.setId("user123");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("USER");

        // Configura un blog de prueba
        testBlog = new Blog();
        testBlog.setId("blog123");
        testBlog.setUserId("user123");
        testBlog.setName("Mi Blog de Prueba");
        testBlog.setDescription("Descripción del blog de prueba");

        // Configura posts de prueba
        Post post1 = new Post();
        post1.setId("post1");
        post1.setBlogId("blog123");
        post1.setTitle("Primer Post");
        post1.setContent("Contenido del primer post");
        post1.setCreatedAt(LocalDateTime.now());

        testPosts = Arrays.asList(post1);
    }

    // ==================== TESTS PARA viewBlog() ====================
    // Pruebas para visualizar un blog específico

    /**
     * Prueba que se muestre correctamente un blog existente con sus posts
     */
    @Test
    void viewBlog_ShouldReturnBlogView_WhenBlogExists() throws Exception {
        // Given: Configuración de los mocks
        when(blogService.findById("blog123")).thenReturn(testBlog);
        when(postService.findByBlogId("blog123")).thenReturn(testPosts);

        // When & Then: Ejecuta la petición y verifica resultados
        mockMvc.perform(get("/blog/blog123"))
                .andExpect(status().isOk()) // Verifica HTTP 200 OK
                .andExpect(view().name("blog-view")) // Verifica vista correcta
                .andExpect(model().attributeExists("blog")) // Verifica que existe atributo blog
                .andExpect(model().attributeExists("posts")); // Verifica que existe atributo posts

        // Verifica que los servicios fueron llamados correctamente
        verify(blogService, times(1)).findById("blog123");
        verify(postService, times(1)).findByBlogId("blog123");
    }

    /**
     * Prueba que se redirija al panel de usuario cuando el blog no existe
     */
    @Test
    void viewBlog_ShouldRedirectToUser_WhenBlogDoesNotExist() throws Exception {
        // Given: Simula que no se encuentra el blog
        when(blogService.findById("nonexistent")).thenReturn(null);

        // When & Then: Verifica redirección
        mockMvc.perform(get("/blog/nonexistent"))
                .andExpect(status().is3xxRedirection()) // Verifica redirección HTTP 3xx
                .andExpect(redirectedUrl("/user")); // Verifica URL de redirección

        verify(blogService, times(1)).findById("nonexistent");
        verify(postService, never()).findByBlogId(anyString()); // No debería buscar posts
    }

    /**
     * Prueba que un blog sin posts se muestre correctamente
     */
    @Test
    void viewBlog_ShouldHandleBlogWithNoPosts() throws Exception {
        // Given: Blog existe pero no tiene posts
        when(blogService.findById("blog123")).thenReturn(testBlog);
        when(postService.findByBlogId("blog123")).thenReturn(Collections.emptyList());

        // When & Then: Verifica que la lista de posts esté vacía
        mockMvc.perform(get("/blog/blog123"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-view"))
                .andExpect(model().attribute("posts", Collections.emptyList()));
    }

    // ==================== TESTS PARA showCreateForm() ====================
    // Pruebas para mostrar el formulario de creación de blog

    /**
     * Prueba que se muestre el formulario de creación cuando el usuario no tiene blog
     */
    @Test
    void showCreateForm_ShouldReturnCreateForm_WhenNoBlogExists() throws Exception {
        // Given: Usuario en sesión sin blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // When & Then: Verifica que muestra el formulario
        mockMvc.perform(get("/blog/create").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-create"));

        verify(blogService, times(1)).findByUserId("user123");
    }

    /**
     * Prueba que se redirija al panel de usuario si ya tiene un blog
     * (Un usuario solo puede tener un blog)
     */
    @Test
    void showCreateForm_ShouldRedirectToUser_WhenUserAlreadyHasBlog() throws Exception {
        // Given: Usuario en sesión que YA tiene blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then: Verifica redirección al panel de usuario
        mockMvc.perform(get("/blog/create").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    /**
     * Prueba que se redirija al login si no hay usuario en sesión
     * (Protección de ruta)
     */
    @Test
    void showCreateForm_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then: Sin sesión, debería redirigir a login
        mockMvc.perform(get("/blog/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(blogService, never()).findByUserId(anyString());
    }

    // ==================== TESTS PARA createBlog() ====================
    // Pruebas para la creación de blogs (metodo POST)

    /**
     * Prueba la creación exitosa de un blog
     * Usa doAnswer para simular la asignación de ID por el servicio
     */
    @Test
    void createBlog_ShouldCreateBlogAndRedirect_WhenValidRequest() throws Exception {
        // Given: Usuario sin blog, datos válidos
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // ✅ Para método void, usamos doAnswer para simular comportamiento
        doAnswer(invocation -> {
            Blog blog = invocation.getArgument(0); // Obtiene el blog pasado al método
            blog.setId("generatedId"); // Simula que el servicio asigna un ID
            return null;
        }).when(blogService).save(any(Blog.class));

        // When & Then: Ejecuta la creación
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Mi Nuevo Blog")
                        .param("description", "Descripción del nuevo blog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created")); // Redirige con mensaje éxito

        // Verifica las llamadas a los servicios
        verify(blogService, times(1)).findByUserId("user123");
        verify(blogService, times(1)).save(any(Blog.class));
    }

    /**
     * Prueba que no se permita crear un segundo blog para el mismo usuario
     */
    @Test
    void createBlog_ShouldNotCreateDuplicateBlog_WhenUserAlreadyHasBlog() throws Exception {
        // Given: Usuario que YA tiene blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then: Intenta crear otro blog
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Otro Blog")
                        .param("description", "Intento de crear otro blog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        // Verifica que NO se intentó guardar el nuevo blog
        verify(blogService, times(1)).findByUserId("user123");
        verify(blogService, never()).save(any(Blog.class));
    }

    /**
     * Prueba que se requiera autenticación para crear blog
     */
    @Test
    void createBlog_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then: Sin sesión, debería redirigir a login
        mockMvc.perform(post("/blog/create")
                        .param("name", "Mi Blog")
                        .param("description", "Descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Verifica que no se llamó a ningún servicio
        verify(blogService, never()).findByUserId(anyString());
        verify(blogService, never()).save(any(Blog.class));
    }

    /**
     * Prueba que se manejen correctamente campos vacíos en el formulario
     */
    @Test
    void createBlog_ShouldHandleEmptyFields() throws Exception {
        // Given: Usuario sin blog, pero campos vacíos
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        doNothing().when(blogService).save(any(Blog.class)); // Simula guardado exitoso

        // When & Then: Aún con campos vacíos, debería crear el blog
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created"));

        verify(blogService, times(1)).save(any(Blog.class));
    }

    /**
     * Prueba el manejo de excepciones durante el guardado del blog
     */
    @Test
    void createBlog_ShouldHandleSaveException() throws Exception {
        // Given: Usuario sin blog, pero el servicio lanza excepción al guardar
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // ✅ Para lanzar excepción en método void, usamos doThrow
        doThrow(new RuntimeException("Error saving blog")).when(blogService).save(any(Blog.class));

        // When & Then: Debe manejar la excepción y redirigir
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Mi Blog")
                        .param("description", "Descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        verify(blogService, times(1)).save(any(Blog.class));
    }

    /**
     * Prueba que las propiedades del blog se establezcan correctamente
     * Usa ArgumentCaptor para capturar y verificar el objeto creado
     */
    @Test
    void createBlog_ShouldSetCorrectBlogProperties() throws Exception {
        // Given: Usuario sin blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // Capturar el blog que se pasa al método save
        ArgumentCaptor<Blog> blogCaptor = ArgumentCaptor.forClass(Blog.class);
        doNothing().when(blogService).save(blogCaptor.capture());

        // When: Ejecuta la creación con datos específicos
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Blog con Propiedades")
                        .param("description", "Verificando propiedades"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created"));

        // Then: Verifica que se llamó al servicio save
        verify(blogService, times(1)).save(any(Blog.class));

        // Verifica que las propiedades del blog capturado son correctas
        Blog capturedBlog = blogCaptor.getValue();
        assertNotNull(capturedBlog); // El blog no debería ser nulo
        assertEquals("user123", capturedBlog.getUserId()); // Debe tener el userId correcto
        assertEquals("Blog con Propiedades", capturedBlog.getName()); // Nombre correcto
        assertEquals("Verificando propiedades", capturedBlog.getDescription()); // Descripción correcta
    }
}
