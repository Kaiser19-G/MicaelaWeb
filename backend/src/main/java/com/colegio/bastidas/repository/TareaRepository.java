package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByCursoAsignadoIdAndSemana(Long cursoAsignadoId, Integer semana);
    List<Tarea> findByDocenteId(Long docenteId);
}
