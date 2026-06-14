package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.alumno.AlumnoRequestDTO;
import com.colegio.bastidas.dto.alumno.AlumnoResponseDTO;
import java.util.List;

/**
 * Contrato de negocio para la gestión de Alumnos.
 */
public interface AlumnoService {

    /** Lista todos los alumnos activos del año académico dado. */
    List<AlumnoResponseDTO> listarActivosPorAnio(Integer anio);

    /** Lista todos los alumnos de un aula específica. */
    List<AlumnoResponseDTO> listarPorAula(Long aulaId);

    /** Busca alumnos por nombre o DNI (búsqueda parcial). */
    List<AlumnoResponseDTO> buscar(String termino);

    /** Busca un alumno por su ID. */
    AlumnoResponseDTO buscarPorId(Long id);

    /** Busca un alumno por su DNI. */
    AlumnoResponseDTO buscarPorDni(String dni);

    /** Crea un nuevo alumno y su matrícula. */
    AlumnoResponseDTO crear(AlumnoRequestDTO dto);

    /** Actualiza datos de un alumno existente. */
    AlumnoResponseDTO actualizar(Long id, AlumnoRequestDTO dto);

    /** Retorna la cantidad de alumnos activos en un año académico. */
    long contarActivosPorAnio(Integer anio);

    /**
     * Lista alumnos con permiso de academia activos.
     * Usado por el módulo de asistencia docente para mostrar el distintivo visual.
     */
    List<AlumnoResponseDTO> listarConPermisoAcademia(Integer anio);
}
