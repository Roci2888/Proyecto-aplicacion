package com.example.blogproject.Integracion;

import com.example.blogproject.FileStorageService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private SightengineService sightengineService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private FileStorageService fileStorageService;

    private MockHttpSession session;
    private User testUser;
    private Blog testBlog;
    private Post testPost;

    private MockMultipartFile mockFile;
    private MockMultipartFile emptyMockFile;

    @BeforeEach
    void setUp() throws Exception {
        // Configuración por defecto de los componentes simulados para el flujo exitoso
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(true);
        when(fileStorageService.saveImage(any())).thenReturn("integration-test-image.jpg");

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

        // Archivos multipart simulados obligatorios para las peticiones
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

    // PRUEBAS DE CREACIÓN DE POST (CORREGIDAS A MULTIPART)

    @Test
    void createPost_ShouldCreateNewPost_WhenValidData() throws Exception {
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Nuevo Post";
        String newContent = "Contenido del nuevo post";
        String imageCaption = "Caption de la imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile) // Enviamos archivo simulado válido
                        .contentType(MediaType.MULTIPART_FORM_DATA)
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
        assertThat(createdPost.getImageUrl()).isEqualTo("integration-test-image.jpg");
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }

    @Test
    void createPost_ShouldCreatePostWithoutOptionalFields() throws Exception {
        int initialPostCount = postRepository.findAll().size();
        String newTitle = "Post sin imagen";
        String newContent = "Contenido sin imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile) // Enviamos Multipart vacío tal como lo espera el controlador
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
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .param("blogId", testBlog.getId())
                            .param("title", postTitle)
                            .param("content", "Contenido de " + postTitle))
                    .andExpect(status().is3xxRedirection());
        }

        List<Post> blogPosts = postRepository.findByBlogId(testBlog.getId());
        assertThat(blogPosts).hasSize(posts.length + 1); // +1 por el original

        for (String postTitle : posts) {
            boolean exists = blogPosts.stream()
                    .anyMatch(p -> p.getTitle().equals(postTitle));
            assertThat(exists).isTrue();
        }
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
                        .file(emptyMockFile)
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
    void createPost_ShouldCreatePostWithAllFields() throws Exception {
        String title = "Post Completo";
        String content = "Contenido completo del post";
        String imageCaption = "Descripción completa de la imagen";

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
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
        assertThat(createdPost.getImageUrl()).isEqualTo("integration-test-image.jpg");
        assertThat(createdPost.getImageCaption()).isEqualTo(imageCaption);
        assertThat(createdPost.getBlogId()).isEqualTo(testBlog.getId());
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }

    @Test
    void createPost_ShouldRejectPost_WhenImageContainsInappropriateContent() throws Exception {
        // Given: Forzamos a que el servicio de filtrado dictamine que la imagen NO es segura (false)
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(false);

        String title = "Post con Imagen Inapropiada";
        String content = "Contenido del post rechazado";

        // When: Ejecutamos la petición Multipart
        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content))
                // Then: Debe redirigir al formulario de creación, NO al blog
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=" + testBlog.getId()))
                // Verifica que se guarde el mensaje exacto en los atributos Flash (Session)
                .andExpect(flash().attribute("error", "La imagen no puede ser publicada porque contiene contenido inapropiado."));

        // Verificación adicional: Asegurar que el post NUNCA llegó a guardarse en la base de datos
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        boolean postExiste = posts.stream().anyMatch(p -> p.getTitle().equals(title));
        assertThat(postExiste).isFalse();
    }

    @Test
    void createPost_ShouldHandleException_WhenImageUploadFails() throws Exception {
        // Given: Simulamos un entorno seguro de imagen, pero el almacenamiento en disco/nube arroja un error imprevisto
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(true);
        when(fileStorageService.saveImage(any())).thenThrow(new RuntimeException("Error simulado de escritura en disco"));

        String title = "Post con Fallo de Almacenamiento";
        String content = "Contenido del post con error de disco";

        // When: Ejecutamos la acción
        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", testBlog.getId())
                        .param("title", title)
                        .param("content", content))
                // Then: El bloque catch del controlador debe atraparlo y redirigir al formulario original
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=" + testBlog.getId()))
                .andExpect(flash().attributeExists("error"));

        // Verificación adicional: Asegurar que la transacción falló y no se persistió nada corrupto
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        boolean postExiste = posts.stream().anyMatch(p -> p.getTitle().equals(title));
        assertThat(postExiste).isFalse();
    }
}

