package com.example.blogproject.Integracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUploadsResourceHandlerIsConfigured() throws Exception {
        // Intenta acceder a una ruta de /uploads/
        // Esperamos un 404 (Not Found) en lugar de un 403 (Forbidden) o 500 (Server Error),
        // lo que confirma que el ResourceHandler intercepta y resuelve la ruta correctamente.
        mockMvc.perform(get("/uploads/non-existent-file.png"))
                .andExpect(status().isNotFound());
    }
}
