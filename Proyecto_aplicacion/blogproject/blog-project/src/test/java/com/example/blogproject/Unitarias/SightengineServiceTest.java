package com.example.blogproject.Unitarias;

import com.example.blogproject.ModerationResult;
import com.example.blogproject.SightengineResponse;
import com.example.blogproject.SightengineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita la integración de Mockito con JUnit 5
class SightengineServiceTest {

    @Mock // Crea un mock del cliente RestTemplate
    private RestTemplate restTemplate;

    @InjectMocks // Inyecta los mocks en el servicio real
    private SightengineService sightengineService;

    private MockMultipartFile safeFile; // Archivo de imagen válido para pruebas
    private MockMultipartFile invalidFile; // Archivo no válido (texto)
    private MockMultipartFile emptyFile; // Archivo de imagen vacío

    @BeforeEach // Se ejecuta antes de cada prueba
    void setUp() {
        // Inyecta el mock de RestTemplate
        ReflectionTestUtils.setField(sightengineService, "restTemplate", restTemplate);

        // Configura credenciales de la API de prueba
        ReflectionTestUtils.setField(sightengineService, "apiUser", "test_user");
        ReflectionTestUtils.setField(sightengineService, "apiSecret", "test_secret");

        // Configura umbrales de prueba (@Value) para evitar falsos positivos
        ReflectionTestUtils.setField(sightengineService, "nuditySexualActivityThreshold", 0.05);
        ReflectionTestUtils.setField(sightengineService, "nuditySexualDisplayThreshold", 0.10);
        ReflectionTestUtils.setField(sightengineService, "nudityEroticaThreshold", 0.20);
        ReflectionTestUtils.setField(sightengineService, "nudityVerySuggestiveThreshold", 0.90);

        ReflectionTestUtils.setField(sightengineService, "violenceGlobalThreshold", 0.20);
        ReflectionTestUtils.setField(sightengineService, "violencePhysicalThreshold", 0.15);
        ReflectionTestUtils.setField(sightengineService, "violenceFirearmThreshold", 0.10);

        ReflectionTestUtils.setField(sightengineService, "recreationalDrugThreshold", 0.10);
        ReflectionTestUtils.setField(sightengineService, "medicalDrugThreshold", 0.85);

        // Configura los archivos de prueba
        safeFile = new MockMultipartFile(
                "media",
                "test.jpg",
                "image/jpeg",
                "datos-de-imagen".getBytes()
        );

        invalidFile = new MockMultipartFile(
                "media",
                "test.txt",
                "text/plain",
                "texto".getBytes()
        );

        emptyFile = new MockMultipartFile(
                "media",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );
    }

    // ==================== TESTS PARA moderateAndVerifyFile() ====================
    // Pruebas para la verificación y moderación de archivos de imagen

