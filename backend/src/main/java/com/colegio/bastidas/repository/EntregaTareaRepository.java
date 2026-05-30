package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.EntregaTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {
    List<EntregaTarea> findByTareaId(Long tareaId);
    Optional<EntregaTarea> findByTareaIdAndAlumnoId(Long tareaId, Long alumnoId);
}
