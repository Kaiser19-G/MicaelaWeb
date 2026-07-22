package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.docente.DocenteCreadoResponseDTO;
import com.colegio.bastidas.dto.docente.DocenteDocumentoResponseDTO;
import com.colegio.bastidas.dto.docente.DocenteRequestDTO;
import com.colegio.bastidas.dto.docente.DocenteResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de negocio para la gestión de Docentes.
 */
public interface DocenteService {

    /** Lista todos los docentes con su estado curricular calculado. */
    List<DocenteResponseDTO> listarTodos();

    /** Busca un docente por su ID. */
    DocenteResponseDTO buscarPorId(Long id);

    /** Busca un docente por DNI. */
    DocenteResponseDTO buscarPorDni(String dni);

    /**
     * Crea un nuevo docente y aprovisiona automáticamente su cuenta de Usuario
     * (rol DOCENTE, contraseña inicial predecible que debe cambiar en su primer login).
     */
    DocenteCreadoResponseDTO crear(DocenteRequestDTO dto);

    /** Actualiza datos de un docente existente. */
    DocenteResponseDTO actualizar(Long id, DocenteRequestDTO dto);

    /** Da de baja (o reactiva) a un docente desactivando su cuenta de Usuario (ya no puede iniciar sesión). */
    DocenteResponseDTO actualizarEstadoActivo(Long id, boolean activo);

    /**
     * Semáforo Curricular: lista los 68 docentes con su estado
     * APROBADO | PENDIENTE | RETRASADO calculado por el sistema.
     */
    List<DocenteResponseDTO> obtenerSemaforoCurricular();

    /** Contadores para KPIs del semáforo: {aprobados, pendientes, retrasados}. */
    long contarPorEstadoCurricular(String estado);

    /** Exporta la lista de docentes con sus cursos asignados en formato Excel. */
    byte[] exportarListaConCursos();

    /** Exporta la lista de docentes con sus cursos asignados en formato PDF. */
    byte[] exportarListaConCursosPdf();

    /**
     * Sube (o reemplaza) el documento que certifica que el docente tiene o está
     * cursando docencia. No es obligatorio. Solo admite PDF.
     */
    String cargarDocumentoCertificacion(Long docenteId, MultipartFile archivo);

    /** Documento de certificación docente actual, si fue subido. */
    Optional<DocenteDocumentoResponseDTO> obtenerDocumentoCertificacion(Long docenteId);

    /** Elimina el documento de certificación docente, si existe. */
    void eliminarDocumentoCertificacion(Long docenteId);
}
