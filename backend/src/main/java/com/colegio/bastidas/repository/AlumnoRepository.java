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

    long countByAulaIdAndEstadoMatricula(Long aulaId, Alumno.EstadoMatricula estado);

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

    /**
     * Cuenta, en una sola consulta agregada (sin loop por alumno), los alumnos
     * activos del año que YA tienen su expediente completo (DNI + Partida de
     * Nacimiento verificados). Usado para derivar "matrícula provisional" sin
     * recorrer los ~1500 alumnos uno por uno.
     */
    @Query("""
        SELECT COUNT(DISTINCT a.id) FROM Alumno a
        WHERE a.anioAcademico = :anio
        AND a.estadoMatricula = com.colegio.bastidas.model.Alumno.EstadoMatricula.ACTIVO
        AND a.id IN (
            SELECT e1.alumno.id FROM ExpedienteDocumento e1
            WHERE e1.tipoDocumento = com.colegio.bastidas.model.ExpedienteDocumento.TipoDocumento.DNI
            AND e1.estadoVerificacion = com.colegio.bastidas.model.ExpedienteDocumento.EstadoVerificacion.VERIFICADO
        )
        AND a.id IN (
            SELECT e2.alumno.id FROM ExpedienteDocumento e2
            WHERE e2.tipoDocumento = com.colegio.bastidas.model.ExpedienteDocumento.TipoDocumento.PARTIDA_NACIMIENTO
            AND e2.estadoVerificacion = com.colegio.bastidas.model.ExpedienteDocumento.EstadoVerificacion.VERIFICADO
        )
    """)
    long contarConExpedienteCompletoPorAnio(@Param("anio") Integer anio);
}
