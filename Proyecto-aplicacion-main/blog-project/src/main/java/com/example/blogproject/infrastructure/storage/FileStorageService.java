package com.example.blogproject.infrastructure.storage;

import com.example.blogproject.domain.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
public class FileStorageService implements FileStoragePort {

    private final Path uploadsDir;

    public FileStorageService(@Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @Override
    public String saveImage(MultipartFile imageFile) throws Exception {
        // Nombre único para evitar colisiones; se sanea el original para evitar
        // secuencias de path traversal (../) en el nombre de fichero.
        String original = StringUtils.cleanPath(
                imageFile.getOriginalFilename() == null ? "" : imageFile.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + original;

        Files.createDirectories(uploadsDir);

        Path target = uploadsDir.resolve(uniqueFileName).normalize();
        // Defensa en profundidad: el destino debe quedar dentro del directorio de subidas.
        if (!target.startsWith(uploadsDir)) {
            throw new IllegalArgumentException("Nombre de archivo no válido: " + original);
        }

        imageFile.transferTo(target);

        return uniqueFileName;
    }
}