    /**
     * Prueba que una imagen con armas de fuego sea rechazada por violencia
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenImageContainsViolence() {
        // Given: Configuración de respuesta con violencia
        SightengineResponse mockResponse = createViolenceResponse();

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(SightengineResponse.class)
        )).thenReturn(mockResponse);

        // When: Ejecuta la moderación
        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        // Then: Verifica el rechazo por armas de fuego
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("VIOLENCE_FIREARM_THREAT", result.getReasonCode());
    }

    /**
     * Prueba que una imagen con contenido explícito sea rechazada por desnudez
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenImageContainsNudity() {
        // Given: Configuración de respuesta con desnudez/actividad sexual
        SightengineResponse mockResponse = createNudityResponse();

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(SightengineResponse.class)
        )).thenReturn(mockResponse);

        // When: Ejecuta la moderación
        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        // Then: Verifica el rechazo por actividad sexual
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("NUDITY_SEXUAL_ACTIVITY", result.getReasonCode());
    }

    /**
     * Prueba que una imagen con sustancias sea rechazada por drogas recreativas
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenImageContainsDrugs() {
        // Given: Configuración de respuesta con contenido de drogas
        SightengineResponse mockResponse = createDrugsResponse();

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(SightengineResponse.class)
        )).thenReturn(mockResponse);

        // When: Ejecuta la moderación
        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        // Then: Verifica el rechazo por drogas recreativas
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("DRUGS_RECREATIONAL", result.getReasonCode());
    }

    /**
     * Prueba que se rechace el archivo inmediatamente si el tipo de contenido no es una imagen
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenInvalidFileType() {
        // When: Intenta moderar un archivo que no es imagen
        ModerationResult result = sightengineService.moderateAndVerifyFile(invalidFile);

        // Then: Se rechaza sin llamar al servicio externo
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("INVALID_FILE_TYPE", result.getReasonCode());

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    /**
     * Prueba la respuesta de error cuando la API de Sightengine retorna un estado nulo
     */
    @Test
    void moderateAndVerifyFile_ShouldHandleServiceError_WhenApiReturnsNull() {
        // Given: La API retorna nulo
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(SightengineResponse.class)
        )).thenReturn(null);

        // When: Ejecuta la moderación
        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        // Then: Debe retornar error del servicio de moderación
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("MODERATION_SERVICE_ERROR", result.getReasonCode());
    }

    /**
     * Prueba el manejo de excepciones cuando la conexión con la API falla
     */
    @Test
    void moderateAndVerifyFile_ShouldHandleException_WhenApiFails() {
        // Given: La API lanza una excepción por tiempo de espera
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(SightengineResponse.class)
        )).thenThrow(new RuntimeException("Connection timeout"));

        // When: Ejecuta la moderación
        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        // Then: Captura la excepción y la convierte en resultado rechazado
        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("MODERATION_EXCEPTION", result.getReasonCode());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private SightengineResponse createSafeResponse() {
        SightengineResponse response = new SightengineResponse();
        response.setStatus("success");

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSexualActivity(0.01);
        nudity.setSexualDisplay(0.01);
        nudity.setErotica(0.01);
        nudity.setVerySuggestive(0.01);
        nudity.setSuggestive(0.10);
        nudity.setNone(0.85);
        response.setNudity(nudity);

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setProb(0.01);
        SightengineResponse.Violence.Classes classes = new SightengineResponse.Violence.Classes();
        classes.setPhysicalViolence(0.01);
        classes.setFirearmThreat(0.01);
        classes.setCombatSport(0.01);
        violence.setClasses(classes);
        response.setViolence(violence);

        SightengineResponse.RecreationalDrug recDrug = new SightengineResponse.RecreationalDrug();
        recDrug.setProb(0.01);
        SightengineResponse.RecreationalDrug.Classes recClasses = new SightengineResponse.RecreationalDrug.Classes();
        recClasses.setCannabis(0.01);
        recClasses.setCannabisDrug(0.01);
        recClasses.setRecreationalDrugsNotCannabis(0.01);
        recDrug.setClasses(recClasses);
        response.setRecreationalDrug(recDrug);

        SightengineResponse.Medical medical = new SightengineResponse.Medical();
        medical.setProb(0.01);
        SightengineResponse.Medical.Classes medClasses = new SightengineResponse.Medical.Classes();
        medClasses.setPills(0.01);
        medClasses.setParaphernalia(0.01);
        medical.setClasses(medClasses);
        response.setMedical(medical);

        return response;
    }

    private SightengineResponse createViolenceResponse() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setProb(0.75);
        SightengineResponse.Violence.Classes classes = new SightengineResponse.Violence.Classes();
        classes.setPhysicalViolence(0.01);
        classes.setFirearmThreat(0.75);
        classes.setCombatSport(0.01);
        violence.setClasses(classes);

        response.setViolence(violence);
        return response;
    }

    private SightengineResponse createNudityResponse() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSexualActivity(0.85);
        nudity.setSexualDisplay(0.01);
        nudity.setErotica(0.01);
        nudity.setVerySuggestive(0.01);

        response.setNudity(nudity);
        return response;
    }

    private SightengineResponse createDrugsResponse() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.RecreationalDrug recDrug = new SightengineResponse.RecreationalDrug();
        recDrug.setProb(0.85);
        SightengineResponse.RecreationalDrug.Classes recClasses = new SightengineResponse.RecreationalDrug.Classes();
        recClasses.setCannabis(0.85);
        recClasses.setCannabisDrug(0.85);
        recClasses.setRecreationalDrugsNotCannabis(0.01);
        recDrug.setClasses(recClasses);

        response.setRecreationalDrug(recDrug);
        return response;
    }
    // ============================================================
