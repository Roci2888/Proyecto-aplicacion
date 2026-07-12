package com.example.blogproject.Integracion;

import com.example.blogproject.domain.model.Blog;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.domain.model.User;
import com.example.blogproject.infrastructure.persistence.BlogRepository;
import com.example.blogproject.infrastructure.persistence.PostRepository;
import com.example.blogproject.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private PostRepository postRepository;

    private MockHttpSession session;
    private User testUser;
    private Blog testBlog;

    @BeforeEach
    void setUp() {
        // Limpiar todas las colecciones
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();

        // Crear usuario de prueba
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        // Crear blog para testUser
        testBlog = new Blog();
        testBlog.setName("Blog de Prueba");
        testBlog.setDescription("Descripción del blog de prueba");
        testBlog.setUserId(testUser.getId());
        testBlog = blogRepository.save(testBlog);

        // Inicializar sesión
        session = new MockHttpSession();
        session.setAttribute("usuario", testUser);
    }

    @AfterEach
    void tearDown() {
        // Limpiar después de cada prueba
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();
    }

    // PRUEBAS DEL PANEL DE USUARIO

    @Test
    void userPanel_ShouldShowPosts_WhenViewParamIsPosts() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 3);
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void userPanel_ShouldHandlePagination_WhenViewingPosts() throws Exception {
        // Given: Crear 12 posts para probar paginación (3 páginas de 5)
        createMultiplePosts(testBlog.getId(), 12);
        session.setAttribute("usuario", testUser);

        // When & Then: Primera página
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attributeExists("posts"));

        // When & Then: Segunda página
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1));
    }

    @Test
    void userPanel_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void userPanel_ShouldNotShowPosts_WhenUserHasNoBlog() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("nobloguser3", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts"))
                .andExpect(model().attributeDoesNotExist("currentPage"))
                .andExpect(model().attributeDoesNotExist("totalPages"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "profile", "settings", "dashboard", "stats" })
    void userPanel_ShouldNotShowPosts_WhenViewIsNotPosts(String viewValue) throws Exception {
        // Given
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("view", viewValue))
                .andExpect(model().attributeDoesNotExist("posts"));
    }

    @Test
    void userPanel_ShouldUseDefaultPage_WhenPageParamNotProvided() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 5);
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void userPanel_ShouldHandleEmptyPosts_WhenBlogHasNoPosts() throws Exception {
        // Given: Blog sin posts adicionales
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0));
    }

    // PRUEBAS ADICIONALES RECOMENDADAS

    @Test
    void userPanel_ShouldHandleOutOfRangePage() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 10); // 2 páginas (5 por página)
        session.setAttribute("usuario", testUser);

        // When & Then: Página 5 (más allá del límite)
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"));
    }

    // MÉTODOS AUXILIARES

    private User createAndSaveUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return userRepository.save(user);
    }

    private void createMultiplePosts(String blogId, int count) {
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Post post = new Post();
            post.setTitle("Post " + i);
            post.setContent("Contenido del post " + i);
            post.setBlogId(blogId);
            post.setId(testUser.getId());
            post.setCreatedAt(LocalDateTime.now().minusDays(count - i)); // Posts más recientes primero
            posts.add(post);
        }
        postRepository.saveAll(posts);
    }

    private void printDatabaseState() {
        System.out.println("=== ESTADO DE LA BASE DE DATOS ===");
        System.out.println("Usuarios: " + userRepository.count());
        System.out.println("Blogs: " + blogRepository.count());
        System.out.println("Posts: " + postRepository.count());
        System.out.println("===================================");
    }
}
