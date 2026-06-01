package com.example.blogproject.controllers;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class BlogControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUpBlogTest() {
        session = new MockHttpSession();
    }

    @Test
    void viewBlog_ShouldShowBlogAndPosts_WhenBlogExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/blog/{blogId}", testBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-view"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("blog", testBlog));
    }

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
        assertThat(createdBlog.getName()).isEqualTo("");
        assertThat(createdBlog.getDescription()).isEqualTo("");
    }
}

