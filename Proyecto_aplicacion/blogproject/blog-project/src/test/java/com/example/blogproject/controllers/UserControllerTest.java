package com.example.blogproject.controllers;

import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import com.example.blogproject.services.BlogService;
import com.example.blogproject.services.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private MockHttpSession session;

    @Mock
    private BlogService blogService;

    @Mock
    private PostService postService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private Blog testBlog;
    private List<Post> testPosts;
    private Page<Post> testPostPage;

    // Clase concreta para pruebas si User es abstracto
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
        // Configurar el resolutor de vistas
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // Configurar MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setViewResolvers(viewResolver)
                .build();

        // Inicializar sesión simulada
        session = new MockHttpSession();

        // Configurar usuario de prueba
        testUser = new TestUser();
        testUser.setId("user123");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("USER");

        // Configurar blog de prueba
        testBlog = new Blog();
        testBlog.setId("blog123");
        testBlog.setUserId("user123");
        testBlog.setName("Mi Blog");
        testBlog.setDescription("Descripción del blog");

        // Configurar posts de prueba
        Post post1 = new Post();
        post1.setId("post1");
        post1.setBlogId("blog123");
        post1.setTitle("Post 1");
        post1.setContent("Contenido 1");
        post1.setCreatedAt(LocalDateTime.now());

        Post post2 = new Post();
        post2.setId("post2");
        post2.setBlogId("blog123");
        post2.setTitle("Post 2");
        post2.setContent("Contenido 2");
        post2.setCreatedAt(LocalDateTime.now());

        testPosts = Arrays.asList(post1, post2);

        // Configurar página de posts
        testPostPage = new PageImpl<>(testPosts, PageRequest.of(0, 5), 2);
    }

    // ==================== PRUEBAS DE SEGURIDAD Y AUTENTICACIÓN ====================

    /**
     * Prueba que redirige al login cuando NO hay usuario en sesión
     */
    @Test
    void userPanel_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then: Sin sesión, debería redirigir a login
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Verifica que no se llamó a ningún servicio
        verify(blogService, never()).findByUserId(anyString());
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    /**
     * Prueba que redirige al login cuando la sesión es nula
     */
    @Test
    void userPanel_ShouldRedirectToLogin_WhenSessionIsNull() throws Exception {
        // When & Then: Sin proporcionar sesión
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ==================== PRUEBAS DE VISTA PRINCIPAL DEL PANEL ====================

    /**
     * Prueba que muestra el panel de usuario correctamente (vista por defecto)
     * Sin parámetro 'view', debe mostrar la vista principal
     */
    @Test
    void userPanel_ShouldShowUserPanel_WhenUserHasBlog() throws Exception {
        // Given: Usuario en sesión con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("view"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("blog", testBlog))
                .andExpect(model().attribute("view", null));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    /**
     * Prueba que muestra el panel de usuario cuando NO tiene blog
     */
    @Test
    void userPanel_ShouldShowUserPanel_WhenUserHasNoBlog() throws Exception {
        // Given: Usuario en sesión sin blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attribute("blog", null));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    // ==================== PRUEBAS DE VISUALIZACIÓN DE POSTS CON PAGINACIÓN ====================

    /**
     * Prueba que muestra los posts del usuario con paginación
     * Cuando view="posts" y el usuario tiene blog
     */
    @Test
    void userPanel_ShouldShowPosts_WhenViewIsPostsAndUserHasBlog() throws Exception {
        // Given: Usuario con blog, solicitando ver posts
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user", "blog", "view"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("blog", testBlog))
                .andExpect(model().attribute("view", "posts"))
                .andExpect(model().attribute("posts", testPosts))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, times(1)).getPostsByBlogPaged("blog123", 0, 5);
    }

    /**
     * Prueba que muestra posts con página específica
     */
    @Test
    void userPanel_ShouldShowPosts_WhenSpecificPageRequested() throws Exception {
        // Given: Usuario con blog, solicitando página 2
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> page2Posts = new PageImpl<>(Collections.singletonList(testPosts.get(0)),
                PageRequest.of(1, 5), 3);
        when(postService.getPostsByBlogPaged("blog123", 1, 5)).thenReturn(page2Posts);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 1));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 1, 5);
    }

    /**
     * Prueba que NO muestra posts cuando view="posts" pero el usuario NO tiene blog
     */
    @Test
    void userPanel_ShouldNotShowPosts_WhenViewIsPostsButUserHasNoBlog() throws Exception {
        // Given: Usuario sin blog, solicitando ver posts
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user", "blog", "view"))
                .andExpect(model().attributeDoesNotExist("posts"))
                .andExpect(model().attributeDoesNotExist("currentPage"))
                .andExpect(model().attributeDoesNotExist("totalPages"));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    /**
     * Prueba que maneja páginas vacías correctamente
     */
    @Test
    void userPanel_ShouldHandleEmptyPosts_WhenBlogHasNoPosts() throws Exception {
        // Given: Usuario con blog pero sin posts
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0));
    }

    // ==================== PRUEBAS CON DIFERENTES VALORES DE VIEW ====================

    /**
     * Prueba con diferentes valores del parámetro 'view'
     * Usando ParameterizedTest para probar múltiples valores
     */
    @ParameterizedTest
    @ValueSource(strings = {"profile", "settings", "dashboard", "stats", ""})
    void userPanel_ShouldNotShowPosts_WhenViewIsNotPosts(String viewValue) throws Exception {
        // Given: Usuario con blog, pero view no es "posts"
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("view", viewValue))
                .andExpect(model().attributeDoesNotExist("posts"));

        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    /**
     * Prueba con view nulo o vacío
     */
    @ParameterizedTest
    @NullAndEmptySource
    void userPanel_ShouldNotShowPosts_WhenViewIsNullOrEmpty(String viewValue) throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts"));

        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    // ==================== PRUEBAS DE PAGINACIÓN CON DIFERENTES PÁGINAS ====================

    /**
     * Prueba paginación con diferentes números de página
     */
    @ParameterizedTest
    @CsvSource({
            "0, 0",   // Primera página
            "1, 1",   // Segunda página
            "2, 2",   // Tercera página
            "5, 5"    // Página lejana
    })
    void userPanel_ShouldHandleDifferentPageNumbers(int pageNumber, int expectedPage) throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> pageResult = new PageImpl<>(testPosts, PageRequest.of(pageNumber, 5), 10);
        when(postService.getPostsByBlogPaged("blog123", pageNumber, 5)).thenReturn(pageResult);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", String.valueOf(pageNumber)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", expectedPage));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", pageNumber, 5);
    }

    /**
     * Prueba que usa página por defecto (0) cuando no se proporciona
     */
    @Test
    void userPanel_ShouldUseDefaultPage_WhenPageParamNotProvided() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        // When & Then: Sin parámetro page
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 0, 5);
    }

    /**
     * Prueba que maneja páginas negativas (debería usar el valor por defecto)
     */
    @Test
    void userPanel_ShouldHandleNegativePage() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // El defaultValue="0" en @RequestParam maneja valores no numéricos
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        // When & Then: Página negativa
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "-1"))
                .andExpect(status().isOk());

        // Spring convierte -1 a -1, pero el servicio podría manejarlo
        verify(postService, times(1)).getPostsByBlogPaged("blog123", -1, 5);
    }

    // ==================== PRUEBAS DE BLOG NULO Y CASOS BORDE ====================

    /**
     * Prueba cuando el blog es nulo pero view="posts"
     * No debe intentar cargar posts
     */
    @Test
    void userPanel_ShouldNotLoadPosts_WhenBlogIsNullAndViewIsPosts() throws Exception {
        // Given: Usuario en sesión sin blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeDoesNotExist("posts"))
                .andExpect(model().attributeDoesNotExist("currentPage"))
                .andExpect(model().attributeDoesNotExist("totalPages"));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    /**
     * Prueba con diferentes roles de usuario
     */
    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "MODERATOR", "GUEST"})
    void userPanel_ShouldWorkForDifferentRoles(String role) throws Exception {
        // Given: Usuario con diferentes roles
        testUser.setRole(role);
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("user", testUser));

        verify(blogService, times(1)).findByUserId("user123");
    }

    // ==================== PRUEBAS CON VALORES EXTREMOS ====================

    /**
     * Prueba con página muy grande (más allá del total disponible)
     */
    @Test
    void userPanel_ShouldHandleVeryLargePageNumber() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(100, 5), 10);
        when(postService.getPostsByBlogPaged("blog123", 100, 5)).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 100))
                .andExpect(model().attribute("posts", Collections.emptyList()));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 100, 5);
    }

    /**
     * Prueba con texto no numérico en el parámetro page
     */
    @Test
    void userPanel_ShouldHandleNonNumericPage() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // defaultValue="0" convierte texto no numérico a 0
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        // When & Then: Page es texto no numérico
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "abc"))
                .andExpect(status().isOk());

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 0, 5);
    }

    // ==================== PRUEBAS DE ATRIBUTOS DEL MODELO ====================

    /**
     * Prueba que todos los atributos del modelo estén presentes
     */
    @Test
    void userPanel_ShouldHaveAllRequiredModelAttributes() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("view"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("blog", testBlog));
    }

    /**
     * Prueba que los atributos de paginación solo existen cuando view="posts"
     */
    @Test
    void userPanel_ShouldOnlyHavePaginationAttributes_WhenViewIsPosts() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        // When & Then: Con view=posts
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts", "currentPage", "totalPages"));

        // When & Then: Sin view=posts
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts", "currentPage", "totalPages"));
    }
}