// NUEVOS TESTS PARA AGREGAR A SightengineServiceTest
// (pega estos métodos y helpers dentro de la clase existente)
// ============================================================

    /**
     * Prueba que un archivo vacío (isEmpty()) se permita sin llamar a la API
     * (usa el campo emptyFile que ya estaba declarado pero sin usar)
     */
    @Test
    void moderateAndVerifyFile_ShouldAllow_WhenFileIsEmpty() {
        ModerationResult result = sightengineService.moderateAndVerifyFile(emptyFile);

        Assertions.assertTrue(result.isAllowed());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    /**
     * Prueba el rechazo por sustancias/material médico sensible (DRUGS_MEDICAL)
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenImageContainsMedicalDrugs() {
        SightengineResponse mockResponse = createMedicalDrugsResponse();

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(mockResponse);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("DRUGS_MEDICAL", result.getReasonCode());
    }

    /**
     * Prueba el rechazo aislado por violencia física explícita
     * (sin que se dispare primero el chequeo de arma de fuego)
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenImageContainsPhysicalViolenceOnly() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setProb(0.30);
        SightengineResponse.Violence.Classes classes = new SightengineResponse.Violence.Classes();
        classes.setPhysicalViolence(0.75); // por encima del umbral 0.15
        classes.setFirearmThreat(0.01);    // por debajo del umbral 0.10
        classes.setCombatSport(0.01);
        violence.setClasses(classes);
        response.setViolence(violence);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("VIOLENCE_PHYSICAL", result.getReasonCode());
    }

    /**
     * Prueba que una imagen de deporte de combate (combat_sport alto,
     * physical y firearm bajos) NO sea rechazada aunque el prob global sea alto
     */
    @Test
    void moderateAndVerifyFile_ShouldAllow_WhenImageLooksLikeCombatSportOnly() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setProb(0.85); // alto, pero...
        SightengineResponse.Violence.Classes classes = new SightengineResponse.Violence.Classes();
        classes.setPhysicalViolence(0.05);
        classes.setFirearmThreat(0.01);
        classes.setCombatSport(0.90); // ...se explica por deporte de combate
        violence.setClasses(classes);
        response.setViolence(violence);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertTrue(result.isAllowed());
    }

    /**
     * Prueba que violencia general (global) SÍ se rechace cuando NO
     * parece deporte de combate
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenGeneralViolenceIsHighAndNotCombatSport() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setProb(0.85);
        SightengineResponse.Violence.Classes classes = new SightengineResponse.Violence.Classes();
        classes.setPhysicalViolence(0.05);
        classes.setFirearmThreat(0.01);
        classes.setCombatSport(0.10); // bajo -> no es deporte de combate
        violence.setClasses(classes);
        response.setViolence(violence);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("VIOLENCE_GENERAL", result.getReasonCode());
    }

    /**
     * Prueba que si el bloque "nudity" viene ausente (null) se rechace
     * con NUDITY_ANALYSIS_MISSING
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenNudityBlockIsMissing() {
        SightengineResponse response = createSafeResponse();
        response.setNudity(null);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("NUDITY_ANALYSIS_MISSING", result.getReasonCode());
    }

    /**
     * Prueba que si el bloque "violence" viene ausente (null) se rechace
     * con VIOLENCE_ANALYSIS_MISSING
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenViolenceBlockIsMissing() {
        SightengineResponse response = createSafeResponse();
        response.setViolence(null);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("VIOLENCE_ANALYSIS_MISSING", result.getReasonCode());
    }

    /**
     * Prueba el rechazo por sexual_display (nudez explícita, distinto
     * de sexual_activity que ya estaba cubierto)
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenSexualDisplayExceedsThreshold() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSexualActivity(0.01);
        nudity.setSexualDisplay(0.50); // por encima del umbral 0.10
        nudity.setErotica(0.01);
        nudity.setVerySuggestive(0.01);
        response.setNudity(nudity);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("NUDITY_SEXUAL_DISPLAY", result.getReasonCode());
    }

    /**
     * Prueba el rechazo por contenido erótico (erotica)
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenEroticaExceedsThreshold() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSexualActivity(0.01);
        nudity.setSexualDisplay(0.01);
        nudity.setErotica(0.40); // por encima del umbral 0.20
        nudity.setVerySuggestive(0.01);
        response.setNudity(nudity);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("NUDITY_EROTICA", result.getReasonCode());
    }

    /**
     * Prueba el rechazo por contenido "very_suggestive"
     */
    @Test
    void moderateAndVerifyFile_ShouldReject_WhenVerySuggestiveExceedsThreshold() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSexualActivity(0.01);
        nudity.setSexualDisplay(0.01);
        nudity.setErotica(0.01);
        nudity.setVerySuggestive(0.95); // por encima del umbral 0.90
        response.setNudity(nudity);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(SightengineResponse.class)))
                .thenReturn(response);

        ModerationResult result = sightengineService.moderateAndVerifyFile(safeFile);

        Assertions.assertFalse(result.isAllowed());
        Assertions.assertEquals("NUDITY_VERY_SUGGESTIVE", result.getReasonCode());
    }

// ==================== HELPER NUEVO ====================

    private SightengineResponse createMedicalDrugsResponse() {
        SightengineResponse response = createSafeResponse();

        SightengineResponse.Medical medical = new SightengineResponse.Medical();
        medical.setProb(0.90);
        SightengineResponse.Medical.Classes medClasses = new SightengineResponse.Medical.Classes();
        medClasses.setPills(0.90);
        medClasses.setParaphernalia(0.10);
        medical.setClasses(medClasses);

        response.setMedical(medical);
        return response;
    }
}