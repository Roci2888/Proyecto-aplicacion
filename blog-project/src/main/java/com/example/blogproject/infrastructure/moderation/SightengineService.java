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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;


@Service
public class SightengineService implements ContentModerationPort {

    private static final Logger logger = LoggerFactory.getLogger(SightengineService.class);

    private static final String SIGHTENGINE_URL = "https://api.sightengine.com/1.0/check.json";

    // Modelos: nudity (desnudez), wad (armas/alcohol/drogas), gore (sangre/violencia gráfica)
    private static final String MODELS = "nudity,wad,gore";

    /** La desnudez es segura si "safe" supera este umbral. */
    private static final double NUDITY_SAFE_THRESHOLD = 0.85;

    /** Umbral máximo tolerado para armas, drogas/alcohol y contenido gráfico. */
    private static final double WEAPON_MAX_THRESHOLD = 0.15;
    private static final double DRUGS_ALCOHOL_MAX_THRESHOLD = 0.30;
    private static final double GORE_MAX_THRESHOLD = 0.15;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Value("${sightengine.api.user:}")
    private String apiUser;

    @Value("${sightengine.api.secret:}")
    private String apiSecret;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    @Override
    public boolean moderateAndVerifyFile(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("media", fileResource);
            body.add("models", MODELS);
            body.add("api_user", apiUser);
            body.add("api_secret", apiSecret);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            SightengineResponse response =
                    restTemplate.postForObject(SIGHTENGINE_URL, requestEntity, SightengineResponse.class);

            return isContentSafe(response);

        } catch (Exception e) {
            logger.error("Error al conectar con Sightengine (archivo): {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean moderateAndVerify(String imageUrl) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(SIGHTENGINE_URL)
                    .queryParam("url", imageUrl)
                    .queryParam("models", MODELS)
                    .queryParam("api_user", apiUser)
                    .queryParam("api_secret", apiSecret)
                    .encode()
                    .toUriString();

            SightengineResponse response = restTemplate.getForObject(uri, SightengineResponse.class);

            return isContentSafe(response);

        } catch (Exception e) {
            logger.error("Error al conectar con Sightengine (URL): {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean isContentSafe(SightengineResponse response) {
        if (response == null || !"success".equals(response.getStatus())) {
            return false;
        }

        boolean isNuditySafe = true;
        if (response.getNudity() != null && response.getNudity().getSafe() != null) {
            isNuditySafe = response.getNudity().getSafe() > NUDITY_SAFE_THRESHOLD;
        }

        boolean isWeaponSafe = isBelowThreshold(response.getWeapon(), WEAPON_MAX_THRESHOLD);
        boolean isDrugsSafe = isBelowThreshold(response.getDrugs(), DRUGS_ALCOHOL_MAX_THRESHOLD);
        boolean isAlcoholSafe = isBelowThreshold(response.getAlcohol(), DRUGS_ALCOHOL_MAX_THRESHOLD);

        boolean isGoreSafe = response.getGore() == null
                || isBelowThreshold(response.getGore().getProb(), GORE_MAX_THRESHOLD);

        return isNuditySafe && isWeaponSafe && isDrugsSafe && isAlcoholSafe && isGoreSafe;
    }

    /** Un indicador es seguro si no fue reportado (null) o si está por debajo del umbral. */
    private boolean isBelowThreshold(Double value, double threshold) {
        return value == null || value < threshold;
    }
}