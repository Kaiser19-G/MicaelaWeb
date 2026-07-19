package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReunionRepository extends JpaRepository<Reunion, Long> {

    List<Reunion> findByAulaIdOrderByFechaAscHoraInicioAsc(Long aulaId);

    List<Reunion> findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate desde);

    List<Reunion> findByAlumnoIdOrderByFechaDesc(Long alumnoId);
}
