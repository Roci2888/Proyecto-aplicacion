package com.example.blogproject.controllers;

import com.example.blogproject.FileStorageService;
import com.example.blogproject.models.Post;
import com.example.blogproject.services.PostService;
import com.example.blogproject.SightengineService;
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
    private FileStorageService fileStorageService; // <-- NUEVO: Delegamos el manejo de archivos

    private final SightengineService sightengineService;

    public PostController(SightengineService sightengineService) {
        this.sightengineService = sightengineService;
    }

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

            // 1. Verificar si el usuario efectivamente subió un archivo
            if (imageFile != null && !imageFile.isEmpty()) {

                // 2. Llamamos a la función de moderación para archivos binarios
                boolean isSafe = sightengineService.moderateAndVerifyFile(imageFile);

                if (!isSafe) {
                    redirectAttributes.addFlashAttribute("error",
                            "La imagen no puede ser publicada porque contiene contenido inapropiado.");
                    return "redirect:/post/new?blogId=" + blogId;
                }

                // 3. NUEVO: Guardamos el archivo usando el servicio dedicado
                String uniqueFileName = fileStorageService.saveImage(imageFile);

                // Guardamos el nombre único definitivo para la base de datos
                post.setImageUrl(uniqueFileName);

                redirectAttributes.addFlashAttribute("success",
                        "¡Imagen moderada, aprobada y subida correctamente!");
            }

            // 4. Guardamos el post en la base de datos
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