package com.example.blogproject.Unitarias;

import com.example.blogproject.web.controller.UserController;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.blogproject.domain.model.Blog;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.domain.model.User;
import com.example.blogproject.application.service.BlogService;
import com.example.blogproject.application.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private MockHttpSession session;

    @Mock
    private BlogService blogService;

    @Mock
    private PostService postService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private Blog testBlog;
    private List<Post> testPosts;
    private Page<Post> testPostPage;

    @BeforeEach
    void setUp() {
        // ==================== IMPLEMENTACIÓN ANÓNIMA CONCRETA ====================
        // Esta es la solución más simple para clases abstractas
        testUser = new User() {
            @Override
            public String getId() { return "user123"; }

            @Override
            public String getUsername() { return "testuser"; }

            @Override
            public String getPassword() { return "password123"; }

            @Override
            public String getRole() { return "USER"; }
        };

        // Si User tiene más métodos abstractos, agrégalos aquí:
        // @Override
        // public String getEmail() { return "test@example.com"; }
        // etc.

        // Configurar el resolutor de vistas
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // Configurar MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setViewResolvers(viewResolver)
                .build();

        // Inicializar sesión simulada
        session = new MockHttpSession();

        // Configurar blog de prueba
        testBlog = new Blog();
        testBlog.setId("blog123");
        testBlog.setUserId("user123");
        testBlog.setName("Mi Blog");
        testBlog.setDescription("Descripción del blog");

        // Configurar posts de prueba
        Post post1 = new Post();
        post1.setId("post1");
        post1.setBlogId("blog123");
        post1.setTitle("Post 1");
        post1.setContent("Contenido 1");
        post1.setCreatedAt(LocalDateTime.now());

        Post post2 = new Post();
        post2.setId("post2");
        post2.setBlogId("blog123");
        post2.setTitle("Post 2");
        post2.setContent("Contenido 2");
        post2.setCreatedAt(LocalDateTime.now());

        testPosts = Arrays.asList(post1, post2);

        // Configurar página de posts
        testPostPage = new PageImpl<>(testPosts, PageRequest.of(0, 5), 2);
    }

    // ==================== PRUEBAS DE SEGURIDAD Y AUTENTICACIÓN ====================

    @Test
    void userPanel_ShouldRedirectToLogin_WhenNoUserInSession() throws Exception {
        mockMvc.perform(get("/user").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(blogService, never()).findByUserId(anyString());
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    @Test
    void userPanel_ShouldRedirectToLogin_WhenSessionIsNull() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ==================== PRUEBAS DE VISUALIZACIÓN DE POSTS CON PAGINACIÓN ====================

    @Test
    void userPanel_ShouldShowPosts_WhenViewIsPostsAndUserHasBlog() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeExists("user", "blog", "view"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attribute("user", testUser))
                .andExpect(model().attribute("blog", testBlog))
                .andExpect(model().attribute("view", "posts"))
                .andExpect(model().attribute("posts", testPosts))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, times(1)).getPostsByBlogPaged("blog123", 0, 5);
    }

    @Test
    void userPanel_ShouldHandleEmptyPosts_WhenBlogHasNoPosts() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(emptyPage);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0));
    }

    // ==================== PRUEBAS CON DIFERENTES VALORES DE VIEW ====================

    @ParameterizedTest
    @ValueSource(strings = {"profile", "settings", "dashboard", "stats", ""})
    void userPanel_ShouldNotShowPosts_WhenViewIsNotPosts(String viewValue) throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("view", viewValue))
                .andExpect(model().attributeDoesNotExist("posts"));

        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void userPanel_ShouldNotShowPosts_WhenViewIsNullOrEmpty(String viewValue) throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", viewValue))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts"));

        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    // ==================== PRUEBAS DE PAGINACIÓN CON DIFERENTES PÁGINAS ====================

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1, 1",
            "2, 2",
            "5, 5"
    })
    void userPanel_ShouldHandleDifferentPageNumbers(int pageNumber, int expectedPage) throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> pageResult = new PageImpl<>(testPosts, PageRequest.of(pageNumber, 5), 10);
        when(postService.getPostsByBlogPaged("blog123", pageNumber, 5)).thenReturn(pageResult);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", String.valueOf(pageNumber)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", expectedPage));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", pageNumber, 5);
    }

    @Test
    void userPanel_ShouldUseDefaultPage_WhenPageParamNotProvided() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 0, 5);
    }

    // ==================== PRUEBAS DE BLOG NULO Y CASOS BORDE ====================

    @Test
    void userPanel_ShouldNotLoadPosts_WhenBlogIsNullAndViewIsPosts() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(null);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attributeDoesNotExist("posts"))
                .andExpect(model().attributeDoesNotExist("currentPage"))
                .andExpect(model().attributeDoesNotExist("totalPages"));

        verify(blogService, times(1)).findByUserId("user123");
        verify(postService, never()).getPostsByBlogPaged(anyString(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "MODERATOR", "GUEST"})
    void userPanel_ShouldWorkForDifferentRoles(String role) throws Exception {
        // Para probar diferentes roles, necesitamos recrear el usuario con el nuevo rol
        testUser = new User() {
            @Override
            public String getId() { return "user123"; }
            @Override
            public String getUsername() { return "testuser"; }
            @Override
            public String getPassword() { return "password123"; }
            @Override
            public String getRole() { return role; }
        };

        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("user", testUser));

        verify(blogService, times(1)).findByUserId("user123");
    }

    // ==================== PRUEBAS CON VALORES EXTREMOS ====================

    @Test
    void userPanel_ShouldHandleVeryLargePageNumber() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);

        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(100, 5), 10);
        when(postService.getPostsByBlogPaged("blog123", 100, 5)).thenReturn(emptyPage);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts")
                        .param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 100))
                .andExpect(model().attribute("posts", Collections.emptyList()));

        verify(postService, times(1)).getPostsByBlogPaged("blog123", 100, 5);
    }

    // ==================== PRUEBAS DE ATRIBUTOS DEL MODELO ====================

    @Test
    void userPanel_ShouldOnlyHavePaginationAttributes_WhenViewIsPosts() throws Exception {
        session.setAttribute("usuario", testUser);
        when(blogService.findByUserId("user123")).thenReturn(testBlog);
        when(postService.getPostsByBlogPaged("blog123", 0, 5)).thenReturn(testPostPage);

        mockMvc.perform(get("/user")
                        .session(session)
                        .param("view", "posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("posts", "currentPage", "totalPages"));

        mockMvc.perform(get("/user").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("posts", "currentPage", "totalPages"));
    }
}