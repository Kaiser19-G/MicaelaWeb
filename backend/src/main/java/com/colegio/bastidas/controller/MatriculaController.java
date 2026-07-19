package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el proceso de Matrícula Digital.
 * Endpoints base: {@code /api/v1/matriculas}
 */
@RestController
@RequestMapping("/matriculas")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class MatriculaController {

    private final MatriculaService matriculaService;

    /**
     * POST /matriculas
     * Registra o actualiza la matrícula de un alumno.
     * Acceso: DIRECTOR, ADMIN
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<Alumno> matricular(
            @RequestBody Alumno alumno,
            @RequestParam Integer anioAcademico) {

        log.info("POST /matriculas - DNI={}", alumno.getDni());
        return ResponseEntity.ok(matriculaService.matricularAlumno(alumno, anioAcademico));
    }

    /**
     * GET /matriculas/buscar?termino=...
     * Busca alumnos por nombre o DNI.
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'DOCENTE', 'ADMIN')")
    public ResponseEntity<List<Alumno>> buscar(@RequestParam String termino) {
        return ResponseEntity.ok(matriculaService.buscarPorNombreODni(termino));
    }

    /**
     * GET /matriculas/aula/{aulaId}?anio=...
     * Lista los alumnos de un aula para el año académico.
     */
    @GetMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'DOCENTE')")
    public ResponseEntity<List<Alumno>> listarPorAula(
            @PathVariable Long aulaId,
            @RequestParam Integer anio) {

        return ResponseEntity.ok(matriculaService.listarPorAula(aulaId, anio));
    }

    /**
     * POST /matriculas/{alumnoId}/documentos
     * Sube un documento al expediente de matrícula (DNI, Partida de Nacimiento, etc.).
     * Acceso: DIRECTOR, ADMIN
     */
    @PostMapping("/{alumnoId}/documentos")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> subirDocumento(
            @PathVariable Long alumnoId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipoDocumento") String tipoDocumento) {

        log.info("POST /matriculas/{}/documentos - tipo={}", alumnoId, tipoDocumento);
        String url = matriculaService.cargarDocumentoExpediente(alumnoId, archivo, tipoDocumento);
        return ResponseEntity.ok(Map.of("urlDocumento", url, "tipoDocumento", tipoDocumento));
    }

    /**
     * GET /matriculas/{alumnoId}/expediente/estado
     * Verifica si el expediente está completo.
     */
    @GetMapping("/{alumnoId}/expediente/estado")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Boolean>> estadoExpediente(@PathVariable Long alumnoId) {
        boolean completo = matriculaService.verificarExpedienteCompleto(alumnoId);
        return ResponseEntity.ok(Map.of("expedienteCompleto", completo));
    }

    /**
     * GET /matriculas/{alumnoId}/documentos
     * Lista los documentos del expediente de un alumno.
     */
    @GetMapping("/{alumnoId}/documentos")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<com.colegio.bastidas.dto.expediente.ExpedienteDocumentoResponseDTO>> listarDocumentos(
            @PathVariable Long alumnoId) {
        return ResponseEntity.ok(matriculaService.listarDocumentosExpediente(alumnoId));
    }

    /**
     * DELETE /matriculas/documentos/{documentoId}
     * Elimina un documento del expediente.
     */
    @DeleteMapping("/documentos/{documentoId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Long documentoId) {
        matriculaService.eliminarDocumentoExpediente(documentoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /matriculas/expedientes/resumen?anio=2026
     * Resumen del expediente de todos los alumnos activos del año (Panel de Dirección).
     */
    @GetMapping("/expedientes/resumen")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<com.colegio.bastidas.dto.expediente.ExpedienteResumenDTO>> resumenExpedientes(
            @RequestParam Integer anio) {
        return ResponseEntity.ok(matriculaService.listarResumenExpedientes(anio));
    }

    /**
     * GET /matriculas/exportar/siagie?anio=...&aulaId=...
     * Exporta el consolidado de matrícula en formato Excel para SIAGIE.
     * Acceso: DIRECTOR
     */
    @GetMapping("/exportar/siagie")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<byte[]> exportarSiagie(
            @RequestParam Integer anio,
            @RequestParam(required = false) Long aulaId) {

        log.info("GET /matriculas/exportar/siagie - año={}, aula={}", anio, aulaId);
        byte[] excel = matriculaService.exportarConsolidadoMatriculaSiagie(aulaId, anio);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
            String.format("matricula_siagie_%d.xlsx", anio));

        return ResponseEntity.ok().headers(headers).body(excel);
    }

    /**
     * PATCH /matriculas/{alumnoId}/permiso-academia
     * Actualiza el permiso de academia del alumno.
     */
    @PatchMapping("/{alumnoId}/permiso-academia")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<Alumno> actualizarPermiso(
            @PathVariable Long alumnoId,
            @RequestParam boolean tienePermiso,
            @RequestParam(required = false) String horaEntrada) {

        return ResponseEntity.ok(
            matriculaService.actualizarPermisoAcademia(alumnoId, tienePermiso, horaEntrada));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
