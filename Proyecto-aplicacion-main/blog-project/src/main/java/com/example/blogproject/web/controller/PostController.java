package com.example.blogproject.web.controller;

import com.example.blogproject.domain.model.ModerationResult;
import com.example.blogproject.domain.model.Post;
import com.example.blogproject.application.service.PostService;
import com.example.blogproject.infrastructure.moderation.SightengineService;
import com.example.blogproject.infrastructure.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

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
                ModerationResult moderationResult = sightengineService.moderateAndVerifyFile(imageFile);

                logger.info("Resultado moderación: allowed={}, reasonCode={}",
                        moderationResult.isAllowed(), moderationResult.getReasonCode());

                if (!moderationResult.isAllowed()) {
                    redirectAttributes.addFlashAttribute("error", moderationResult.getUserMessage());
                    redirectAttributes.addFlashAttribute("moderationReasonCode", moderationResult.getReasonCode());

                    logger.warn("Imagen rechazada. reasonCode={}, debug={}",
                            moderationResult.getReasonCode(),
                            moderationResult.getDebugMessage());

                    return "redirect:/post/new?blogId=" + blogId;
                }

                // 3. Guardar el archivo en disco y quedarnos con el nombre único
                String uniqueFileName = fileStorageService.saveImage(imageFile);
                post.setImageUrl(uniqueFileName);

                redirectAttributes.addFlashAttribute("moderationReasonCode", moderationResult.getReasonCode());
                redirectAttributes.addFlashAttribute("success",
                        "¡Imagen moderada, aprobada y subida correctamente!");
            }

            // 4. Guardar el post
            postService.save(post);

            redirectAttributes.addFlashAttribute("success", "¡Post creado exitosamente!");
            return "redirect:/blog/" + blogId;

        } catch (Exception e) {
            // Registrar el detalle técnico sin filtrarlo al usuario.
            logger.error("Error al crear el post", e);
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo crear el post. Inténtalo de nuevo más tarde.");
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
