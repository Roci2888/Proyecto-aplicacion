package com.example.blogproject.Unitarias;

import com.example.blogproject.SightengineResponse;
import com.example.blogproject.SightengineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SightengineServiceTest {

    @InjectMocks
    private SightengineService sightengineService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Inyecta el restTemplate mockeado dentro de tu servicio antes de correr el test
        org.springframework.test.util.ReflectionTestUtils.setField(
                sightengineService, "restTemplate", restTemplate
        );
    }
    @Test
    void testModerateAndVerifyFile_SafeImage() {
        // Mock de la respuesta de Sightengine
        SightengineResponse mockResponse = new SightengineResponse();
        mockResponse.setStatus("success");

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSafe(0.95); // Seguro (> 0.85)
        mockResponse.setNudity(nudity);

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setWeapon(0.02); // Seguro (< 0.15)
        violence.setInjury(0.01);
        violence.setGraphicViolence(0.00);
        mockResponse.setViolence(violence);

        when(restTemplate.postForObject(
                anyString(),
                any(),
                any(Class.class) // <-- Cambia eq(...) por any(Class.class)
        )).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile("media", "test.jpg", "image/jpeg", "data".getBytes());
        boolean result = sightengineService.moderateAndVerifyFile(file);

        org.junit.jupiter.api.Assertions.assertTrue(result);
    }

    @Test
    void testModerateAndVerifyFile_UnsafeViolence() {
        SightengineResponse mockResponse = new SightengineResponse();
        mockResponse.setStatus("success");

        SightengineResponse.Nudity nudity = new SightengineResponse.Nudity();
        nudity.setSafe(0.90);
        mockResponse.setNudity(nudity);

        SightengineResponse.Violence violence = new SightengineResponse.Violence();
        violence.setWeapon(0.75); // 🚨 ALERTA: Contiene armas!
        violence.setInjury(0.01);
        violence.setGraphicViolence(0.00);
        mockResponse.setViolence(violence);

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),          // Forzamos a que detecte el cuerpo como HttpEntity
                (Class<SightengineResponse>) any(Class.class) // Casteo explícito del tipo de respuesta
        )).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile("media", "weapon.jpg", "image/jpeg", "data".getBytes());
        boolean result = sightengineService.moderateAndVerifyFile(file);

        org.junit.jupiter.api.Assertions.assertFalse(result);
    }
}