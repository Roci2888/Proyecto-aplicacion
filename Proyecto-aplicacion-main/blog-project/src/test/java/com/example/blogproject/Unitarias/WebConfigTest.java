package com.example.blogproject.Unitarias;
import com.example.blogproject.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void testAddResourceHandlers() {
        WebConfig webConfig = new WebConfig("custom-uploads");
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);
        when(registration.addResourceLocations(any(String[].class))).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry, times(1)).addResourceHandler("/uploads/**");

        // Capturamos las rutas pasadas a addResourceLocations para asegurar que terminen con "/"
        ArgumentCaptor<String> locationsCaptor = ArgumentCaptor.forClass(String.class);
        verify(registration).addResourceLocations(locationsCaptor.capture(), locationsCaptor.capture());

        String customLocation = locationsCaptor.getAllValues().get(0);
        assertTrue(customLocation.endsWith("/"), "La ruta personalizada debe finalizar con /");
    }
}
