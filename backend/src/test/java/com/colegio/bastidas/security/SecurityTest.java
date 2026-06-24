package com.colegio.bastidas.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAccesoSinAutenticacionDebeRetornar401() throws Exception {
        mockMvc.perform(get("/api/v1/asistencia/resumen/1"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "docente1", roles = {"DOCENTE"})
    void testAccesoDocenteAEndpointPermitido() throws Exception {
        // Suponiendo que /api/v1/asistencia/aula/1 requiere rol DOCENTE o ADMIN
        mockMvc.perform(get("/api/v1/asistencia/aula/1?fecha=2026-10-10"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alumno1", roles = {"ALUMNO"})
    void testAccesoAlumnoAEndpointDeDocenteDebeRetornar403() throws Exception {
        // Un alumno no debería poder listar las asistencias de toda un aula
        mockMvc.perform(get("/api/v1/asistencia/aula/1?fecha=2026-10-10"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testAccesoAdminAMatriculasPermitido() throws Exception {
        mockMvc.perform(get("/api/v1/matriculas/anio/2026"))
               .andExpect(status().isOk());
    }
}
