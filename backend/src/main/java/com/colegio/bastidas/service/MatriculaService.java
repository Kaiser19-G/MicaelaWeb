package com.colegio.bastidas.service;

import com.colegio.bastidas.model.Alumno;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de negocio para la Matrícula Digital.
 * Gestiona el ciclo completo de matrícula y carga de expedientes.
 */
public interface MatriculaService {

    /**
     * Crea o actualiza la matrícula de un alumno para el año académico.
     *
     * @param alumno     Datos del alumno a matricular
     * @param anioAcademico Año académico destino
     * @return Alumno persistido con código asignado
     * @deprecated no vincula un Aula ni verifica vacantes. Usar {@link #crearMatricula}.
     */
    @Deprecated
    Alumno matricularAlumno(Alumno alumno, Integer anioAcademico);

    /**
     * Carga un documento del expediente de matrícula a Supabase Storage.
     * Tipos válidos: DNI, Partida de Nacimiento, Certificado de Estudios, etc.
     *
     * @param alumnoId    ID del alumno
     * @param archivo     Archivo a subir (imagen/PDF)
     * @param tipoDocumento Tipo de documento
     * @return URL pública del documento en Supabase Storage
     */
    String cargarDocumentoExpediente(Long alumnoId, MultipartFile archivo,
                                     String tipoDocumento);

    /**
     * Lista los documentos del expediente de un alumno.
     */
    List<com.colegio.bastidas.dto.expediente.ExpedienteDocumentoResponseDTO> listarDocumentosExpediente(Long alumnoId);

    /**
     * Elimina un documento del expediente (Supabase Storage + registro).
     */
    void eliminarDocumentoExpediente(Long documentoId);

    /**
     * Resumen del expediente de todos los alumnos activos de un año académico,
     * para la tabla del Panel de Dirección (una sola consulta agregada, sin N+1).
     */
    List<com.colegio.bastidas.dto.expediente.ExpedienteResumenDTO> listarResumenExpedientes(Integer anio);

    /**
     * Verifica que el expediente del alumno esté completo (todos los documentos
     * requeridos cargados y verificados).
     *
     * @param alumnoId ID del alumno
     * @return true si el expediente está completo
     */
    boolean verificarExpedienteCompleto(Long alumnoId);

    /**
     * Cuenta los alumnos activos del año cuyo expediente NO está completo
     * (matrícula "provisional"). Usa una consulta agregada, no un loop por alumno.
     */
    long contarConExpedienteIncompleto(Integer anio);

    /**
     * Exporta el consolidado de matrícula en formato Excel compatible con SIAGIE.
     *
     * @param aulaId       ID del aula (opcional, null = toda la IE)
     * @param anioAcademico Año académico
     * @return Bytes del archivo Excel generado con Apache POI
     */
    byte[] exportarConsolidadoMatriculaSiagie(Long aulaId, Integer anioAcademico);

    Optional<Alumno> buscarPorDni(String dni);

    List<Alumno> buscarPorNombreODni(String termino);

    List<Alumno> listarPorAula(Long aulaId, Integer anioAcademico);

    Alumno actualizarPermisoAcademia(Long alumnoId, boolean tienePermiso, String horaEntrada);

    // ── CRUD de la entidad Matricula (Sprint 5) ─────────────────────────────
    List<com.colegio.bastidas.dto.MatriculaDto> listarPorAnio(Integer anio);
    com.colegio.bastidas.dto.MatriculaDto crearMatricula(com.colegio.bastidas.dto.MatriculaDto dto);
    com.colegio.bastidas.dto.MatriculaDto actualizarMatricula(Long id, com.colegio.bastidas.dto.MatriculaDto dto);
    void eliminarMatricula(Long id);
}
