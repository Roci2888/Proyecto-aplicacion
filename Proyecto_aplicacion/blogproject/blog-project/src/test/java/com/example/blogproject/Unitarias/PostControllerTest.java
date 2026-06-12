package com.example.blogproject.Unitarias;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.blogproject.controllers.PostController;
import com.example.blogproject.models.Post;
import com.example.blogproject.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @InjectMocks  //Esto inyecta el mock del servicio en el controlador REAL
    private PostController postController;

    @BeforeEach
    void setUp() {
        //Configurar el resolutor de vistas para procesar templates HTML
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        //Configurar MockMvc con el controlador REAL (no un mock)
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testNewPostForm() throws Exception {
        //Test simple sin necesidad de configurar mocks
        mockMvc.perform(get("/post/new")
                        .param("blogId", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", "123"));
    }

    @Test
    void testCreatePost() throws Exception {
        //Configurar el mock para que no haga nada al guardar
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(post("/post/create")
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post")
                        .param("imageUrl", "http://example.com/image.jpg")
                        .param("imageCaption", "Una descripción"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));  // ✅ Corregido: redirectedUrl en lugar de redirectUrl

        // Verificamos que el servicio intentó guardar un objeto Post
        verify(postService).save(any(Post.class));
    }

    @Test
    void testCreatePostWithoutOptionalFields() throws Exception {
        //Probar creación sin campos opcionales (imageUrl e imageCaption)
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(post("/post/create")
                        .param("blogId", "123")
                        .param("title", "Mi Primer Post")
                        .param("content", "Contenido del post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
    }

    @Test
    void testDeletePost() throws Exception {
        //Configurar el mock para que no haga nada al eliminar
        doNothing().when(postService).deleteById("abc");

        mockMvc.perform(post("/post/delete")
                        .param("postId", "abc")
                        .param("blogId", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));  // ✅ Corregido: redirectedUrl

        // Verificamos que el método de eliminación se llamó con el ID correcto
        verify(postService).deleteById("abc");
    }

    @Test
    void testCreatePostWithEmptyFields() throws Exception {
        //Probar con campos vacíos
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(post("/post/create")
                        .param("blogId", "123")
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
    }

    @Test
    void testCreatePostWithSpecialCharacters() throws Exception {
        //Probar con caracteres especiales
        doNothing().when(postService).save(any(Post.class));

        mockMvc.perform(post("/post/create")
                        .param("blogId", "123")
                        .param("title", "¡Post especial! @#$%")
                        .param("content", "Contenido con <html> & 'caracteres' especiales"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));
    }

    @Test
    void testDeletePostWithNonExistentId() throws Exception {
        //Probar eliminación de post inexistente
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
        //Usar ArgumentCaptor para verificar propiedades del post
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        doNothing().when(postService).save(postCaptor.capture());

        mockMvc.perform(post("/post/create")
                        .param("blogId", "123")
                        .param("title", "Título de Prueba")
                        .param("content", "Contenido de Prueba")
                        .param("imageUrl", "https://ejemplo.com/foto.jpg")
                        .param("imageCaption", "Mi foto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/123"));

        verify(postService).save(any(Post.class));

        // Verificar propiedades del post capturado
        Post capturedPost = postCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(capturedPost).isNotNull();
        org.assertj.core.api.Assertions.assertThat(capturedPost.getBlogId()).isEqualTo("123");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getTitle()).isEqualTo("Título de Prueba");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getContent()).isEqualTo("Contenido de Prueba");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getImageUrl()).isEqualTo("https://ejemplo.com/foto.jpg");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getImageCaption()).isEqualTo("Mi foto");
        org.assertj.core.api.Assertions.assertThat(capturedPost.getCreatedAt()).isNotNull();
    }
}
