package com.example.blogproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadsDir;

    public WebConfig(@Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        // Ubicación principal: directorio externo configurable. Como respaldo se
        // mantiene classpath:/static/uploads/ para no romper imágenes ya existentes.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dir.toUri().toString(), "classpath:/static/uploads/");
    }
}