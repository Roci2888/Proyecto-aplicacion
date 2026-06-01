package com.example.blogproject.controllers;

import com.example.blogproject.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUpUserTest() {
        session = new MockHttpSession();
    }

    @Test
    void userPanel_ShouldShowUserInfo_WhenUserLoggedIn() throws Exception {
        // Given
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("blog", testBlog));
    }

    @Test
    void userPanel_ShouldShowUserInfoWithoutBlog_WhenUserHasNoBlog() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("nobloguser2", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        // When & Then
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attribute("user", userWithoutBlog))
                .andExpect(model().attribute("blog", null));
    }

    @Test
    void userPanel_ShouldShowPosts_WhenViewParamIsPosts() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 3);
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void userPanel_ShouldHandlePagination_WhenViewingPosts() throws Exception {
        // Given: Crear 12 posts para probar paginación (3 páginas de 5)
        createMultiplePosts(testBlog.getId(), 12);
        session.setAttribute("usuario", testUser);

        // When & Then: Primera página
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attributeExists("posts"));

        // When & Then: Segunda página
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1));
    }

    @Test
    void userPanel_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        // When & Then
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void userPanel_ShouldNotShowPosts_WhenUserHasNoBlog() throws Exception {
        // Given: Usuario sin blog
        User userWithoutBlog = createAndSaveUser("nobloguser3", "pass123", "USER");
        session.setAttribute("usuario", userWithoutBlog);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts"))
                .andExpect(model().attributeDoesNotExist("currentPage"))
                .andExpect(model().attributeDoesNotExist("totalPages"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"profile", "settings", "dashboard", "stats"})
    void userPanel_ShouldNotShowPosts_WhenViewIsNotPosts(String viewValue) throws Exception {
        // Given
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("view", viewValue))
                .andExpect(model().attributeDoesNotExist("posts"));
    }

    @Test
    void userPanel_ShouldUseDefaultPage_WhenPageParamNotProvided() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 5);
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void userPanel_ShouldHandleNonNumericPage() throws Exception {
        // Given
        createMultiplePosts(testBlog.getId(), 5);
        session.setAttribute("usuario", testUser);

        // When & Then: Página no numérica debe usar valor por defecto (0)
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void userPanel_ShouldHandleEmptyPosts_WhenBlogHasNoPosts() throws Exception {
        // Given: Blog sin posts adicionales
        session.setAttribute("usuario", testUser);

        // When & Then
        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0));
    }
}