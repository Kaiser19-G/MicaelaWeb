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
     */
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
     * Verifica que el expediente del alumno esté completo (todos los documentos
     * requeridos cargados y verificados).
     *
     * @param alumnoId ID del alumno
     * @return true si el expediente está completo
     */
    boolean verificarExpedienteCompleto(Long alumnoId);

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
}
