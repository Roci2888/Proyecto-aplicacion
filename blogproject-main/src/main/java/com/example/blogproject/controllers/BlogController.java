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

    @GetMapping("/blog/{id}")
    public String viewBlog(@PathVariable String id, Model model) {

        Blog blog = blogService.findById(id);

        if (blog == null) {
            return "redirect:/user";
        }

        List<Post> posts = postService.findByBlogId(id);

        model.addAttribute("blog", blog);
        model.addAttribute("posts", posts);

        return "blog-view";
    }

    @PostMapping("/blog/delete/{id}")
    public String deleteBlog(@PathVariable String id,
                             HttpSession session) {

        User user = (User) session.getAttribute("usuario");

        if (user == null) {
            return "redirect:/login";
        }

        blogService.deleteBlog(id);

        return "redirect:/user";
    }

    // Mostrar form
    @GetMapping("/blog/create")
    public String showCreateForm(HttpSession session, Model model) {

        User user = (User) session.getAttribute("usuario");

        if (user == null) {
            return "redirect:/login";
        }

        // Evitar crear múltiples blogs
        Blog existing = blogService.findByUserId(user.getId());

        if (existing != null) {
            return "redirect:/user";
        }

        return "blog-create";
    }

    // 🔹 Guardar blog
    @PostMapping("/blog/create")
    public String createBlog(HttpSession session,
                             @RequestParam String name,
                             @RequestParam String description) {

        User user = (User) session.getAttribute("usuario");

        if (user == null) {
            return "redirect:/login";
        }

        // Evitar duplicados
        Blog existing = blogService.findByUserId(user.getId());

        if (existing != null) {
            return "redirect:/user";
        }

        Blog blog = new Blog();
        blog.setUserId(user.getId());
        blog.setName(name);
        blog.setDescription(description);

        blogService.save(blog);

        // redirección
        return "redirect:/user?created";
    }
}