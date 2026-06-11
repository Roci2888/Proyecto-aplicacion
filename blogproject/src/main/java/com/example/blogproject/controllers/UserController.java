package com.example.blogproject.controllers;


import com.example.blogproject.models.Blog;
import com.example.blogproject.models.User;
import com.example.blogproject.services.BlogService;
import com.example.blogproject.services.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private PostService postService;

    // Panel principal con paginación
    @GetMapping("/user")
    public String userPanel(
            HttpSession session, // para validar la identidad
            Model model,
            @RequestParam(required = false) String view, // Captura la sección que el usuario quiere ver (opcional, ej: ?view=posts)
            @RequestParam(defaultValue = "0") int page // Captura el número de página a mostrar; si no se envía,
            // por defecto es la 0 (primera página)
    ) {

        // Intenta recuperar al usuario guardado en la sesión
        User user = (User) session.getAttribute("usuario");

        // Si no ha iniciado sesión o la sesión expiró...
        if (user == null) {
            return "redirect:/login"; // ... lo manda a la pantalla de login
        }

        // Busca en la base de datos si el usuario actual ya posee un blog creado
        Blog blog = blogService.findByUserId(user.getId());

        // Envía datos del formulario
        model.addAttribute("user", user);
        model.addAttribute("blog", blog);
        model.addAttribute("view", view);

        //  LÓGICA DE PAGINACIÓN
        // Si la sección solicitada es "posts" y efectivamente el usuario ya tiene un blog activo...
        if ("posts".equals(view) && blog != null) {

            // Solicita al servicio un fragmento (página) de publicaciones usando el ID del blog, la página actual
            // y un tamaño fijo de 5 elementos
            var postPage = postService.getPostsByBlogPaged(blog.getId(), page, 5);

            // Extrae y envía al HTML solo la lista con los 5 posts
            model.addAttribute("posts", postPage.getContent());
            // Envía el número de la página en la que se encuentra actualmente el usuario
            model.addAttribute("currentPage", page);
            // Envía la cantidad total de páginas calculadas (útil para pintar los botones de "Anterior" y "Siguiente")
            model.addAttribute("totalPages", postPage.getTotalPages());
        }
        
        return "user";
    }
}