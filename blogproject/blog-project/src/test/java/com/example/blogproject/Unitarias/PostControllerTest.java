package com.example.blogproject.Unitarias;

import com.example.blogproject.FileStorageService;
import com.example.blogproject.SightengineService;
import com.example.blogproject.controllers.PostController;
import com.example.blogproject.models.Post;
import com.example.blogproject.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private SightengineService sightengineService;

    @Mock
    private FileStorageService fileStorageService;

    // Quitamos @InjectMocks para controlar la inicialización exacta manualmente en el setUp
    private PostController postController;

    private MockMultipartFile mockFile;
    private MockMultipartFile emptyMockFile;

    @BeforeEach
    void setUp() {
        // 1. Inicializar los Mocks de Mockito
        MockitoAnnotations.openMocks(this);

        // 2. Instanciar el controlador pasando el servicio del constructor
        postController = new PostController(sightengineService);

        // 3. Inyectar con Reflection los servicios que usan @Autowired
        ReflectionTestUtils.setField(postController, "postService", postService);
        ReflectionTestUtils.setField(postController, "fileStorageService", fileStorageService);

        // 4. Configurar el entorno de vistas simulado de Spring MVC
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setViewResolvers(viewResolver)
                .build();

        // 5. Preparar archivos de prueba
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

    @Test
    void testNewPostForm() throws Exception {
        mockMvc.perform(get("/post/new")
                        .param("blogId", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "123"));
    }

    @Test
    void testCreatePostWithoutOptionalFields() throws Exception {
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(sightengineService, never()).moderateAndVerifyFile(any());
        verify(postService).save(any(Post.class));
    }

    @Test
    void testDeletePost() throws Exception {
        doNothing().when(postService).deleteById("abc");

        mockMvc.perform(post("/post/delete")
                        .param("postId", "abc")
                        .param("blogId", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        verify(postService).deleteById("abc");
    }

    @Test
    void testCreatePost() throws Exception {
        // Configuraciones limpias que Mockito ahora sí interceptará con éxito
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(true);
        when(fileStorageService.saveImage(any())).thenReturn("mocked-unique-image.jpg");
        doNothing().when(postService).save(any());

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post")
                        .param("imageCaption", "Una descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(sightengineService, times(1)).moderateAndVerifyFile(any());
        verify(fileStorageService, times(1)).saveImage(any());
        verify(postService, times(1)).save(any());
    }

    @Test
    void testCreatePostWithEmptyFields() throws Exception {
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .param("blogId", "123")
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
    }

    @Test
    void testCreatePostWithSpecialCharacters() throws Exception {
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .param("blogId", "123")
                        .param("title", "¡Post especial! @#$%")
                        .param("content", "Contenido con <html> & 'caracteres' especiales"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
    }

    @Test
    void testDeletePostWithNonExistentId() throws Exception {
        doNothing().when(postService).deleteById("nonexistent");

        mockMvc.perform(post("/post/delete")
                        .param("postId", "nonexistent")
                        .param("blogId", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        verify(postService).deleteById("nonexistent");
    }

    @Test
    void testCreatePostAndVerifyPostProperties() throws Exception {
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(true);
        when(fileStorageService.saveImage(any())).thenReturn("test-image.jpg");

        doNothing().when(postService).save(postCaptor.capture());

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", "123")
                        .param("title", "Título de Prueba")
                        .param("content", "Contenido de Prueba")
                        .param("imageCaption", "Mi foto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));

        Post capturedPost = postCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(capturedPost).isNotNull();
        org.assertj.core.api.Assertions.assertThat(capturedPost.getBlogId()).isEqualTo("123");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getImageUrl()).isEqualTo("test-image.jpg");
    }
    @Test
    void testCreatePostWithInappropriateImage() throws Exception {
        // Configurar el mock para que muerda el filtro de seguridad
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(false);

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .param("blogId", "123")
                        .param("title", "Post Inapropiado")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "La imagen no puede ser publicada porque contiene contenido inapropiado."));

        // Asegurar que NUNCA se guardó la imagen ni el post
        verify(fileStorageService, never()).saveImage(any());
        verify(postService, never()).save(any());
    }

    @Test
    void testCreatePostStorageException() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(true);
        // Forzar un error de E/S o excepción genérica
        when(fileStorageService.saveImage(any())).thenThrow(new RuntimeException("Disco lleno"));

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .param("blogId", "123")
                        .param("title", "Post Fallido")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attributeExists("error"));

        verify(postService, never()).save(any());
    }


}

