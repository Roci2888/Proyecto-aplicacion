package com.example.blogproject.web.controller;

import com.example.blogproject.domain.model.Post;
import com.example.blogproject.application.service.PostService;
import com.example.blogproject.infrastructure.moderation.SightengineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ModerationController {

    @Autowired
    private PostService postService;
    @Autowired
    private SightengineService sightengineService;

    @PostMapping("/admin/moderate-image")
    public String moderateImage(
            @RequestParam String postId,
            @RequestParam String blogId,
            RedirectAttributes redirectAttributes) {

        try {
            Post post = postService.findById(postId);

            if (post == null) {
                redirectAttributes.addFlashAttribute("error", "Post no encontrado");
                return "redirect:/blog/" + blogId;
            }

            if (post.getImageUrl() == null || post.getImageUrl().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "El post no tiene imagen para moderar");
                return "redirect:/blog/" + blogId;
            }

            // Moderar la imagen
            boolean isSafe = sightengineService.moderateAndVerify(post.getImageUrl());

            if (isSafe) {
                redirectAttributes.addFlashAttribute("success",
                        "Imagen aprobada - Contenido seguro");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Imagen rechazada - Contenido inapropiado");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al moderar: " + e.getMessage());
        }

        return "redirect:/blog/" + blogId;
    }
}