package com.example.blogproject.Unitarias;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.blogproject.domain.model.ModerationResult;
import com.example.blogproject.infrastructure.moderation.SightengineService;
import com.example.blogproject.infrastructure.storage.FileStorageService;
import com.example.blogproject.web.controller.PostController;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.application.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private SightengineService sightengineService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks // Esto inyecta los mocks en el controlador REAL
    private PostController postController;

    private static final String DEFAULT_STORED_FILENAME = "stored-image.jpg";

    @BeforeEach
    void setUp() {
        // Configurar el resolutor de vistas para procesar templates HTML
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // Configurar MockMvc con el controlador REAL (no un mock)
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Helper: archivo multipart vacío (simula "sin imagen")
    private MockMultipartFile emptyImagePart() {
        return new MockMultipartFile("imageFile", "", "image/jpeg", new byte[0]);
    }

    // Helper: archivo multipart con contenido (simula "con imagen")
    private MockMultipartFile fakeImagePart() {
        return new MockMultipartFile("imageFile", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());
    }

    @Test
    void testCreatePost_ShouldHandleGenericException_WhenSaveFails() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());
        when(fileStorageService.saveImage(any())).thenReturn(DEFAULT_STORED_FILENAME);
        doThrow(new RuntimeException("DB down")).when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Post con error")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "No se pudo crear el post. Inténtalo de nuevo más tarde."));
    }

    @Test
    void testNewPostForm() throws Exception {
        // Test simple sin necesidad de configurar mocks
        mockMvc.perform(get("/post/new")
                        .param("blogId", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "123"));
    }

    @Test
    void testCreatePost() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());
        when(fileStorageService.saveImage(any())).thenReturn(DEFAULT_STORED_FILENAME);
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post")
                        .param("imageCaption", "Una descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
        verify(sightengineService).moderateAndVerifyFile(any());
        verify(fileStorageService).saveImage(any());
    }

    @Test
    void testCreatePostWithoutOptionalFields() throws Exception {
        // Sin imagen, no debería llamar a moderación ni a guardado de archivo
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyImagePart())
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
        verify(sightengineService, never()).moderateAndVerifyFile(any());
        verify(fileStorageService, never()).saveImage(any());
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
    void testCreatePostWithEmptyFields() throws Exception {
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(multipart("/post/create")
                        .file(emptyImagePart())
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
                        .file(emptyImagePart())
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
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());
        when(fileStorageService.saveImage(any())).thenReturn(DEFAULT_STORED_FILENAME);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        doNothing().when(postService).save(postCaptor.capture());

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
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
        org.assertj.core.api.Assertions.assertThat(capturedPost.getTitle()).isEqualTo("Título de Prueba");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getContent()).isEqualTo("Contenido de Prueba");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getImageUrl()).isEqualTo(DEFAULT_STORED_FILENAME);
        org.assertj.core.api.Assertions.assertThat(capturedPost.getImageCaption()).isEqualTo("Mi foto");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getCreatedAt()).isNotNull();
    }

    @Test
    void testCreatePost_ShouldRejectPost_WhenModerationFails() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any()))
                .thenReturn(ModerationResult.reject("NUDITY", "Imagen no permitida", "debug-nudity"));

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Post rechazado")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "Imagen no permitida"))
                .andExpect(flash().attribute("moderationReasonCode", "NUDITY"));

        verify(postService, never()).save(any(Post.class));
        verify(fileStorageService, never()).saveImage(any());
    }

    @Test
    void testCreatePost_ShouldHandleException_WhenFileStorageFails() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());
        when(fileStorageService.saveImage(any())).thenThrow(new RuntimeException("Disk full"));

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Post con error de storage")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "No se pudo crear el post. Inténtalo de nuevo más tarde."));

        verify(postService, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_ShouldHandleException_WhenModerationServiceThrows() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any()))
                .thenThrow(new RuntimeException("Sightengine API timeout"));

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Post con timeout")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/new?blogId=123"))
                .andExpect(flash().attribute("error", "No se pudo crear el post. Inténtalo de nuevo más tarde."));

        verify(fileStorageService, never()).saveImage(any());
    }

    @Test
    void testCreatePost_ShouldSetSuccessFlashMessage_WhenImageUploaded() throws Exception {
        when(sightengineService.moderateAndVerifyFile(any())).thenReturn(ModerationResult.allow());
        when(fileStorageService.saveImage(any())).thenReturn(DEFAULT_STORED_FILENAME);

        mockMvc.perform(multipart("/post/create")
                        .file(fakeImagePart())
                        .param("blogId", "123")
                        .param("title", "Post con imagen")
                        .param("content", "Contenido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "¡Post creado exitosamente!"));
    }

    @Test
    void testCreatePost_ShouldReturnBadRequest_WhenImageFilePartMissing() throws Exception {
        mockMvc.perform(multipart("/post/create")
                        .param("blogId", "123")
                        .param("title", "Sin parte de imagen")
                        .param("content", "Contenido"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testNewPostForm_ShouldReturnBadRequest_WhenBlogIdMissing() throws Exception {
        mockMvc.perform(get("/post/new"))
                .andExpect(status().is4xxClientError());
    }
}