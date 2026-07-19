package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.aula.AulaRequestDTO;
import com.colegio.bastidas.dto.aula.AulaResponseDTO;
import java.util.List;

/**
 * Contrato de negocio para la gestión de Aulas.
 */
public interface AulaService {

    /** Lista todas las aulas del año académico dado. */
    List<AulaResponseDTO> listarPorAnio(Integer anio);

    /** Lista aulas filtrando por nivel (PRIMARIA | SECUNDARIA). */
    List<AulaResponseDTO> listarPorNivelYAnio(String nivel, Integer anio);

    /** Busca un aula por su ID. */
    AulaResponseDTO buscarPorId(Long id);

    /** Total de aulas registradas en el año. */
    long contarPorAnio(Integer anio);

    /** Crea una nueva Aula (Director/Admin). */
    AulaResponseDTO crear(AulaRequestDTO dto);

    /** Cupos libres = capacidad - alumnos activos matriculados en el aula. */
    int contarVacantes(Long aulaId);

    /**
     * Elimina un aula. Solo permitido si no tiene alumnos activos ni cursos
     * asignados — pensado para corregir un aula creada por error, no para
     * "cerrar" un aula con historial real.
     */
    void eliminar(Long id);
}
