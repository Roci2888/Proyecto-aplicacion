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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private PostService postService;

    // Ver blog y sus posts
    @GetMapping("/blog/{id}") // Responde a peticiones HTTP GET en la ruta /blog/ seguido de un ID dinámico
    public String viewBlog(@PathVariable String id, Model model) { // @PathVariable vincula el {id} de la URL
        // al parámetro 'id' del metodo

        // Busca el blog en la base de datos utilizando el ID recibido en la URL
        Blog blog = blogService.findById(id);

        // Si el blog no existe (es nulo)
        if (blog == null) {
            return "redirect:/user"; // ...redirige al usuario a su panel de control
        }

        // Obtiene la lista de todos los posts de este blog especifico
        List<Post> posts = postService.findByBlogId(id);

        // Envía el objeto blog a la vista
        model.addAttribute("blog", blog);
        // Envía la lista de publicaciones a la vista
        model.addAttribute("posts", posts);

        return "blog-view";
    }

    // Mostrar formulario para crear blog
    @GetMapping("/blog/create")
    public String showCreateForm(HttpSession session, Model model) {

        // Recupera el usuario que tiene la sesión activa en el navegador
        User user = (User) session.getAttribute("usuario");

        // Si el usuario no ha iniciado sesión...
        if (user == null) {
            return "redirect:/login"; // ... lo redirige a login
        }

        // Busca si este usuario ya tiene un blog registrado
        Blog existing = blogService.findByUserId(user.getId());

        // Si el servicio encuentra que ya tiene un blog...
        if (existing != null) {
            return "redirect:/user"; // ... lo devuelve al panel
        }

        // Si está logueado y no tiene ningún blog puede crear uno
        return "blog-create";
    }

    // 🔹 Guardar nuevo blog
    @PostMapping("/blog/create")
    public String createBlog(HttpSession session,
                             @RequestParam String name,
                             @RequestParam String description) {

        // Recupera al usuario de la sesión actual
        User user = (User) session.getAttribute("usuario");

        // Si la sesión expiró o no está logueado, lo manda al login
        if (user == null) {
            return "redirect:/login";
        }

        // Verifica otra vez que no tenga ya un blog
        Blog existing = blogService.findByUserId(user.getId());

        // Si ya tiene un blog, frena el proceso de guardado
        if (existing != null) {
            return "redirect:/user";
        }

        // Instancia un nuevo objeto de la entidad Blog
        Blog blog = new Blog();
        // Asigna desde el formulario
        blog.setUserId(user.getId());
        blog.setName(name);
        blog.setDescription(description);

        blogService.save(blog);

        return "redirect:/user?created";
    }
}