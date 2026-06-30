package com.example.blogproject.controllers;

import com.example.blogproject.models.Post;
import com.example.blogproject.services.ModerationService;
import com.example.blogproject.services.PostService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ModerationService moderationService;

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

        if (imageUrl != null && !imageUrl.isBlank()) {

            String resultado = moderationService.checkImage(imageUrl);

            if (resultado == null) {
                // No se pudo contactar a Sightengine (red caída, timeout, etc.)
                System.out.println("Moderacion no disponible, se publica sin verificar: " + imageUrl);
            } else {

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(resultado);

                    if ("failure".equals(root.path("status").asText())) {
                        // Error a nivel de API (credenciales invalidas, url inaccesible, etc.)
                        String errorMsg = root.path("error").path("message").asText("desconocido");
                        System.out.println("Sightengine devolvio failure: " + errorMsg);
                    } else {

                        double erotica = root.path("nudity")
                                .path("erotica")
                                .asDouble();

                        double firearm = root.path("weapon")
                                .path("classes")
                                .path("firearm")
                                .asDouble();

                        double drugs = root.path("recreational_drug")
                                .path("prob")
                                .asDouble();

                        double alcohol = root.path("alcohol")
                                .path("prob")
                                .asDouble();

                        double violence = root.path("violence")
                                .path("prob")
                                .asDouble();

                        if (erotica >= 0.4 ||
                                firearm >= 0.4 ||
                                drugs >= 0.4 ||
                                alcohol >= 0.4 ||
                                violence >= 0.4) {

                            return "redirect:/post/new?blogId=" + blogId + "&error=image_not_allowed";
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Error al interpretar la respuesta de Sightengine: " + e.getMessage());
                }
            }
        }

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