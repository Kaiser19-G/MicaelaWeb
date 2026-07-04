package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.CalificacionDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalificacionDiariaRepository extends JpaRepository<CalificacionDiaria, Long> {
    List<CalificacionDiaria> findByCursoAsignadoIdAndSemana(Long cursoAsignadoId, Integer semana);
    List<CalificacionDiaria> findByAlumnoIdAndCursoAsignadoId(Long alumnoId, Long cursoAsignadoId);
}
