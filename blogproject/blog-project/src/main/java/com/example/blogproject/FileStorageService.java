package com.example.blogproject;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

        public String saveImage(MultipartFile imageFile) throws Exception {
            // Generar un nombre único para evitar colisiones de archivos
            String uniqueFileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

            // Obtener ruta raíz
            String rootPath = new File(".").getAbsolutePath();

            // === UBICACIÓN A: Carpeta de código fuente (Persistencia en desarrollo) ===
            String devPath = rootPath + "/src/main/resources/static/uploads/";
            File devDirectory = new File(devPath);
            if (!devDirectory.exists()) {
                devDirectory.mkdirs();
            }
            File devFile = new File(devPath + uniqueFileName);
            imageFile.transferTo(devFile); // Guardado físico inicial

            // === UBICACIÓN B: Carpeta de salida del Servidor (Renderizado inmediato) ===
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

