package com.example.blogproject;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // Cambiamos el parámetro de 'MultipartFile' a 'byte[]' junto con el nombre original
    public String saveImage(byte[] imageBytes, String originalFilename) throws Exception {
        // Generar un nombre único para evitar colisiones de archivos
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // Obtener ruta raíz
        String rootPath = new File(".").getAbsolutePath();

        // === UBICACIÓN A: Carpeta de código fuente ===
        String devPath = rootPath + "/src/main/resources/static/uploads/";
        File devDirectory = new File(devPath);
        if (!devDirectory.exists()) {
            devDirectory.mkdirs();
        }
        File devFile = new File(devPath + uniqueFileName);

        // 🛡️ GUARDADO SEGURO: Escribimos los bytes directamente en lugar de usar transferTo
        Files.write(devFile.toPath(), imageBytes);

        // === UBICACIÓN B: Carpeta de salida del Servidor ===
        String targetPath = rootPath + "/target/classes/static/uploads/";
        File targetDirectory = new File(targetPath);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        File targetFile = new File(targetPath + uniqueFileName);

        // Duplicamos el archivo en caliente usando NIO
        Files.copy(devFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }
}

