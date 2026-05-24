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
    @Autowired private BlogService blogService;
    @Autowired private PostService postService;

    @GetMapping("/blog/{id}")
    public String viewBlog(@PathVariable String id, Model model, HttpSession session) {

        Blog blog = blogService.findById(id);

        if (blog == null) return "redirect:/";

        User sessionUser = (User) session.getAttribute("usuario");
        boolean isOwner = sessionUser != null && sessionUser.getId().equals(blog.getUserId());

        if (blog.isDraft() && !isOwner) {
            return "redirect:/";
        }

        List<Post> posts = postService.findByBlogId(id);

        model.addAttribute("blog", blog);
        model.addAttribute("posts", posts);
        model.addAttribute("isOwner", isOwner);

        return "blog-view";
    }

    @GetMapping("/blog/create")
    public String showCreateForm(HttpSession session) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        return "blog-create";
    }

    @PostMapping("/blog/create")
    public String createBlog(HttpSession session,
                             @RequestParam String name,
                             @RequestParam String description,
                             @RequestParam(required = false) String coverImageUrl) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        Blog blog = new Blog();
        blog.setUserId(user.getId());
        blog.setName(name);
        blog.setDescription(description);
        blog.setStatus("DRAFT");
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            blog.setCoverImageUrl(coverImageUrl);
        }

        blogService.save(blog);
        return "redirect:/user?created";
    }

    @GetMapping("/blog/{id}/edit")
    public String showEditForm(@PathVariable String id,
                               HttpSession session, Model model) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        Blog blog = blogService.findById(id);

        if (blog == null || !blog.getUserId().equals(user.getId())) {
            return "redirect:/user";
        }

        model.addAttribute("blog", blog);
        return "blog-edit";
    }

    @PostMapping("/blog/{id}/edit")
    public String updateBlog(@PathVariable String id,
                             HttpSession session,
                             @RequestParam String name,
                             @RequestParam String description,
                             @RequestParam(required = false) String coverImageUrl) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        blogService.update(id, user.getId(), name, description, coverImageUrl);
        return "redirect:/user?updated";
    }

    @PostMapping("/blog/{id}/publish")
    public String publishBlog(@PathVariable String id, HttpSession session) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        blogService.publish(id, user.getId());
        return "redirect:/user?published";
    }

    @PostMapping("/blog/{id}/unpublish")
    public String unpublishBlog(@PathVariable String id, HttpSession session) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        blogService.unpublish(id, user.getId());
        return "redirect:/user?unpublished";
    }

    @PostMapping("/blog/{id}/delete")
    public String deleteBlog(@PathVariable String id, HttpSession session) {

        User user = (User) session.getAttribute("usuario");
        if (user == null) return "redirect:/login";

        blogService.delete(id, user.getId());
        return "redirect:/user?deleted";
    }
}