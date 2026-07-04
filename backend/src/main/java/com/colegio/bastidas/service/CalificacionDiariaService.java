package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.CalificacionDiariaRequestDTO;
import com.colegio.bastidas.model.CalificacionDiaria;
import java.util.List;

public interface CalificacionDiariaService {
    List<CalificacionDiaria> guardarLote(Long cursoAsignadoId, Integer semana, Long docenteId, List<CalificacionDiariaRequestDTO> calificaciones);
    List<CalificacionDiaria> obtenerPorCursoYSemana(Long cursoAsignadoId, Integer semana);
}
