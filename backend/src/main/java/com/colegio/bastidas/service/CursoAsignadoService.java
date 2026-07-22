package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.curso.CursoAsignadoRequestDTO;
import com.colegio.bastidas.dto.curso.HorarioRequestDTO;
import com.colegio.bastidas.model.CursoAsignado;

import java.util.List;

/**
 * Contrato de negocio para la asignación de cursos (Docente + Aula + Área) a cargo del Director.
 */
public interface CursoAsignadoService {

    /** Asigna un docente a un aula/área/año. Falla si ya existe la misma asignación o hay solape de horario. */
    CursoAsignado crear(CursoAsignadoRequestDTO dto);

    /** Elimina una asignación existente. */
    void eliminar(Long id);

    /** Lista las asignaciones (con horario, si ya se definió) de una aula. */
    List<CursoAsignado> listarPorAula(Long aulaId);

    /** Fija o actualiza el horario (día + hora) de una asignación ya creada. */
    CursoAsignado actualizarHorario(Long id, HorarioRequestDTO dto);
}
