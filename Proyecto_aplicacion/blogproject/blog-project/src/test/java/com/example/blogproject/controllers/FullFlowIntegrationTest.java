package com.example.blogproject.controllers;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FullFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void completeUserJourney_FromRegistrationToPostDeletion() throws Exception {
        MockHttpSession userSession = new MockHttpSession();

        // ========== 1. REGISTRO ==========
        String newUsername = "flowuser";
        String newPassword = "flowpass";

        mockMvc.perform(post("/auth/register")
                        .param("username", newUsername)
                        .param("password", newPassword)
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success"));

        // Verificar usuario creado
        Optional<User> newUser = userRepository.findByUsername(newUsername);
        assertThat(newUser).isNotNull();

        // ========== 2. LOGIN ==========
        mockMvc.perform(post("/auth/login")
                        .param("username", newUsername)
                        .param("password", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        // ========== 3. CREAR BLOG ==========
        userSession.setAttribute("usuario", newUser);

        mockMvc.perform(post("/blog/create")
                        .session(userSession)
                        .param("name", "Blog del Usuario Flow")
                        .param("description", "Blog creado en prueba de flujo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?created"));

        // Verificar blog creado
        Blog createdBlog = blogRepository.findByUserId(String.valueOf(newUser.get()));
        assertThat(createdBlog).isNotNull();
        assertThat(createdBlog.getName()).isEqualTo("Blog del Usuario Flow");

        // ========== 4. CREAR POSTS ==========
        // Primer post
        mockMvc.perform(post("/post/create")
                        .param("blogId", createdBlog.getId())
                        .param("title", "Primer Post Flow")
                        .param("content", "Contenido del primer post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + createdBlog.getId()));

        // Segundo post con imagen
        mockMvc.perform(post("/post/create")
                        .param("blogId", createdBlog.getId())
                        .param("title", "Segundo Post Flow")
                        .param("content", "Contenido del segundo post")
                        .param("imageUrl", "https://ejemplo.com/flow-image.jpg")
                        .param("imageCaption", "Imagen del flow"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/" + createdBlog.getId()));

        // Verificar posts creados
        List<Post> posts = postRepository.findByBlogId(createdBlog.getId());
        assertThat(posts).hasSize(2);

        // ========== 5. VER BLOG ==========
        mockMvc.perform(get("/blog/" + createdBlog.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("blog-view"))
                .andExpect(model().attributeExists("blog", "posts"));

        // ========== 6. VER PANEL CON POSTS ==========
        mockMvc.perform(get("/user")
                        .session(userSession)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("posts"));

        // ========== 7. ELIMINAR UN POST ==========
        Post postToDelete = posts.get(0);

        mockMvc.perform(post("/post/delete")
                        .param("postId", postToDelete.getId())
                        .param("blogId", createdBlog.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?view=posts"));

        // Verificar post eliminado
        assertThat(postRepository.findById(postToDelete.getId())).isEmpty();

        // ========== 8. VERIFICAR POST RESTANTE ==========
        List<Post> remainingPosts = postRepository.findByBlogId(createdBlog.getId());
        assertThat(remainingPosts).hasSize(1);

        // ========== 9. LOGOUT ==========
        mockMvc.perform(get("/logout").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // ========== 10. VERIFICAR ACCESO DENEGADO ==========
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminFlow_ShouldManageUsersAndBlogs() throws Exception {
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("usuario", adminUser);

        // ========== 1. CREAR USUARIO NORMAL ==========
        String normalUser = "normaluser";
        mockMvc.perform(post("/auth/register")
                        .param("username", normalUser)
                        .param("password", "pass123")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection());

        Optional<User> createdUser = userRepository.findByUsername(normalUser);
        assertThat(createdUser).isNotNull();

        // ========== 2. ADMIN CREA BLOG PARA USUARIO ==========
        // Nota: El admin normalmente no crea blogs directamente,
        // pero puede gestionar usuarios

        // ========== 3. ADMIN VE LISTA DE USUARIOS ==========
        mockMvc.perform(get("/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("users"));

        // ========== 4. ADMIN ELIMINA USUARIO ==========
        mockMvc.perform(post("/admin/delete/" + createdUser.get())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        // Verificar usuario eliminado
        assertThat(userRepository.findByUsername(normalUser)).isNull();
    }
}
