package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Evidencia (fotos de exámenes, trabajos, etc.).
 */
@Repository
public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    List<Evidencia> findByAlumnoId(Long alumnoId);

    List<Evidencia> findByNotaId(Long notaId);

    List<Evidencia> findByDocenteIdAndPeriodoAcademico(Long docenteId, String periodo);

    List<Evidencia> findByAlumnoIdAndPeriodoAcademico(Long alumnoId, String periodo);

    void deleteByNotaId(Long notaId);
}
