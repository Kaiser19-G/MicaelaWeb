package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.docente.DocenteRequestDTO;
import com.colegio.bastidas.dto.docente.DocenteResponseDTO;
import java.util.List;

/**
 * Contrato de negocio para la gestión de Docentes.
 */
public interface DocenteService {

    /** Lista todos los docentes con su estado curricular calculado. */
    List<DocenteResponseDTO> listarTodos();

    /** Busca un docente por su ID. */
    DocenteResponseDTO buscarPorId(Long id);

    /** Busca un docente por DNI. */
    DocenteResponseDTO buscarPorDni(String dni);

    /** Crea un nuevo docente. */
    DocenteResponseDTO crear(DocenteRequestDTO dto);

    /** Actualiza datos de un docente existente. */
    DocenteResponseDTO actualizar(Long id, DocenteRequestDTO dto);

    /**
     * Semáforo Curricular: lista los 68 docentes con su estado
     * APROBADO | PENDIENTE | RETRASADO calculado por el sistema.
     */
    List<DocenteResponseDTO> obtenerSemaforoCurricular();

    /** Contadores para KPIs del semáforo: {aprobados, pendientes, retrasados}. */
    long contarPorEstadoCurricular(String estado);
}
