package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.curso.CursoAsignadoRequestDTO;
import com.colegio.bastidas.model.CursoAsignado;

/**
 * Contrato de negocio para la asignación de cursos (Docente + Aula + Área) a cargo del Director.
 */
public interface CursoAsignadoService {

    /** Asigna un docente a un aula/área/año. Falla si ya existe la misma asignación. */
    CursoAsignado crear(CursoAsignadoRequestDTO dto);

    /** Elimina una asignación existente. */
    void eliminar(Long id);
}
