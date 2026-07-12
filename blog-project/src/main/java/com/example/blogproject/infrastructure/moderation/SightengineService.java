package com.example.blogproject.infrastructure.moderation;

import com.example.blogproject.SightengineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SightengineService {

    private final SightengineProperties sightengineProperties;
    private final RestTemplate restTemplate;

    /**
     * Moderar imagen desde URL
     */
    public SightengineResponse moderateImage(String imageUrl) {
        return moderateImage(imageUrl, sightengineProperties.getModels());
    }

    public SightengineResponse moderateImage(String imageUrl, String models) {
        log.info("Moderando imagen desde URL: {}", imageUrl);

        String url = String.format("%s?api_user=%s&api_secret=%s&models=%s&url=%s",
                sightengineProperties.getUrl(),
                sightengineProperties.getUser(),
                sightengineProperties.getSecret(),
                models,
                imageUrl);

        try {
            ResponseEntity<SightengineResponse> response = restTemplate.getForEntity(
                    url,
                    SightengineResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Moderación completada exitosamente");
                return response.getBody();
            } else {
                throw new RuntimeException("Error en la moderación: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error al moderar imagen: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con SightEngine: " + e.getMessage());
        }
    }

    /**
     * Verificar si la imagen es segura basado en umbrales
     */
    public boolean isImageSafe(SightengineResponse response) {
        if (response == null) return false;

        // Umbrales de seguridad (ajústalos según tu necesidad)
        double nudityThreshold = 0.3;    // 30% de probabilidad de desnudo
        double offensiveThreshold = 0.4;  // 40% de probabilidad de contenido ofensivo
        double goreThreshold = 0.4;       // 40% de probabilidad de violencia gráfica

        // Verificar nudity
        if (response.getNudity() != null) {
            Double nudeScore = response.getNudity().get("raw");
            if (nudeScore != null && nudeScore > nudityThreshold) {
                log.warn("Imagen rechazada por desnudo: score={}", nudeScore);
                return false;
            }
        }

        // Verificar contenido ofensivo
        if (response.getOffensive() != null) {
            Double offensiveScore = response.getOffensive().get("prob");
            if (offensiveScore != null && offensiveScore > offensiveThreshold) {
                log.warn("Imagen rechazada por contenido ofensivo: score={}", offensiveScore);
                return false;
            }
        }

        // Verificar gore
        if (response.getGore() != null) {
            Double goreScore = response.getGore().get("prob");
            if (goreScore != null && goreScore > goreThreshold) {
                log.warn("Imagen rechazada por violencia gráfica: score={}", goreScore);
                return false;
            }
        }

        log.info("Imagen aprobada: todos los scores están dentro del umbral");
        return true;
    }

    /**
     * Método completo: modera y verifica la imagen
     */
    public boolean moderateAndVerify(String imageUrl) {
        try {
            SightengineResponse response = moderateImage(imageUrl);
            return isImageSafe(response);
        } catch (Exception e) {
            log.error("Error en el proceso de moderación: {}", e.getMessage());
            return false; // Por seguridad, rechazar si hay error
        }
    }
}