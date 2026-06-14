package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.aula.AulaResponseDTO;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.service.AulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de Aulas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AulaServiceImpl implements AulaService {

    // Repositorio propio de Aulas se añadirá cuando sea creado.
    // Por ahora usamos AlumnoRepository para obtener datos de aulas vía relación.
    private final AlumnoRepository alumnoRepository;

    @Override
    public List<AulaResponseDTO> listarPorAnio(Integer anio) {
        // Se implementará con AulaRepository en Sprint 3
        // Por ahora retorna lista vacía para no bloquear el dashboard
        return List.of();
    }

    @Override
    public List<AulaResponseDTO> listarPorNivelYAnio(String nivel, Integer anio) {
        return List.of();
    }

    @Override
    public AulaResponseDTO buscarPorId(Long id) {
        throw new RuntimeException("Aula no encontrada con ID: " + id);
    }

    @Override
    public long contarPorAnio(Integer anio) {
        // Conteo estático basado en la especificación institucional:
        // 23 aulas primaria + 22 aulas secundaria = 45 aulas
        return 45L;
    }
}
