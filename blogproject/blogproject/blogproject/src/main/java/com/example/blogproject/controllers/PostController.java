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

@Controller
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("/post/new")
    public String newPostForm(@RequestParam String blogId, Model model) {
        model.addAttribute("blogId", blogId);
        return "post-form";
    }

    @PostMapping("/post/create")
    public String createPost(
            @RequestParam String blogId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String imageCaption
    ) {

        Post post = new Post();
        post.setBlogId(blogId);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setImageCaption(imageCaption);
        post.setCreatedAt(LocalDateTime.now());

        postService.save(post);

        return "redirect:/blog/" + blogId;
    }

    @PostMapping("/post/delete")
    public String deletePost(
            @RequestParam String postId,
            @RequestParam String blogId
    ) {

        postService.deleteById(postId);

        return "redirect:/user?view=posts";
    }
}