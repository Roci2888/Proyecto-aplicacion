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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIT {

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
    private Post testPost;

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

        // Crear blog de prueba
        testBlog = new Blog();
        testBlog.setName("Blog de Prueba");
        testBlog.setDescription("Descripción del blog");
        testBlog.setUserId(testUser.getId());
        testBlog = blogRepository.save(testBlog);

        // Crear post de prueba
        testPost = new Post();
        testPost.setBlogId(testBlog.getId());
        testPost.setTitle("Post Original");
        testPost.setContent("Contenido original del post");
        testPost.setCreatedAt(LocalDateTime.now());
        testPost = postRepository.save(testPost);

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

    // PRUEBAS DE MOSTRAR FORMULARIO DE POST

    @Test
    void newPostForm_ShouldShowForm_WhenBlogExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/post/new")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attributeExists("blogId"))
                .andExpect(model().attribute("blogId", testBlog.getId()));
    }

    @Test
    void newPostForm_ShouldHandleInvalidBlogId() throws Exception {
        // When & Then
        mockMvc.perform(get("/post/new")
                        .param("blogId", "invalid-blog-id"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "invalid-blog-id"));
    }

    // PRUEBAS DE CREACIÓN DE POST

    @Test
    void createPost_ShouldCreateNewPost_WhenValidData() throws Exception {
        // Given
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Nuevo Post";
        String newContent = "Contenido del nuevo post";
        String imageUrl = "https://example.com/image.jpg";
        String imageCaption = "Caption de la imagen";

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent)
                        .param("imageUrl", imageUrl)
                        .param("imageCaption", imageCaption))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount + 1);

        // Verificar contenido del post
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(newTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEqualTo(newTitle);
        assertThat(createdPost.getContent()).isEqualTo(newContent);
        assertThat(createdPost.getImageUrl()).isEqualTo(imageUrl);
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }

    @Test
    void createPost_ShouldCreatePostWithoutOptionalFields() throws Exception {
        // Given
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Post sin imagen";
        String newContent = "Contenido sin imagen";

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount + 1);

        // Verificar que los campos opcionales son null
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(newTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getImageUrl()).isNull();
        assertThat(createdPost.getImageCaption()).isNull();
    }

    @Test
    void createPost_ShouldHandleEmptyTitleAndContent() throws Exception {
        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().isEmpty())
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEmpty();
        assertThat(createdPost.getContent()).isEmpty();
    }

    @Test
    void createPost_ShouldCreateMultiplePostsForSameBlog() throws Exception {
        // Given
        String[] posts = {"Post 1", "Post 2", "Post 3"};

        // When
        for (String postTitle : posts) {
            mockMvc.perform(post("/post/create")
                            .param("blogId", testBlog.getId())
                            .param("title", postTitle)
                            .param("content", "Contenido de " + postTitle))
                    .andExpect(status().is3xxRedirection());
        }

        // Then
        List<Post> blogPosts = postRepository.findByBlogId(testBlog.getId());
        assertThat(blogPosts).hasSize(posts.length + 1); // +1 por el post original del setUp

        for (String postTitle : posts) {
            boolean exists = blogPosts.stream()
                    .anyMatch(p -> p.getTitle().equals(postTitle));
            assertThat(exists).isTrue();
        }
    }

    // PRUEBAS DE ELIMINACIÓN DE POST

    @Test
    void deletePost_ShouldDeleteExistingPost() throws Exception {
        // Given
        int initialPostCount = postRepository.findAll().size();

        // When
        mockMvc.perform(post("/post/delete")
                        .param("postId", testPost.getId())
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount - 1);

        Optional<Post> deletedPost = postRepository.findById(testPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void deletePost_ShouldHandleNonExistentPost() throws Exception {
        // Given
        String nonExistentId = "nonexistent-id-123";
        int initialPostCount = postRepository.findAll().size();

        // When
        mockMvc.perform(post("/post/delete")
                        .param("postId", nonExistentId)
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount); // El count no cambia
    }

    @Test
    void deletePost_ShouldDeleteOnlySpecifiedPost() throws Exception {
        // Given
        Post anotherPost = new Post();
        anotherPost.setBlogId(testBlog.getId());
        anotherPost.setTitle("Otro Post");
        anotherPost.setContent("Contenido de otro post");
        anotherPost.setCreatedAt(LocalDateTime.now());
        anotherPost = postRepository.save(anotherPost);

        int initialPostCount = postRepository.findAll().size();

        // When
        mockMvc.perform(post("/post/delete")
                        .param("postId", testPost.getId())
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount - 1);

        // Verificar que el otro post aún existe
        Optional<Post> remainingPost = postRepository.findById(anotherPost.getId());
        assertThat(remainingPost).isPresent();
        assertThat(remainingPost.get().getTitle()).isEqualTo("Otro Post");

        // Verificar que el post eliminado ya no existe
        Optional<Post> deletedPost = postRepository.findById(testPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void deletePost_ShouldHandleEmptyPostId() throws Exception {
        // Given
        int initialPostCount = postRepository.findAll().size();

        // When
        mockMvc.perform(post("/post/delete")
                        .param("postId", "")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Then
        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount); // No se eliminó nada
    }

    // PRUEBAS DE FLUJO COMPLETO

    @Test
    void completeFlow_CreateAndDeletePost() throws Exception {
        // 1. Crear un nuevo post
        String newTitle = "Post de Flujo Completo";
        String newContent = "Contenido del post de flujo completo";

        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // 2. Verificar que el post fue creado
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(newTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        String createdPostId = createdPost.getId();

        // 3. Eliminar el post
        mockMvc.perform(post("/post/delete")
                        .param("postId", createdPostId)
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // 4. Verificar que el post fue eliminado
        Optional<Post> deletedPost = postRepository.findById(createdPostId);
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void createPost_ShouldPreserveTimestamp() throws Exception {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", "Post con timestamp")
                        .param("content", "Verificar timestamp"))
                .andExpect(status().is3xxRedirection());

        // Then
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals("Post con timestamp"))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getCreatedAt()).isNotNull();
        assertThat(createdPost.getCreatedAt()).isAfterOrEqualTo(beforeCreation);
    }

    @Test
    void createPost_ShouldHandleSpecialCharacters() throws Exception {
        // Given
        String specialTitle = "¡Post con caracteres especiales! @#$%^&*()";
        String specialContent = "Contenido con <script>alert('XSS')</script> y emojis 🎉🚀";

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", specialTitle)
                        .param("content", specialContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(specialTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEqualTo(specialTitle);
        assertThat(createdPost.getContent()).isEqualTo(specialContent);
    }

    // PRUEBA DE POST CON TODOS LOS CAMPOS

    @Test
    void createPost_ShouldCreatePostWithAllFields() throws Exception {
        // Given
        String title = "Post Completo";
        String content = "Contenido completo del post";
        String imageUrl = "https://example.com/full-image.jpg";
        String imageCaption = "Descripción completa de la imagen";

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content)
                        .param("imageUrl", imageUrl)
                        .param("imageCaption", imageCaption))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(title))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEqualTo(title);
        assertThat(createdPost.getContent()).isEqualTo(content);
        assertThat(createdPost.getImageUrl()).isEqualTo(imageUrl);
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }
}