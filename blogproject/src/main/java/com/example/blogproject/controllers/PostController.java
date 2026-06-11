package com.example.blogproject.controllers;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.models.User;
import com.example.blogproject.services.BlogService;
import com.example.blogproject.services.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller // Spring MVC encargado de manejar peticiones y devolver vistas HTML
public class PostController {

    @Autowired // Inyecta automáticamente la lógica de negocio del servicio de posts (publicaciones)
    private PostService postService;

    // Mostrar formulario nuevo post
    @GetMapping("/post/new")
    public String newPostForm(@RequestParam String blogId, Model model) { // Captura el parámetro 'blogId' de la URL

        // Pasa el ID del blog al modelo para que el formulario sepa a qué blog va a pertenecer este post
        model.addAttribute("blogId", blogId);

        return "post-form";
    }

    // Crear post (asigna createdAt automático)
    @PostMapping("/post/create")
    public String createPost(
            @RequestParam String blogId, // Captura obligatoriamente
            @RequestParam String title, // Captura obligatoriamente
            @RequestParam String content, // Captura obligatoriamente
            @RequestParam(required = false) String imageUrl, // Captura la URL de la imagen (es opcional)
            @RequestParam(required = false) String imageCaption // Captura el pie de foto de la imagen (opcional)
    ) {

        // Instancia un nuevo objeto de la entidad Post
        Post post = new Post();

        // Asignaciones
        post.setBlogId(blogId);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setImageCaption(imageCaption);
        // Registra la fecha y hora de creacion
        post.setCreatedAt(LocalDateTime.now());

        // Guarda el objeto post en la base de datos
        postService.save(post);

        // Se ve el blog con su nueva publicación ya incluida
        return "redirect:/blog/" + blogId;
    }

    // Eliminar post
    @PostMapping("/post/delete")
    public String deletePost(
            @RequestParam String postId, // Captura el ID del post que se desea borrar
            @RequestParam String blogId // Captura el ID del blog al que pertenece
    ) {
        // Llama al servicio para borrar el registro de la base de datos usando el ID
        postService.deleteById(postId);

        // Redirige al panel de usuario donde se gestionan las publicaciones
        return "redirect:/user?view=posts";
    }
}