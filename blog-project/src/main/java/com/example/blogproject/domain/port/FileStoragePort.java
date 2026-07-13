package com.example.blogproject.domain.port;

import org.springframework.web.multipart.MultipartFile;

public interface FileStoragePort {

    /**
     * Persiste la imagen y devuelve el nombre único con el que quedó almacenada.
     *
     * @param file imagen ya moderada y aprobada
     * @return nombre único del fichero almacenado (para guardar en la base de datos)
     * @throws Exception si falla la escritura del fichero
     */
    String saveImage(MultipartFile file) throws Exception;
}