package com.colegio.bastidas.service;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.service.impl.AsistenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private AsistenciaServiceImpl asistenciaService;

    private Alumno alumnoTest;

    @BeforeEach
    void setUp() {
        alumnoTest = new Alumno();
        alumnoTest.setId(1L);
        alumnoTest.setNombres("Maria");
        alumnoTest.setApellidoPaterno("Lopez");
    }

    @Test
    void testRegistrarAsistenciaNormal() {
        LocalDate hoy = LocalDate.now();
        Asistencia asistenciaMock = new Asistencia();
        asistenciaMock.setAlumno(alumnoTest);
        asistenciaMock.setFecha(hoy);
        asistenciaMock.setEstado(Asistencia.EstadoAsistencia.ASISTIO);

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumnoTest));
        when(asistenciaRepository.save(any(Asistencia.class))).thenReturn(asistenciaMock);

        Asistencia resultado = asistenciaService.registrarAsistencia(1L, hoy, Asistencia.EstadoAsistencia.ASISTIO, 10L);

        assertNotNull(resultado);
        assertEquals(Asistencia.EstadoAsistencia.ASISTIO, resultado.getEstado());
        verify(asistenciaRepository, times(1)).save(any(Asistencia.class));
    }

    @Test
    void testRegistrarAsistenciaLote() {
        LocalDate hoy = LocalDate.now();
        AsistenciaService.RegistroAsistenciaDto reg1 = new AsistenciaService.RegistroAsistenciaDto(1L, Asistencia.EstadoAsistencia.ASISTIO, null);
        AsistenciaService.RegistroAsistenciaDto reg2 = new AsistenciaService.RegistroAsistenciaDto(2L, Asistencia.EstadoAsistencia.FALTA, null);

        Alumno alumno2 = new Alumno();
        alumno2.setId(2L);

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumnoTest));
        when(alumnoRepository.findById(2L)).thenReturn(Optional.of(alumno2));
        
        when(asistenciaRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<Asistencia> resultados = asistenciaService.registrarAsistenciaLote(1L, hoy, List.of(reg1, reg2), 10L);

        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        assertEquals(Asistencia.EstadoAsistencia.ASISTIO, resultados.get(0).getEstado());
        assertEquals(Asistencia.EstadoAsistencia.FALTA, resultados.get(1).getEstado());
        verify(asistenciaRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testSincronizarRegistrosOffline() {
        Asistencia a1 = new Asistencia();
        a1.setAlumno(alumnoTest);
        a1.setFecha(LocalDate.now());
        a1.setEstado(Asistencia.EstadoAsistencia.TARDANZA);

        when(asistenciaRepository.findByAlumnoIdAndFecha(1L, LocalDate.now())).thenReturn(Optional.empty());
        when(asistenciaRepository.saveAll(anyList())).thenReturn(List.of(a1));

        int sincronizados = asistenciaService.sincronizarRegistrosOffline(List.of(a1));

        assertEquals(1, sincronizados);
        verify(asistenciaRepository, times(1)).saveAll(anyList());
    }
}
