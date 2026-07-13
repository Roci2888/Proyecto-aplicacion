package com.example.blogproject.infrastructure.moderation;

import com.example.blogproject.domain.model.ModerationResult;
import com.example.blogproject.domain.port.ContentModerationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SightengineService implements ContentModerationPort {

    private static final Logger logger = LoggerFactory.getLogger(SightengineService.class);

    private static final String SIGHTENGINE_URL = "https://api.sightengine.com/1.0/check.json";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Value("${sightengine.api.user}")
    private String apiUser;

    @Value("${sightengine.api.secret}")
    private String apiSecret;

    @Value("${moderation.threshold.nudity.sexual_activity:0.05}")
    private double nuditySexualActivityThreshold;

    @Value("${moderation.threshold.nudity.sexual_display:0.10}")
    private double nuditySexualDisplayThreshold;

    @Value("${moderation.threshold.nudity.erotica:0.20}")
    private double nudityEroticaThreshold;

    @Value("${moderation.threshold.nudity.very_suggestive:0.90}")
    private double nudityVerySuggestiveThreshold;

    @Value("${moderation.threshold.violence.global:0.20}")
    private double violenceGlobalThreshold;

    @Value("${moderation.threshold.violence.physical:0.15}")
    private double violencePhysicalThreshold;

    @Value("${moderation.threshold.violence.firearm:0.10}")
    private double violenceFirearmThreshold;

    @Value("${moderation.threshold.drugs.recreational:0.10}")
    private double recreationalDrugThreshold;

    @Value("${moderation.threshold.drugs.medical:0.85}")
    private double medicalDrugThreshold;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    @Override
    public ModerationResult moderateAndVerifyFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ModerationResult.allow();
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                logger.warn("Archivo rechazado: contentType no válido -> {}", file.getContentType());
                return ModerationResult.reject(
                        "INVALID_FILE_TYPE",
                        "El archivo subido no es una imagen válida.",
                        "El contentType del archivo no empieza por image/."
                );
            }

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
            body.add("models", "nudity-2.1,violence,recreational_drug,medical");
            body.add("api_user", apiUser);
            body.add("api_secret", apiSecret);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            SightengineResponse response =
                    restTemplate.postForObject(SIGHTENGINE_URL, requestEntity, SightengineResponse.class);

            if (response == null || !"success".equalsIgnoreCase(response.getStatus())) {
                logger.warn("Respuesta inválida de Sightengine. status={}",
                        response != null ? response.getStatus() : "null");

                return ModerationResult.reject(
                        "MODERATION_SERVICE_ERROR",
                        "No se pudo verificar la imagen correctamente. Inténtalo de nuevo.",
                        "Sightengine devolvió una respuesta nula o con estado distinto de success."
                );
            }

            ModerationResult nudityResult = validateNudity(response.getNudity());
            if (!nudityResult.isAllowed()) {
                return nudityResult;
            }

            ModerationResult violenceResult = validateViolence(response.getViolence());
            if (!violenceResult.isAllowed()) {
                return violenceResult;
            }

            ModerationResult drugsResult = validateDrugs(
                    response.getRecreationalDrug(),
                    response.getMedical()
            );
            if (!drugsResult.isAllowed()) {
                return drugsResult;
            }

            return ModerationResult.allow();

        } catch (Exception e) {
            logger.error("Error moderando imagen con Sightengine", e);
            return ModerationResult.reject(
                    "MODERATION_EXCEPTION",
                    "Se produjo un error al analizar la imagen.",
                    e.getMessage()
            );
        }
    }

    private ModerationResult validateNudity(SightengineResponse.Nudity nudity) {
        if (nudity == null) {
            logger.warn("Bloque nudity ausente.");
            return ModerationResult.reject(
                    "NUDITY_ANALYSIS_MISSING",
                    "No se pudo analizar correctamente el contenido visual de la imagen.",
                    "La respuesta no contiene el bloque nudity."
            );
        }

        double sexualActivity = n(nudity.getSexualActivity());
        double sexualDisplay = n(nudity.getSexualDisplay());
        double erotica = n(nudity.getErotica());
        double verySuggestive = n(nudity.getVerySuggestive());
        double suggestive = n(nudity.getSuggestive());
        double none = n(nudity.getNone());

        logger.info("nudity -> sexual_activity={}, sexual_display={}, erotica={}, very_suggestive={}, suggestive={}, none={}",
                sexualActivity, sexualDisplay, erotica, verySuggestive, suggestive, none);

        if (sexualActivity >= nuditySexualActivityThreshold) {
            return ModerationResult.reject(
                    "NUDITY_SEXUAL_ACTIVITY",
                    "La imagen ha sido rechazada por contener actividad sexual explícita.",
                    "sexual_activity=" + sexualActivity
            );
        }

        if (sexualDisplay >= nuditySexualDisplayThreshold) {
            return ModerationResult.reject(
                    "NUDITY_SEXUAL_DISPLAY",
                    "La imagen ha sido rechazada por contener desnudez sexual explícita.",
                    "sexual_display=" + sexualDisplay
            );
        }

        if (erotica >= nudityEroticaThreshold) {
            return ModerationResult.reject(
                    "NUDITY_EROTICA",
                    "La imagen ha sido rechazada por contenido erótico.",
                    "erotica=" + erotica
            );
        }

        if (verySuggestive >= nudityVerySuggestiveThreshold) {
            return ModerationResult.reject(
                    "NUDITY_VERY_SUGGESTIVE",
                    "La imagen ha sido rechazada por contenido demasiado sugerente.",
                    "very_suggestive=" + verySuggestive
            );
        }

        return ModerationResult.allow();
    }

    private ModerationResult validateViolence(SightengineResponse.Violence violence) {
        if (violence == null) {
            logger.warn("Bloque violence ausente.");
            return ModerationResult.reject(
                    "VIOLENCE_ANALYSIS_MISSING",
                    "No se pudo analizar correctamente la violencia en la imagen.",
                    "La respuesta no contiene el bloque violence."
            );
        }

        double global = n(violence.getProb());
        double physical = 0.0;
        double firearm = 0.0;
        double combatSport = 0.0;

        if (violence.getClasses() != null) {
            physical = n(violence.getClasses().getPhysicalViolence());
            firearm = n(violence.getClasses().getFirearmThreat());
            combatSport = n(violence.getClasses().getCombatSport());
        }

        logger.info("VIOLENCE SCORES -> global={}, physical={}, firearm={}, combat_sport={}",
                global, physical, firearm, combatSport);

        // ✅ DETECCIÓN DE VIOLENCIA FÍSICA EXPLÍCITA
        if (physical >= violencePhysicalThreshold) {
            logger.warn("VIOLENCIA FÍSICA DETECTADA: {}", physical);
            return ModerationResult.reject(
                    "VIOLENCE_PHYSICAL",
                    "La imagen ha sido rechazada por contener violencia física explícita.",
                    "physical_violence=" + physical
            );
        }

        // DETECCIÓN DE ARMAS DE FUEGO
        if (firearm >= violenceFirearmThreshold) {
            logger.warn("ARMA DE FUEGO DETECTADA: {}", firearm);
            return ModerationResult.reject(
                    "VIOLENCE_FIREARM_THREAT",
                    "La imagen ha sido rechazada por contener amenaza con arma de fuego.",
                    "firearm_threat=" + firearm
            );
        }

        // VIOLENCIA GENERAL (excluyendo deportes de combate)
        boolean looksLikeCombatSportOnly =
                combatSport >= 0.80 &&
                        physical < violencePhysicalThreshold &&
                        firearm < violenceFirearmThreshold;

        logger.info("  - looksLikeCombatSportOnly: {}", looksLikeCombatSportOnly);

        if (!looksLikeCombatSportOnly && global >= violenceGlobalThreshold) {
            logger.warn("VIOLENCIA GENERAL DETECTADA: {}", global);
            return ModerationResult.reject(
                    "VIOLENCE_GENERAL",
                    "La imagen ha sido rechazada por contenido violento.",
                    "violence.prob=" + global
            );
        }

        logger.info("VIOLENCIA APROBADA");
        return ModerationResult.allow();
    }

    private ModerationResult validateDrugs(SightengineResponse.RecreationalDrug recreationalDrug,
                                           SightengineResponse.Medical medical) {

        if (recreationalDrug != null) {
            double recProb = n(recreationalDrug.getProb());
            double cannabis = 0.0;
            double cannabisDrug = 0.0;
            double otherDrugs = 0.0;

            if (recreationalDrug.getClasses() != null) {
                cannabis = n(recreationalDrug.getClasses().getCannabis());
                cannabisDrug = n(recreationalDrug.getClasses().getCannabisDrug());
                otherDrugs = n(recreationalDrug.getClasses().getRecreationalDrugsNotCannabis());
            }

            logger.info("recreational_drug -> prob={}, cannabis={}, cannabis_drug={}, recreational_drugs_not_cannabis={}",
                    recProb, cannabis, cannabisDrug, otherDrugs);

            if (recProb >= recreationalDrugThreshold
                    || cannabisDrug >= recreationalDrugThreshold
                    || otherDrugs >= recreationalDrugThreshold) {

                return ModerationResult.reject(
                        "DRUGS_RECREATIONAL",
                        "La imagen ha sido rechazada por mostrar drogas recreativas.",
                        "recreational_drug.prob=" + recProb +
                                ", cannabis_drug=" + cannabisDrug +
                                ", other_drugs=" + otherDrugs
                );
            }
        }

        if (medical != null) {
            double medicalProb = n(medical.getProb());
            double pills = 0.0;
            double paraphernalia = 0.0;

            if (medical.getClasses() != null) {
                pills = n(medical.getClasses().getPills());
                paraphernalia = n(medical.getClasses().getParaphernalia());
            }

            logger.info("medical -> prob={}, pills={}, paraphernalia={}",
                    medicalProb, pills, paraphernalia);

            if (medicalProb >= medicalDrugThreshold
                    || pills >= medicalDrugThreshold
                    || paraphernalia >= medicalDrugThreshold) {

                return ModerationResult.reject(
                        "DRUGS_MEDICAL",
                        "La imagen ha sido rechazada por mostrar sustancias o material médico sensible.",
                        "medical.prob=" + medicalProb +
                                ", pills=" + pills +
                                ", paraphernalia=" + paraphernalia
                );
            }
        }

        return ModerationResult.allow();
    }

    private double n(Double value) {
        return value != null ? value : 0.0;
    }
}
