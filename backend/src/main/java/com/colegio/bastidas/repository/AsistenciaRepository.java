package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Asistencia.
 * Soporte para consultas de tiempo real y reportes consolidados.
 */
@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);

    List<Asistencia> findByAulaIdAndFechaOrderByAlumnoApellidoPaterno(
        Long aulaId, LocalDate fecha);

    List<Asistencia> findByAlumnoIdAndFechaBetween(
        Long alumnoId, LocalDate fechaInicio, LocalDate fechaFin);

    List<Asistencia> findByAlumnoIdAndEstado(Long alumnoId, Asistencia.EstadoAsistencia estado);

    /** Resumen de asistencia por aula en un rango de fechas. */
    @Query("""
        SELECT a.estado, COUNT(a)
        FROM Asistencia a
        WHERE a.aula.id = :aulaId
        AND a.fecha BETWEEN :inicio AND :fin
        GROUP BY a.estado
    """)
    List<Object[]> resumenAsistenciaPorAula(
        @Param("aulaId") Long aulaId,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin);

    /** Alumnos con más de N faltas en el período (para alertas automáticas). */
    @Query("""
        SELECT a.alumno.id, COUNT(a) as totalFaltas
        FROM Asistencia a
        WHERE a.aula.id = :aulaId
        AND a.fecha BETWEEN :inicio AND :fin
        AND a.estado = com.colegio.bastidas.model.Asistencia.EstadoAsistencia.FALTA
        AND a.tieneJustificacion = false
        GROUP BY a.alumno.id
        HAVING COUNT(a) >= :minFaltas
        ORDER BY totalFaltas DESC
    """)
    List<Object[]> alumnosConFaltasExcesivas(
        @Param("aulaId") Long aulaId,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin,
        @Param("minFaltas") long minFaltas);

    /** Igual que {@link #alumnosConFaltasExcesivas} pero sin filtrar por aula (vista global del director). */
    @Query("""
        SELECT a.alumno.id, COUNT(a) as totalFaltas
        FROM Asistencia a
        WHERE a.fecha BETWEEN :inicio AND :fin
        AND a.estado = com.colegio.bastidas.model.Asistencia.EstadoAsistencia.FALTA
        AND a.tieneJustificacion = false
        GROUP BY a.alumno.id
        HAVING COUNT(a) >= :minFaltas
        ORDER BY totalFaltas DESC
    """)
    List<Object[]> alumnosConFaltasExcesivasGlobal(
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin,
        @Param("minFaltas") long minFaltas);

    /** Registros pendientes de sincronización offline. */
    List<Asistencia> findBySincronizadoOfflineAndDocenteId(
        Boolean sincronizado, Long docenteId);

    long countByAlumnoIdAndEstadoAndFechaBetween(
        Long alumnoId, Asistencia.EstadoAsistencia estado,
        LocalDate inicio, LocalDate fin);

    /** Conteo por fecha y estado (usado por DashboardController para KPIs diarios). */
    long countByFechaAndEstado(LocalDate fecha, Asistencia.EstadoAsistencia estado);

    boolean existsByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);

    /**
     * Conteo de alumnos que ASISTIO, agrupado por fecha, dentro de un rango.
     * {@code aulaId} es opcional: null = global (Dashboard del Director),
     * con valor = acotado a una sola aula (módulo Aulas).
     */
    @Query("""
        SELECT a.fecha, COUNT(a)
        FROM Asistencia a
        WHERE a.fecha BETWEEN :inicio AND :fin
        AND a.estado = com.colegio.bastidas.model.Asistencia.EstadoAsistencia.ASISTIO
        AND (:aulaId IS NULL OR a.aula.id = :aulaId)
        GROUP BY a.fecha
    """)
    List<Object[]> countAsistioPorFechaEntre(
        @Param("aulaId") Long aulaId,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin);
}
