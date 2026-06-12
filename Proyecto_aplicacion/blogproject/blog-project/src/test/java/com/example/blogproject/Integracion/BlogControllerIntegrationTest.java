package com.example.blogproject.Integracion;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import com.example.blogproject.repositories.BlogRepository;
import com.example.blogproject.repositories.PostRepository;
import com.example.blogproject.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogControllerIntegrationTest {

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
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Limpiar todas las colecciones
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();

        // Crear usuario normal
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        // Crear administrador
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword("admin123");
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);

        // Crear blog para testUser
        testBlog = new Blog();
        testBlog.setName("Blog de Test");
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

    // PRUEBAS DE VISUALIZACIÓN DE BLOG

    @Test
    void viewBlog_ShouldRedirectToUser_WhenBlogDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/blog/{blogId}", "nonexistent-id-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    @Test
    void viewBlog_ShouldShowPostsInCorrectOrder() throws Exception {
        // Given: Crear múltiples posts
        createMultiplePosts(testBlog.getId(), 5);

        // When & Then
        mockMvc.perform(get("/blog/{blogId}", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-view"))
                .andExpect(model().attributeExists("posts"));
    }

    // PRUEBAS DE CREACIÓN DE BLOG - FORMULARIO

    @Test
    void showCreateForm_ShouldShowForm_WhenUserHasNoBlog() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("nobloguser", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        // When & Then
        mockMvc.perform(get("/blog/create").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-create"));
    }

    @Test
    void showCreateForm_ShouldRedirectToUser_WhenUserAlreadyHasBlog() throws Exception {
        // Given: Usuario con blog
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/blog/create").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    @Test
    void showCreateForm_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then
        mockMvc.perform(get("/blog/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // PRUEBAS DE CREACIÓN DE BLOG - POST

    @Test
    void createBlog_ShouldCreateNewBlog_WhenValidData() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("newbloguser", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        int initialBlogCount = blogRepository.findAll().size();

        // When & Then
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Nuevo Blog")
                        .param("description", "Descripción del nuevo blog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created"));

        // Verificar que el blog fue creado
        int finalBlogCount = blogRepository.findAll().size();
        assertThat(finalBlogCount).isEqualTo(initialBlogCount + 1);

        Blog createdBlog = blogRepository.findByUserId(userWithoutBlog.getId());
        assertThat(createdBlog).isNotNull();
        assertThat(createdBlog.getName()).isEqualTo("Nuevo Blog");
        assertThat(createdBlog.getDescription()).isEqualTo("Descripción del nuevo blog");
    }

    @Test
    void createBlog_ShouldNotCreateDuplicateBlog_WhenUserAlreadyHasBlog() throws Exception {
        // Given: Usuario ya tiene blog
        session.setAttribute("usuario", testUser);
        int initialBlogCount = blogRepository.findAll().size();

        // When & Then
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "Otro Blog")
                        .param("description", "Intento de duplicado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        // Verificar que no se creó nuevo blog
        int finalBlogCount = blogRepository.findAll().size();
        assertThat(finalBlogCount).isEqualTo(initialBlogCount);

        // Verificar que el blog original sigue siendo el mismo
        Blog existingBlog = blogRepository.findByUserId(testUser.getId());
        assertThat(existingBlog.getName()).isEqualTo("Blog de Test");
    }

    @Test
    void createBlog_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then
        mockMvc.perform(post("/blog/create")
                        .param("name", "Mi Blog")
                        .param("description", "Descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void createBlog_ShouldHandleEmptyFields() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("emptyfieldsuser", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        // When & Then
        mockMvc.perform(post("/blog/create")
                        .session(session)
                        .param("name", "")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created"));

        // Verificar que se creó con campos vacíos
        Blog createdBlog = blogRepository.findByUserId(userWithoutBlog.getId());
        assertThat(createdBlog).isNotNull();
        assertThat(createdBlog.getName()).isEmpty();
        assertThat(createdBlog.getDescription()).isEmpty();
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
            posts.add(post);
        }
        postRepository.saveAll(posts);
    }
}
