package com.example.blogproject.controllers;

import com.example.blogproject.models.Post;
import com.example.blogproject.services.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PostControllerIntegrationTest extends BaseIntegrationTest {

    private BlogService postService;

    @BeforeEach
    void setUpPostTest() {
        session = new MockHttpSession();
    }

    @Test
    void newPostForm_ShouldShowForm_WhenCalled() throws Exception {
        // When & Then
        mockMvc.perform(get("/post/new")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("post-form"))
                .andExpect(model().attribute("blogId", testBlog.getId()));
    }

    @Test
    void createPost_ShouldCreateNewPost_WhenValidData() throws Exception {
        // Given
        int initialPostCount = postRepository.findByBlogId(testBlog.getId()).size();

        // When & Then
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", "Nuevo Post de Integración")
                        .param("content", "Contenido del nuevo post")
                        .param("imageUrl", "https://ejemplo.com/nueva-imagen.jpg")
                        .param("imageCaption", "Nueva imagen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Verificar que el post fue creado
        int finalPostCount = postRepository.findByBlogId(testBlog.getId()).size();
        assertThat(finalPostCount).isEqualTo(initialPostCount + 1);

        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals("Nuevo Post de Integración"))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getContent()).isEqualTo("Contenido del nuevo post");
        assertThat(createdPost.getImageUrl()).isEqualTo("https://ejemplo.com/nueva-imagen.jpg");
        assertThat(createdPost.getImageCaption()).isEqualTo("Nueva imagen");
        assertThat(createdPost.getCreatedAt()).isNotNull();
    }

    @Test
    void createPost_ShouldCreatePostWithoutImage() {
        // Given
        Post newPost = new Post();
        newPost.setBlogId(testBlog.getId());
        newPost.setTitle("Post sin imagen");
        newPost.setContent("Contenido");
        newPost.setCreatedAt(LocalDateTime.now());

        // When
        postService.save(newPost);

        // Then
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());

        Post createdPost = posts.stream()
                .filter(p -> "Post sin imagen".equals(p.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Post no encontrado"));

        assertThat(createdPost.getImageUrl()).isNull();
        assertThat(createdPost.getImageCaption()).isNull();
    }


    @Test
    void createPost_ShouldAllowMultiplePostsForSameBlog() throws Exception {
        // Given
        int initialPostCount = postRepository.findByBlogId(testBlog.getId()).size();

        // When: Crear 3 posts
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/post/create")
                            .param("blogId", testBlog.getId())
                            .param("title", "Post Múltiple " + i)
                            .param("content", "Contenido " + i))
                    .andExpect(status().is3xxRedirection());
        }

        // Then
        int finalPostCount = postRepository.findByBlogId(testBlog.getId()).size();
        assertThat(finalPostCount).isEqualTo(initialPostCount + 3);
    }

    @Test
    void deletePost_ShouldDeletePostAndRedirect() throws Exception {
        // Given: Post existe
        assertThat(postRepository.findById(testPost.getId())).isPresent();

        // When & Then
        mockMvc.perform(post("/post/delete")
                        .param("postId", testPost.getId())
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Verificar que el post fue eliminado
        assertThat(postRepository.findById(testPost.getId())).isEmpty();
    }

    @Test
    void deletePost_ShouldHandleNonExistentPost() throws Exception {
        // When & Then
        mockMvc.perform(post("/post/delete")
                        .param("postId", "nonexistent-id")
                        .param("blogId", testBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));
    }

    @Test
    void createPost_ShouldHandleSpecialCharacters() throws Exception {
        // Given
        String specialTitle = "¡Post con @caracteres #especiales $100!";
        String specialContent = "Contenido con <html> & más 'caracteres' especiales";

        // When
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", specialTitle)
                        .param("content", specialContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Then - Usando Optional de forma más limpia
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());

        Post createdPost = posts.stream()
                .filter(p -> specialTitle.equals(p.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Post with title '" + specialTitle + "' not found"));

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getContent()).isEqualTo(specialContent);
        assertThat(createdPost.getTitle()).isEqualTo(specialTitle);
    }

    @Test
    void createPost_ShouldHandleVeryLongContent() throws Exception {
        // Given: Contenido de 5000 caracteres
        String longContent = "A".repeat(5000);

        // When & Then
        mockMvc.perform(post("/post/create")
                        .param("blogId", testBlog.getId())
                        .param("title", "Post con contenido largo")
                        .param("content", longContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + testBlog.getId()));

        // Verificar contenido largo
        List<Post> posts = postRepository.findByBlogId(testBlog.getId());
        Post createdPost = posts.stream()
                .filter(p -> p.getTitle().equals("Post con contenido largo"))
                .findFirst()
                .orElse(null);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getContent()).isEqualTo(longContent);
    }
}