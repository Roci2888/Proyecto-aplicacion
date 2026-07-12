package com.example.blogproject.web.controller;

import com.example.blogproject.domain.model.Post;
import com.example.blogproject.application.service.PostService;
import com.example.blogproject.infrastructure.moderation.SightengineService;
import com.example.blogproject.infrastructure.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private SightengineService sightengineService;
    @Autowired
    private FileStorageService fileStorageService;

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
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(required = false) String imageCaption,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Post post = new Post();
            post.setBlogId(blogId);
            post.setTitle(title);
            post.setContent(content);
            post.setImageCaption(imageCaption);
            post.setCreatedAt(LocalDateTime.now());

            // 1. Verificar si el usuario subió un archivo
            if (imageFile != null && !imageFile.isEmpty()) {

                // 2. Moderar el archivo con Sightengine
                boolean isSafe = sightengineService.moderateAndVerifyFile(imageFile);

                if (!isSafe) {
                    redirectAttributes.addFlashAttribute("error",
                            "La imagen no puede ser publicada porque contiene contenido inapropiado.");
                    return "redirect:/post/new?blogId=" + blogId;
                }

                // 3. Guardar el archivo en disco y quedarnos con el nombre único
                String uniqueFileName = fileStorageService.saveImage(imageFile);
                post.setImageUrl(uniqueFileName);

                redirectAttributes.addFlashAttribute("success",
                        "¡Imagen moderada, aprobada y subida correctamente!");
            }

            // 4. Guardar el post
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