package com.example.blogproject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

@Service
public class SightengineService {

    @Value("${sightengine.api.user:497934805}")
    private String apiUser;

    @Value("${sightengine.api.secret:4ZWMhrMjcBoExJavMXGSkt6KAxASdwKb}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean moderateAndVerifyFile(MultipartFile file) {
        try {
            String url = "https://api.sightengine.com/1.0/check.json";

            // 1. Configuramos las cabeceras para enviar un formulario con archivo (Multipart)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 2. Convertimos el archivo subido a un recurso que RestTemplate pueda leer
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // Necesario para que la API detecte el archivo
                }
            };

            // 3. Juntamos todos los parámetros del formulario
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("media", fileResource); // El archivo va aquí

            // MODIFICACIÓN: Solicitamos tanto el modelo de desnudez como el de violencia
            body.add("models", "nudity,violence");

            body.add("api_user", apiUser);
            body.add("api_secret", apiSecret);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. Hacemos la petición POST a Sightengine
            SightengineResponse response = restTemplate.postForObject(url, requestEntity, SightengineResponse.class);

            // 5. Análisis combinado de Desnudez y Violencia
            if (response != null && "success".equals(response.getStatus())) {
                boolean isNuditySafe = true;
                boolean isViolenceSafe = true;

                // Validar bloque de Desnudez (Safe debe ser mayor al 85%)
                if (response.getNudity() != null) {
                    isNuditySafe = response.getNudity().getSafe() > 0.85;
                }

                // Validar bloque de Violencia (Armas, heridas y violencia gráfica deben ser menores al 15%)
                if (response.getViolence() != null) {
                    isViolenceSafe = response.getViolence().getWeapon() < 0.15
                            && response.getViolence().getInjury() < 0.15
                            && response.getViolence().getGraphicViolence() < 0.15;
                }

                // La imagen se aprueba únicamente si supera ambos filtros de seguridad
                return isNuditySafe && isViolenceSafe;
            }
            return false;

        } catch (Exception e) {
            System.err.println("Error al conectar con Sightengine: " + e.getMessage());
            return false;
        }
    }
}