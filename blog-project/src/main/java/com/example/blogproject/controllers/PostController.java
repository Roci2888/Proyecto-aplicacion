package com.example.blogproject.controllers;

import com.example.blogproject.models.Post;
import com.example.blogproject.services.PostService;
import com.example.blogproject.SightengineService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class PostController {

    @Autowired
    private PostService postService;
    private final SightengineService sightengineService;

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
            @RequestParam(required = false) String imageCaption,
            RedirectAttributes redirectAttributes

    ) {
        try {
            // 1. Verificar si hay URL de imagen
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 2. Moderar la imagen
                boolean isSafe = sightengineService.moderateAndVerify(imageUrl);

                if (!isSafe) {
                    redirectAttributes.addFlashAttribute("error",
                            "La imagen no puede ser publicada porque contiene contenido inapropiado.");
                    return "redirect:/post/new?blogId=" + blogId;
                }

                redirectAttributes.addFlashAttribute("success",
                        "¡Imagen moderada y aprobada correctamente!");
            }

            Post post = new Post();
            post.setBlogId(blogId);
            post.setTitle(title);
            post.setContent(content);
            post.setImageUrl(imageUrl);
            post.setImageCaption(imageCaption);
            post.setCreatedAt(LocalDateTime.now());

            postService.save(post);

            redirectAttributes.addFlashAttribute("success", "¡Post creado exitosamente!");
            return "redirect:/blog/" + blogId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al crear el post: " + e.getMessage());
            return "redirect:/post/new?blogId=" + blogId;
        }
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