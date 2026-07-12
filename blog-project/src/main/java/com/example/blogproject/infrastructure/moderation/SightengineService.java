package com.example.blogproject.infrastructure.moderation;

import com.example.blogproject.domain.port.ContentModerationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;


@Service
public class SightengineService implements ContentModerationPort {

    private static final Logger logger = LoggerFactory.getLogger(SightengineService.class);

    private static final String SIGHTENGINE_URL = "https://api.sightengine.com/1.0/check.json";

    private static final String MODELS = "nudity,violence";

    private static final double NUDITY_SAFE_THRESHOLD = 0.85;

    private static final double VIOLENCE_MAX_THRESHOLD = 0.15;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Value("${sightengine.api.user:}")
    private String apiUser;

    @Value("${sightengine.api.secret:}")
    private String apiSecret;

    private final RestTemplate restTemplate = buildRestTemplate();

    /** Construye un RestTemplate con timeouts para evitar peticiones colgadas. */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    @Override
    public boolean moderateAndVerifyFile(MultipartFile file) {
        try {
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

            // Solicitamos tanto el modelo de desnudez como el de violencia
            body.add("models", MODELS);

            body.add("api_user", apiUser);
            body.add("api_secret", apiSecret);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. Hacemos la petición POST a Sightengine
            SightengineResponse response =
                    restTemplate.postForObject(SIGHTENGINE_URL, requestEntity, SightengineResponse.class);

            // 5. Análisis combinado de Desnudez y Violencia
            if (response != null && "success".equals(response.getStatus())) {
                boolean isNuditySafe = true;
                boolean isViolenceSafe = true;

                // Validar bloque de Desnudez (Safe debe ser mayor al 85%)
                if (response.getNudity() != null) {
                    isNuditySafe = response.getNudity().getSafe() > NUDITY_SAFE_THRESHOLD;
                }

                // Validar bloque de Violencia (Armas, heridas y violencia gráfica deben ser menores al 15%)
                if (response.getViolence() != null) {
                    isViolenceSafe = response.getViolence().getWeapon() < VIOLENCE_MAX_THRESHOLD
                            && response.getViolence().getInjury() < VIOLENCE_MAX_THRESHOLD
                            && response.getViolence().getGraphicViolence() < VIOLENCE_MAX_THRESHOLD;
                }

                // La imagen se aprueba únicamente si supera ambos filtros de seguridad
                return isNuditySafe && isViolenceSafe;
            }
            return false;

        } catch (Exception e) {
            logger.error("Error al conectar con Sightengine: {}", e.getMessage(), e);
            return false;
        }
    }
}