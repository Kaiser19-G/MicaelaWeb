package com.colegio.bastidas.repository;

import com.colegio.bastidas.model.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Alumno.
 * Todas las operaciones se ejecutan contra la base de datos PostgreSQL de Supabase.
 */
@Repository
public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    Optional<Alumno> findByDni(String dni);

    Optional<Alumno> findByCodigoEstudiante(String codigoEstudiante);

    Optional<Alumno> findByUsuarioId(Long usuarioId);

    List<Alumno> findByAulaIdAndEstadoMatricula(Long aulaId, Alumno.EstadoMatricula estado);

    List<Alumno> findByAnioAcademicoAndEstadoMatricula(Integer anio, Alumno.EstadoMatricula estado);

    /** Busca alumnos por nombre o DNI (búsqueda parcial, case-insensitive). */
    @Query("""
        SELECT a FROM Alumno a
        WHERE LOWER(CONCAT(a.apellidoPaterno, ' ', a.apellidoMaterno, ' ', a.nombres))
              LIKE LOWER(CONCAT('%', :termino, '%'))
        OR a.dni LIKE CONCAT('%', :termino, '%')
        ORDER BY a.apellidoPaterno, a.apellidoMaterno
    """)
    List<Alumno> buscarPorNombreODni(@Param("termino") String termino);

    /** Alumnos con permiso de academia activos en el año académico actual. */
    List<Alumno> findByTienePermisoAcademiaAndAnioAcademico(
        Boolean tienePermiso, Integer anioAcademico);

    @Query("SELECT COUNT(a) FROM Alumno a WHERE a.anioAcademico = :anio " +
           "AND a.estadoMatricula = com.colegio.bastidas.model.Alumno.EstadoMatricula.ACTIVO")
    long contarAlumnosActivosPorAnio(@Param("anio") Integer anio);

    boolean existsByDni(String dni);
}
