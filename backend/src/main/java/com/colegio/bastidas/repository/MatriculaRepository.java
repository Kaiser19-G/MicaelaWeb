package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAnioEscolar(Integer anioEscolar);
    List<Matricula> findByAulaIdAndAnioEscolar(Long aulaId, Integer anioEscolar);
    List<Matricula> findByAlumnoId(Long alumnoId);
    boolean existsByAlumnoIdAndAnioEscolar(Long alumnoId, Integer anioEscolar);
}
