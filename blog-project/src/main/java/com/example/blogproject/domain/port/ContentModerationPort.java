package com.example.blogproject.domain.port;

import org.springframework.web.multipart.MultipartFile;

public interface ContentModerationPort {

    /**
     * Analiza el fichero y decide si es publicable.
     *
     * @param file imagen a moderar
     * @return {@code true} si el contenido es seguro; {@code false} si debe bloquearse
     */
    boolean moderateAndVerifyFile(MultipartFile file);
}