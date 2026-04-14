package com.example.demo.controllers;

import com.example.demo.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class ImagenController {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private List<Post> posts = new ArrayList<>();

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("posts", posts);
        return "index";
    }

    @GetMapping("/subir")
    public String mostrarFormulario(Model model) {
        model.addAttribute("post", new Post());
        return "subir";
    }

    @PostMapping("/guardar")
    public String guardarPost(@ModelAttribute Post post,
                              @RequestParam("imagen") MultipartFile imagen,
                              RedirectAttributes redirectAttributes) {

        // Validar tamaño del texto
        if (post.getTexto().length() > 3000) {
            redirectAttributes.addFlashAttribute("error", "El texto excede los 3000 caracteres");
            return "redirect:/subir";
        }

        // Guardar imagen
        if (!imagen.isEmpty()) {
            try {
                // Crear directorio si no existe
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Generar nombre único para la imagen
                String nombreImagen = UUID.randomUUID().toString() + "_" + imagen.getOriginalFilename();
                Path filePath = uploadPath.resolve(nombreImagen);
                Files.copy(imagen.getInputStream(), filePath);

                post.setNombreImagen(nombreImagen);

            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen");
                return "redirect:/subir";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una imagen");
            return "redirect:/subir";
        }

        posts.add(post);
        redirectAttributes.addFlashAttribute("mensaje", "Post guardado exitosamente");
        return "redirect:/";
    }
}
