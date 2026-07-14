package com.example.blogproject.web.controller;

import com.example.blogproject.domain.model.Blog;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.domain.model.User;
import com.example.blogproject.application.service.BlogService;
import com.example.blogproject.application.service.PostService;
import com.example.blogproject.web.dto.BlogForm;
import com.example.blogproject.web.security.SessionUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Mostrar form
    @GetMapping("/blog/create")
    public String showCreateForm(HttpSession session, Model model) {

        User user = SessionUtils.getCurrentUser(session);

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
                             @Valid @ModelAttribute BlogForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {

        User user = SessionUtils.getCurrentUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/blog/create";
        }

        // Evitar duplicados
        Blog existing = blogService.findByUserId(user.getId());

        if (existing != null) {
            return "redirect:/user";
        }

        Blog blog = new Blog();
        blog.setUserId(user.getId());
        blog.setName(form.getName());
        blog.setDescription(form.getDescription());

        blogService.save(blog);

        // redirección
        return "redirect:/user?created";
    }
}