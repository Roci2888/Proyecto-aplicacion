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

    @GetMapping("/user")
    public String userPanel(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String view,
            @RequestParam(defaultValue = "0") int page
    ) {

        User user = (User) session.getAttribute("usuario");

        if (user == null) {
            return "redirect:/login";
        }

        Blog blog = blogService.findByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("blog", blog);
        model.addAttribute("view", view);

        //  PAGINACIÓN
        if ("posts".equals(view) && blog != null) {

            var postPage = postService.getPostsByBlogPaged(blog.getId(), page, 5);

            model.addAttribute("posts", postPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", postPage.getTotalPages());
        }

        return "user";
    }
}