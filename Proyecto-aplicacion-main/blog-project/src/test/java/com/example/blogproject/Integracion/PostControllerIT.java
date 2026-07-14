package com.example.blogproject.Integracion;

import com.example.blogproject.domain.model.Blog;
import com.example.blogproject.domain.model.ModerationResult;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.domain.model.User;
import com.example.blogproject.infrastructure.moderation.SightengineService;
import com.example.blogproject.infrastructure.persistence.BlogRepository;
import com.example.blogproject.infrastructure.persistence.PostRepository;
import com.example.blogproject.infrastructure.persistence.UserRepository;
import com.example.blogproject.infrastructure.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @MockitoBean
    private SightengineService sightengineService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private MockHttpSession session;
    private User testUser;
    private Blog testBlog;
    private Post testPost;

    private static final String DEFAULT_STORED_FILENAME = "stored-image.jpg";

    @BeforeEach
    void setUp() throws Exception {
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

        // Mock por defecto: toda imagen es aprobada por Sightengine
        ModerationResult approved = mock(ModerationResult.class);
        when(approved.isAllowed()).thenReturn(true);
        when(approved.getReasonCode()).thenReturn("OK");
        when(approved.getUserMessage()).thenReturn("Aprobada");
        when(approved.getDebugMessage()).thenReturn("debug-ok");
        // Mock por defecto: toda imagen es aprobada por Sightengine
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());

// Mock por defecto: guardar imagen devuelve un nombre fijo
        when(fileStorageService.saveImage(any())).thenReturn(DEFAULT_STORED_FILENAME);
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Helper: crea un archivo multipart vacío (simula "sin imagen")
    private MockMultipartFile emptyImagePart() {
        return new MockMultipartFile("imageFile", "", "image/jpeg", new byte[0]);
    }

    // Helper: crea un archivo multipart con contenido (simula "con imagen")
    private MockMultipartFile fakeImagePart() {
        return new MockMultipartFile("imageFile", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());
    }

    // PRUEBAS DE MOSTRAR FORMULARIO DE POST

    @Test
    void newPostForm_ShouldShowForm_WhenBlogExists() throws Exception {
        mockMvc.perform(get("/post/new")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attributeExists("blogId"))
                .andExpect(model().attribute("blogId", testBlog.getId()));
    }

    @Test
    void newPostForm_ShouldHandleInvalidBlogId() throws Exception {
        mockMvc.perform(get("/post/new")
                        .param("blogId", "invalid-blog-id"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "invalid-blog-id"));
    }

    // PRUEBAS DE CREACIÓN DE POST

    @Test
    void createPost_ShouldCreateNewPost_WhenValidData() throws Exception {
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Nuevo Post";
        String newContent = "Contenido del nuevo post";
        String imageCaption = "Caption de la imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", testBlog.getId())
                        .param("title", newTitle)
                        .param("content", newContent)
                        .param("imageCaption", imageCaption))
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
        assertThat(createdPost.getTitle()).isEqualTo(newTitle);
        assertThat(createdPost.getContent()).isEqualTo(newContent);
        assertThat(createdPost.getImageUrl()).isEqualTo(DEFAULT_STORED_FILENAME);
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();

        verify(sightengineService).moderateAndVerifyFile(any());
        verify(fileStorageService).saveImage(any());
    }

    @Test
    void createPost_ShouldCreatePostWithoutOptionalFields() throws Exception {
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Post sin imagen";
        String newContent = "Contenido sin imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyImagePart())
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
        // Con archivo vacío, el controller no entra al bloque de moderación/guardado
        assertThat(createdPost.getImageUrl()).isNull();
        assertThat(createdPost.getImageCaption()).isNull();

        verify(sightengineService, never()).moderateAndVerifyFile(any());
        verify(fileStorageService, never()).saveImage(any());
    }

    @Test
    void createPost_ShouldHandleEmptyTitleAndContent() throws Exception {
        mockMvc.perform(multipart("/post/create")
                        .file(emptyImagePart())
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
                            .file(emptyImagePart())
                            .param("blogId", testBlog.getId())
                            .param("title", postTitle)
                            .param("content", "Contenido de " + postTitle))
                    .andExpect(status().is3xxRedirection());
        }

        List<Post> blogPosts = postRepository.findByBlogId(testBlog.getId());
        assertThat(blogPosts).hasSize(posts.length + 1); // +1 por el post original del setUp

        for (String postTitle : posts) {
            boolean exists = blogPosts.stream()
                    .anyMatch(p -> p.getTitle().equals(postTitle));
            assertThat(exists).isTrue();
        }
    }

    @Test
    void createPost_ShouldRejectPost_WhenModerationFails() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any()))
                .thenReturn(ModerationResult.reject("NUDITY", "Imagen no permitida", "debug-nudity"));

        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", testBlog.getId())
                        .param("title", "Post rechazado")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=" + testBlog.getId()))
                .andExpect(flash().attribute("error", "Imagen no permitida"))
                .andExpect(flash().attribute("moderationReasonCode", "NUDITY"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount); // no se creó el post

        verify(fileStorageService, never()).saveImage(any());
    }
    // PRUEBAS DE ELIMINACIÓN DE POST

    @Test
    void deletePost_ShouldDeleteExistingPost() throws Exception {
        int initialPostCount = postRepository.findAll().size();

        mockMvc.perform(post("/post/delete")
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
                        .param("postId", "")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        int finalPostCount = postRepository.findAll().size();
        assertThat(finalPostCount).isEqualTo(initialPostCount);
    }

    // PRUEBAS DE FLUJO COMPLETO

    @Test
    void completeFlow_CreateAndDeletePost() throws Exception {
        String newTitle = "Post de Flujo Completo";
        String newContent = "Contenido del post de flujo completo";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyImagePart())
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
                        .file(emptyImagePart())
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
                        .file(emptyImagePart())
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

    // PRUEBA DE POST CON TODOS LOS CAMPOS

    @Test
    void createPost_ShouldCreatePostWithAllFields() throws Exception {
        String title = "Post Completo";
        String content = "Contenido completo del post";
        String imageCaption = "Descripción completa de la imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content)
                        .param("imageCaption", imageCaption))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals(title))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getTitle()).isEqualTo(title);
        assertThat(createdPost.getContent()).isEqualTo(content);
        assertThat(createdPost.getImageUrl()).isEqualTo(DEFAULT_STORED_FILENAME);
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }
}