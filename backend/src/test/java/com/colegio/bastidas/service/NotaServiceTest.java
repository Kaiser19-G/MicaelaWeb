package com.colegio.bastidas.service;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Nota;
import com.colegio.bastidas.repository.NotaRepository;
import com.colegio.bastidas.service.impl.NotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private NotaServiceImpl notaService;

    private Nota notaTest;
    private Alumno alumnoTest;

    @BeforeEach
    void setUp() {
        alumnoTest = new Alumno();
        alumnoTest.setId(1L);

        notaTest = new Nota();
        notaTest.setId(10L);
        notaTest.setAlumno(alumnoTest);
        notaTest.setValorNota(15.0);
        notaTest.setPeriodo("BIMESTRE_1");
    }

    @Test
    void testRegistrarNota() {
        when(notaRepository.save(any(Nota.class))).thenReturn(notaTest);

        Nota resultado = notaService.registrarNota(notaTest);

        assertNotNull(resultado);
        assertEquals(15.0, resultado.getValorNota());
        verify(notaRepository, times(1)).save(notaTest);
    }

    @Test
    void testAdjuntarEvidencia() {
        MultipartFile archivoMock = mock(MultipartFile.class);
        when(archivoMock.getOriginalFilename()).thenReturn("examen.jpg");
        when(notaRepository.findById(10L)).thenReturn(Optional.of(notaTest));
        when(storageService.subirArchivo(eq(archivoMock), anyString())).thenReturn("https://supabase.co/storage/v1/object/public/evidencias/nota_10.jpg");
        when(notaRepository.save(any(Nota.class))).thenReturn(notaTest);

        String url = notaService.adjuntarEvidencia(10L, archivoMock, "Examen Parcial");

        assertNotNull(url);
        assertTrue(url.contains("supabase.co"));
        verify(storageService, times(1)).subirArchivo(eq(archivoMock), anyString());
        verify(notaRepository, times(1)).save(notaTest);
    }

    @Test
    void testCalcularPromedioFinal() {
        Nota n1 = new Nota();
        n1.setValorNota(12.0);
        Nota n2 = new Nota();
        n2.setValorNota(16.0);

        when(notaRepository.findByAlumnoIdAndPeriodoAndAnio(1L, "BIMESTRE_1", 2026))
            .thenReturn(List.of(n1, n2));

        Double promedio = notaService.calcularPromedioFinal(1L, "BIMESTRE_1", 2026);

        assertNotNull(promedio);
        assertEquals(14.0, promedio);
    }
}
