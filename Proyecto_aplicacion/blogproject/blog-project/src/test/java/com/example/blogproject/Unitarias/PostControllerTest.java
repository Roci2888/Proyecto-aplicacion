package com.example.blogproject.Unitarias;

import com.example.blogproject.FileStorageService;
import com.example.blogproject.ModerationResult;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;


@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private SightengineService sightengineService;

    @Mock
    private FileStorageService fileStorageService;

    private PostController postController;

    private MockMultipartFile mockFile;
    private MockMultipartFile emptyMockFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        postController = new PostController(sightengineService);

        ReflectionTestUtils.setField(postController, "postService", postService);
        ReflectionTestUtils.setField(postController, "fileStorageService", fileStorageService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setViewResolvers(viewResolver)
                .build();

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
        ModerationResult approvedResult = new ModerationResult(
                true,
                "approved",
                "Imagen aprobada",
                null
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(approvedResult);

        // ✅ CORREGIDO: Se pasan los dos argumentos (byte[], String)
        when(fileStorageService.saveImage(any(byte[].class), anyString())).thenReturn("mocked-unique-image.jpg");
        doNothing().when(postService).save(any());

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post")
                        .param("imageCaption", "Una descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(sightengineService, times(1)).moderateAndVerifyFile(any());
        verify(fileStorageService, times(1)).saveImage(any(byte[].class), anyString());
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

        ModerationResult approvedResult = new ModerationResult(
                true,
                "approved",
                "Imagen aprobada",
                null
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(approvedResult);

        // ✅ CORREGIDO: Se pasan los dos argumentos (byte[], String)
        when(fileStorageService.saveImage(any(byte[].class), anyString())).thenReturn("test-image.jpg");

        doNothing().when(postService).save(postCaptor.capture());

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("blogId", "123")
                        .param("title", "Título de Prueba")
                        .param("content", "Contenido de Prueba")
                        .param("imageCaption", "Mi foto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        Post capturedPost = postCaptor.getValue();
        assertThat(capturedPost).isNotNull();
        assertThat(capturedPost.getBlogId()).isEqualTo("123");
        assertThat(capturedPost.getImageUrl()).isEqualTo("/uploads/test-image.jpg");
    }

    @Test
    void testCreatePostWithInappropriateImage() throws Exception {
        ModerationResult rejectedResult = new ModerationResult(
                false,
                "NSFW",
                "La imagen no puede ser publicada porque contiene contenido inapropiado.",
                "Contenido explícito detectado"
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(rejectedResult);

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .param("blogId", "123")
                        .param("title", "Post Inapropiado")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "La imagen no puede ser publicada porque contiene contenido inapropiado."));

        // ✅ CORREGIDO: Se especifica la firma correcta en la verificación
        verify(fileStorageService, never()).saveImage(any(byte[].class), anyString());
        verify(postService, never()).save(any());
    }

    @Test
    void testCreatePostStorageException() throws Exception {
        ModerationResult approvedResult = new ModerationResult(
                true,
                "approved",
                "Imagen aprobada",
                null
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(approvedResult);

        // ✅ CORREGIDO: Lanza excepción usando la firma correcta de 2 argumentos
        when(fileStorageService.saveImage(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Disco lleno"));

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

    /**
     * Prueba el catch general: si postService.save() lanza una excepción
     * (por ejemplo, error de base de datos) DESPUÉS de que la imagen fue aprobada,
     * el controlador debe redirigir a /post/new con un mensaje de error
     */
    @Test
    void testCreatePost_ShouldHandleException_WhenPostServiceSaveFails() throws Exception {
        ModerationResult approvedResult = new ModerationResult(
                true, "approved", "Imagen aprobada", null
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(approvedResult);
        when(fileStorageService.saveImage(any(byte[].class), anyString())).thenReturn("img.jpg");
        doThrow(new RuntimeException("Error de base de datos"))
                .when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .param("blogId", "123")
                        .param("title", "Post con error de BD")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attributeExists("error"));

        verify(postService, times(1)).save(any(Post.class));
    }

    /**
     * Prueba que cuando no se sube imagen, el flash attribute
     * moderationReasonCode quede como "NO_IMAGE"
     */
    @Test
    void testCreatePostWithoutOptionalFields_ShouldSetNoImageReasonCode() throws Exception {
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyMockFile)
                        .param("blogId", "123")
                        .param("title", "Post sin imagen")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"))
                .andExpect(flash().attribute("moderationReasonCode", "NO_IMAGE"));

        verify(sightengineService, never()).moderateAndVerifyFile(any());
    }

    /**
     * Prueba que cuando la imagen es aprobada, se propague el reasonCode
     * real devuelto por el servicio de moderación (no solo "NO_IMAGE")
     */
    @Test
    void testCreatePost_ShouldPropagateApprovedReasonCode() throws Exception {
        ModerationResult approvedResult = new ModerationResult(
                true, "approved", "Imagen aprobada", null
        );
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(approvedResult);
        when(fileStorageService.saveImage(any(byte[].class), anyString())).thenReturn("img.jpg");
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(mockFile)
                        .param("blogId", "123")
                        .param("title", "Post con imagen")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("moderationReasonCode", "approved"));
    }
}