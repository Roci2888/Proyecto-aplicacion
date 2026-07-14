package com.example.blogproject.domain.port;

import com.example.blogproject.domain.model.ModerationResult;
import org.springframework.web.multipart.MultipartFile;

public interface ContentModerationPort {

    /**
     * Analiza el fichero y decide si es publicable.
     *
     * @param file imagen a moderar
     * @return el resultado de la moderación, con el código de motivo y los
     *         mensajes de usuario y de depuración
     */
    ModerationResult moderateAndVerifyFile(MultipartFile file);
}