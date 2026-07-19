package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.docente.DocenteCreadoResponseDTO;
import com.colegio.bastidas.dto.docente.DocenteRequestDTO;
import com.colegio.bastidas.dto.docente.DocenteResponseDTO;
import com.colegio.bastidas.service.DocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.model.CursoAsignado;

/**
 * API REST de Docentes.
 * Base: {@code /api/v1/docentes}
 *
 * Permisos:
 *   GET  → DIRECTOR, ADMIN, DOCENTE (lectura)
 *   POST/PUT → DIRECTOR, ADMIN (escritura)
 */
@RestController
@RequestMapping("/docentes")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class DocenteController {

    private final DocenteService docenteService;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    /**
     * GET /docentes
     * Lista todos los docentes.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<DocenteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(docenteService.listarTodos());
    }

    /**
     * GET /docentes/{id}
     * Retorna un docente por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<DocenteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(docenteService.buscarPorId(id));
    }

    /**
     * GET /docentes/dni/{dni}
     * Busca un docente por DNI.
     */
    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<DocenteResponseDTO> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(docenteService.buscarPorDni(dni));
    }

    /**
     * GET /docentes/{id}/cursos
     * Lista los cursos asignados a un docente.
     */
    @GetMapping("/{id}/cursos")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<CursoAsignado>> obtenerCursosDelDocente(@PathVariable Long id) {
        return ResponseEntity.ok(cursoAsignadoRepository.findByDocenteId(id));
    }

    /**
     * GET /docentes/semaforo
     * Retorna todos los docentes con su estado curricular para el Semáforo Curricular.
     * Usado exclusivamente por el Panel del Director.
     */
    @GetMapping("/semaforo")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<List<DocenteResponseDTO>> obtenerSemaforo() {
        return ResponseEntity.ok(docenteService.obtenerSemaforoCurricular());
    }

    /**
     * GET /docentes/semaforo/conteo
     * Retorna el conteo por estado para los KPIs del Dashboard.
     */
    @GetMapping("/semaforo/conteo")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<Map<String, Long>> conteoSemaforo() {
        return ResponseEntity.ok(Map.of(
            "aprobados",  docenteService.contarPorEstadoCurricular("APROBADO"),
            "pendientes", docenteService.contarPorEstadoCurricular("PENDIENTE"),
            "retrasados", docenteService.contarPorEstadoCurricular("RETRASADO")
        ));
    }

    /**
     * POST /docentes
     * Registra un nuevo docente.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<DocenteCreadoResponseDTO> crear(@Valid @RequestBody DocenteRequestDTO dto) {
        DocenteCreadoResponseDTO creado = docenteService.crear(dto);
        log.info("Docente registrado vía API: {}", creado.getDocente().getNombreCompleto());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * PUT /docentes/{id}
     * Actualiza datos de un docente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<DocenteResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocenteRequestDTO dto) {
        return ResponseEntity.ok(docenteService.actualizar(id, dto));
    }

    /**
     * PATCH /docentes/{id}/estado?activo=false
     * Da de baja (o reactiva) a un docente: desactiva su cuenta de usuario,
     * ya no puede iniciar sesión. No borra su historial (cursos, notas).
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<DocenteResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(docenteService.actualizarEstadoActivo(id, activo));
    }

    /**
     * GET /docentes/exportar
     * Descarga la lista de docentes con sus cursos asignados en Excel.
     * Acceso: DIRECTOR, ADMIN
     */
    @GetMapping("/exportar")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<byte[]> exportar() {
        byte[] excel = docenteService.exportarListaConCursos();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "docentes.xlsx");
        return ResponseEntity.ok().headers(headers).body(excel);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
