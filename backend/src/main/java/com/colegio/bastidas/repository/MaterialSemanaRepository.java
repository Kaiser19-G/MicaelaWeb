package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.MaterialSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialSemanaRepository extends JpaRepository<MaterialSemana, Long> {
    List<MaterialSemana> findByCursoAsignadoIdAndSemana(Long cursoAsignadoId, Integer semana);
    List<MaterialSemana> findByCursoAsignadoId(Long cursoAsignadoId);
}
