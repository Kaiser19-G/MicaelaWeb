package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

    List<Nota> findByAlumnoIdAndAnioAcademico(Long alumnoId, Integer anio);

    List<Nota> findByAlumnoIdAndPeriodoAcademicoAndAnioAcademico(
        Long alumnoId, String periodo, Integer anio);

    Optional<Nota> findByAlumnoIdAndAreaCurricularAndPeriodoAcademicoAndAnioAcademico(
        Long alumnoId, String area, String periodo, Integer anio);

    List<Nota> findByAulaIdAndAreaCurricularAndPeriodoAcademicoAndAnioAcademico(
        Long aulaId, String area, String periodo, Integer anio);

    /** Promedio del aula por área en un período (para monitor del director). */
    @Query("""
        SELECT AVG(n.calificacionNumerica)
        FROM Nota n
        WHERE n.aula.id = :aulaId
        AND n.areaCurricular = :area
        AND n.periodoAcademico = :periodo
        AND n.anioAcademico = :anio
    """)
    Double promedioAulaPorAreaYPeriodo(
        @Param("aulaId") Long aulaId,
        @Param("area") String area,
        @Param("periodo") String periodo,
        @Param("anio") Integer anio);
}
