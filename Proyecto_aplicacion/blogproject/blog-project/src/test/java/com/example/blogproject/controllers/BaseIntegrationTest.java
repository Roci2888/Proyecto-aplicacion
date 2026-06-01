package com.example.blogproject.controllers;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import com.example.blogproject.repositories.BlogRepository;
import com.example.blogproject.repositories.PostRepository;
import com.example.blogproject.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BlogRepository blogRepository;

    @Autowired
    protected PostRepository postRepository;

    protected MockHttpSession session;
    protected User testUser;
    protected User adminUser;
    protected Blog testBlog;
    protected Post testPost;

    // ✅ Clase concreta para User (solución para clase abstracta)
    protected static class ConcreteUser extends User {
        private String id;
        private String username;
        private String password;
        private String role;

        @Override
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @Override
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @Override
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        @Override
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    @BeforeEach
    void setUpBase() {
        // Limpiar repositorios
        postRepository.deleteAll();
        blogRepository.deleteAll();
        userRepository.deleteAll();

        // Crear sesión
        session = new MockHttpSession();

        // Crear usuario normal
        testUser = createAndSaveUser("testuser", "password123", "USER");

        // Crear usuario admin
        adminUser = createAndSaveUser("admin", "admin123", "ADMIN");

        // Crear blog para usuario normal
        testBlog = createAndSaveBlog(testUser.getId(), "Blog Test", "Descripción del blog");

        // Crear post para el blog
        testPost = createAndSavePost(testBlog.getId(), "Post Test", "Contenido del post");
    }

    // ✅ Métodos auxiliares
    protected User createAndSaveUser(String username, String password, String role) {
        ConcreteUser user = new ConcreteUser();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return userRepository.save(user);
    }

    protected Blog createAndSaveBlog(String userId, String name, String description) {
        Blog blog = new Blog();
        blog.setId(UUID.randomUUID().toString());
        blog.setUserId(userId);
        blog.setName(name);
        blog.setDescription(description);
        return blogRepository.save(blog);
    }

    protected Post createAndSavePost(String blogId, String title, String content) {
        Post post = new Post();
        post.setId(UUID.randomUUID().toString());
        post.setBlogId(blogId);
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    protected void createMultiplePosts(String blogId, int count) {
        for (int i = 1; i <= count; i++) {
            Post post = new Post();
            post.setId(UUID.randomUUID().toString());
            post.setBlogId(blogId);
            post.setTitle("Post " + i);
            post.setContent("Contenido del post " + i);
            post.setCreatedAt(LocalDateTime.now().minusDays(i));
            postRepository.save(post);
        }
    }
}
