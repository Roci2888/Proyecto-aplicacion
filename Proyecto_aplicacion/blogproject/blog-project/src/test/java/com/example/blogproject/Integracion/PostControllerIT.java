package com.example.blogproject.Integracion;

import com.example.blogproject.FileStorageService;
import com.example.blogproject.ModerationResult;
import com.example.blogproject.SightengineService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    // Aislamos los componentes que tocan API externa y almacenamiento en disco
    @MockitoBean
    private SightengineService sightengineService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private MockHttpSession session;
    private User testUser;
    private Blog testBlog;
    private Post testPost;

    private MockMultipartFile mockFile;
    private MockMultipartFile emptyMockFile;

    @BeforeEach
    void setUp() throws Exception {
        // Configuración por defecto adaptada a la respuesta DTO y a los 2 parámetros de saveImage
        when(sightengineService.moderateAndVerifyFile(any(MultipartFile.class)))
                .thenReturn(ModerationResult.allow());

        when(fileStorageService.saveImage(any(), any()))
                .thenReturn("integration-test-image.jpg");

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

        // Inicializar sesión con el usuario autenticado
        session = new MockHttpSession();
        session.setAttribute("usuario", testUser);
        session.setAttribute("user", testUser);

        // Archivos multipart simulados
        mockFile = new MockMultipartFile(
                "imageFile",
                "test-image.jpg",
                "image/jpeg",
                "bytes-de-prueba-imagen".getBytes()
        );

        emptyMockFile = new MockMultipartFile(
                "imageFile",
                "",
                "application/octet-stream",
                new byte[0]
        );
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PRUEBAS DE MOSTRAR FORMULARIO DE POST ====================

    @Test
    void newPostForm_ShouldShowForm_WhenBlogExists() throws Exception {
        mockMvc.perform(get("/post/new")
                        .session(session)
                        .param("blogId", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attributeExists("blogId"))
                .andExpect(model().attribute("blogId", testBlog.getId()));
    }

    @Test
    void newPostForm_ShouldHandleInvalidBlogId() throws Exception {
        mockMvc.perform(get("/post/new")
                        .session(session)
                        .param("blogId", "invalid-blog-id"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "invalid-blog-id"));
    }

    // ==================== PRUEBAS DE CREACIÓN DE POST ====================

    @Test
    void createPost_ShouldCreatePostWithoutOptionalFields() throws Exception {
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Post sin imagen";
        String newContent = "Contenido sin imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount + 1);

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
        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

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
        String[] posts = {"Post 1", "Post 2", "Post 3"};

        for (String postTitle : posts) {
            mockMvc.perform(multipart("/post/create")
                            .file(emptyMockFile)
                            .session(session)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .param("blogId", testBlog.getId())
                            .param("title", postTitle)
                            .param("content", "Contenido de " + postTitle))
                    .andExpect(status().is3xxRedirection());
        }

        List<Post> blogPosts = postRepository.findByBlogId(testBlog.getId());
        assertThat(blogPosts).hasSize(posts.length + 1);

        for (String postTitle : posts) {
            boolean exists = blogPosts.stream()
                    .anyMatch(p -> p.getTitle().equals(postTitle));
            assertThat(exists).isTrue();
        }
    }

    // ==================== PRUEBAS DE ELIMINACIÓN DE POST ====================

    @Test
    void deletePost_ShouldDeleteExistingPost() throws Exception {
        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(post("/post/delete")
                        .session(session)
                        .param("postId", testPost.getId())
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount - 1);

        Optional<Post> deletedPost = postRepository.findById(testPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void deletePost_ShouldHandleNonExistentPost() throws Exception {
        String nonExistentId = "nonexistent-id-123";
        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(post("/post/delete")
                        .session(session)
                        .param("postId", nonExistentId)
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount);
    }

    @Test
    void deletePost_ShouldDeleteOnlySpecifiedPost() throws Exception {
        Post anotherPost = new Post();
        anotherPost.setBlogId(testBlog.getId());
        anotherPost.setTitle("Otro Post");
        anotherPost.setContent("Contenido de otro post");
        anotherPost.setCreatedAt(LocalDateTime.now());
        anotherPost = postRepository.save(anotherPost);

        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(post("/post/delete")
                        .session(session)
                        .param("postId", testPost.getId())
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount - 1);

        Optional<Post> remainingPost = postRepository.findById(anotherPost.getId());
        assertThat(remainingPost).isPresent();
        assertThat(remainingPost.get().getTitle()).isEqualTo("Otro Post");

        Optional<Post> deletedPost = postRepository.findById(testPost.getId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void deletePost_ShouldHandleEmptyPostId() throws Exception {
        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(post("/post/delete")
                        .session(session)
                        .param("postId", "")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount);
    }

    // ==================== PRUEBAS DE FLUJO COMPLETO Y EXCEPCIONES ====================

    @Test
    void completeFlow_CreateAndDeletePost() throws Exception {
        String newTitle = "Post de Flujo Completo";
        String newContent = "Contenido del post de flujo completo";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(newTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        String createdPostId = createdPost.getId();

        mockMvc.perform(post("/post/delete")
                        .session(session)
                        .param("postId", createdPostId)
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        Optional<Post> deletedPost = postRepository.findById(createdPostId);
        assertThat(deletedPost).isEmpty();
    }

    @Test
    void createPost_ShouldPreserveTimestamp() throws Exception {
        LocalDateTime beforeCreation = LocalDateTime.now();

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", "Post con timestamp")
                        .param("content", "Verificar timestamp"))
                .andExpect(status().is3xxRedirection());

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
        String specialTitle = "¡Post con caracteres especiales! @#$%^&*()";
        String specialContent = "Contenido con <script>alert('XSS')</script> y emojis 🎉🚀";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", specialTitle)
                        .param("content", specialContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(specialTitle))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEqualTo(specialTitle);
        assertThat(createdPost.getContent()).isEqualTo(specialContent);
    }

    @Test
    void createPost_ShouldRejectPost_WhenImageContainsInappropriateContent() throws Exception {
        // Given: Se simula rechazo de la imagen
        when(sightengineService.moderateAndVerifyFile(any(MultipartFile.class)))
                .thenReturn(ModerationResult.reject("NUDITY_SEXUAL_ACTIVITY", "Contenido inapropiado", "Sexual activity detected"));

        String title = "Post con Imagen Inapropiada";
        String content = "Contenido del post rechazado";

        // When
        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content))
                // Then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=" + testBlog.getId()))
                .andExpect(flash().attributeExists("error"));

        // Verificación en BD
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        boolean postExiste = posts.stream().anyMatch(p -> p.getTitle().equals(title));
        assertThat(postExiste).isFalse();
    }

    @Test
    void createPost_ShouldHandleException_WhenImageUploadFails() throws Exception {
        // Given: Pasa moderación pero falla al guardar con 2 parámetros
        when(sightengineService.moderateAndVerifyFile(any(MultipartFile.class)))
                .thenReturn(ModerationResult.allow());

        when(fileStorageService.saveImage(any(), any()))
                .thenThrow(new RuntimeException("Error simulado de escritura en disco"));

        String title = "Post con Fallo de Almacenamiento";
        String content = "Contenido del post con error de disco";

        // When
        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .session(session)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content))
                // Then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=" + testBlog.getId()))
                .andExpect(flash().attributeExists("error"));

        // Verificación en BD
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        boolean postExiste = posts.stream().anyMatch(p -> p.getTitle().equals(title));
        assertThat(postExiste).isFalse();
    }
}



