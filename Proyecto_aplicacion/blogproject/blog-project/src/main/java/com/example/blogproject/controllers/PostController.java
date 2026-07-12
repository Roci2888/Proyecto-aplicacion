package com.example.blogproject.controllers;

import com.example.blogproject.FileStorageService;
import com.example.blogproject.ModerationResult;
import com.example.blogproject.SightengineService;
import com.example.blogproject.models.Post;
import com.example.blogproject.services.PostService;
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
    private FileStorageService fileStorageService;

    private final SightengineService sightengineService;

    public PostController(SightengineService sightengineService) {
        this.sightengineService = sightengineService;
    }

    // ✅ CORREGIDO - GET con @RequestParam
    @GetMapping("/post/new")
    public String newPostForm(@RequestParam String blogId, Model model) {
        logger.info("Mostrando formulario para blogId: {}", blogId);
        model.addAttribute("blogId", blogId);
        return "post-form";
    }

    // ✅ CORREGIDO - POST sin @RequestBody
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
            logger.info("Creando nuevo post para blogId: {}", blogId);
            logger.info("  - Título: {}", title);
            logger.info("  - Imagen: {}", imageFile != null ? imageFile.getOriginalFilename() : "Sin imagen");

            byte[] imageBytes = null;
            String originalFilename = null;
            String moderationReasonCode = "NO_IMAGE";

            if (imageFile != null && !imageFile.isEmpty()) {
                logger.info("Procesando imagen: {}", imageFile.getOriginalFilename());

                imageBytes = imageFile.getBytes();
                originalFilename = imageFile.getOriginalFilename();

                // Ejecutar moderación
                ModerationResult moderationResult = sightengineService.moderateAndVerifyFile(imageFile);
                moderationReasonCode = moderationResult.getReasonCode();

                logger.info("📋 Resultado moderación: allowed={}, reasonCode={}",
                        moderationResult.isAllowed(), moderationResult.getReasonCode());

                if (!moderationResult.isAllowed()) {
                    redirectAttributes.addFlashAttribute("error", moderationResult.getUserMessage());
                    redirectAttributes.addFlashAttribute("moderationReasonCode", moderationResult.getReasonCode());

                    logger.warn("Imagen rechazada. reasonCode={}, debug={}",
                            moderationResult.getReasonCode(),
                            moderationResult.getDebugMessage());

                    return "redirect:/post/new?blogId=" + blogId;
                }

                // APROBADO
                logger.info("Imagen aprobada");
                redirectAttributes.addFlashAttribute("moderationReasonCode", moderationReasonCode);

            } else {
                logger.info("No se subió imagen");
                redirectAttributes.addFlashAttribute("moderationReasonCode", "NO_IMAGE");
            }

            // Crear el post
            Post post = new Post();
            post.setBlogId(blogId);
            post.setTitle(title);
            post.setContent(content);
            post.setImageCaption(imageCaption);
            post.setCreatedAt(LocalDateTime.now());

            // Guardar imagen si existe
            if (imageBytes != null && imageBytes.length > 0) {
                String uniqueFileName = fileStorageService.saveImage(imageBytes, originalFilename);
                post.setImageUrl("/uploads/" + uniqueFileName);
            }

            postService.save(post);

            logger.info("Post creado exitosamente con ID: {}", post.getId());
            redirectAttributes.addFlashAttribute("success", "¡Post creado exitosamente!");

            return "redirect:/blog/" + blogId;

        } catch (Exception e) {
            logger.error("Error al crear el post", e);
            redirectAttributes.addFlashAttribute("error", "Error al crear el post: " + e.getMessage());
            return "redirect:/post/new?blogId=" + blogId;
        }
    }

    @PostMapping("/post/delete")
    public String deletePost(@RequestParam String postId, @RequestParam String blogId) {
        logger.info("🗑️ Eliminando post: {}", postId);
        postService.deleteById(postId);
        return "redirect:/user?view=posts";
    }
}