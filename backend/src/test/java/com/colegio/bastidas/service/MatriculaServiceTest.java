package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.MatriculaDto;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatriculaServiceTest {

    @Autowired
    private MatriculaService matriculaService;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @MockBean
    private SupabaseStorageService supabaseStorageService;

    private Alumno alumnoTest;

    @BeforeEach
    void setUp() {
        alumnoTest = new Alumno();
        alumnoTest.setNombres("Juan");
        alumnoTest.setApellidos("Pérez");
        alumnoTest.setDni("12345678");
        alumnoTest.setCodigoEstudiante("EST-12345678");
        alumnoTest = alumnoRepository.save(alumnoTest);
    }

    @Test
    void testCrearMatriculaExito() {
        MatriculaDto dto = new MatriculaDto();
        dto.setAlumnoId(alumnoTest.getId());
        dto.setAnioEscolar(2026);
        dto.setGrado("3");
        dto.setSeccion("A");

        MatriculaDto result = matriculaService.crearMatricula(dto);

        assertNotNull(result.getId());
        assertEquals(2026, result.getAnioEscolar());
        assertEquals("3", result.getGrado());
        
        assertTrue(matriculaRepository.findById(result.getId()).isPresent());
    }

    @Test
    void testCrearMatriculaDuplicadaDebeFallar() {
        MatriculaDto dto = new MatriculaDto();
        dto.setAlumnoId(alumnoTest.getId());
        dto.setAnioEscolar(2026);
        dto.setGrado("3");
        dto.setSeccion("A");

        // Primera matrícula exitosa
        matriculaService.crearMatricula(dto);

        // Segunda matrícula para el mismo año debe lanzar excepción
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            matriculaService.crearMatricula(dto);
        });

        assertTrue(exception.getMessage().contains("Ya existe una matrícula"));
    }

    @Test
    void testListarPorAnio() {
        MatriculaDto dto = new MatriculaDto();
        dto.setAlumnoId(alumnoTest.getId());
        dto.setAnioEscolar(2026);
        dto.setGrado("3");
        dto.setSeccion("A");
        matriculaService.crearMatricula(dto);

        List<MatriculaDto> matriculas = matriculaService.listarPorAnio(2026);
        assertFalse(matriculas.isEmpty());
        assertEquals(1, matriculas.size());
        assertEquals("12345678", matriculas.get(0).getAlumnoDni());
    }
}
