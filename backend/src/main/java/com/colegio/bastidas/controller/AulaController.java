package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.aula.AulaRequestDTO;
import com.colegio.bastidas.dto.aula.AulaResponseDTO;
import com.colegio.bastidas.service.AulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de Aulas.
 * Base: {@code /api/v1/aulas}
 */
@RestController
@RequestMapping("/aulas")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService aulaService;

    /**
     * GET /aulas?anio=2026
     * Lista todas las aulas del año académico.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AulaResponseDTO>> listar(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(aulaService.listarPorAnio(anio));
    }

    /**
     * GET /aulas/nivel/{nivel}?anio=2026
     * Lista aulas por nivel (PRIMARIA | SECUNDARIA).
     */
    @GetMapping("/nivel/{nivel}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AulaResponseDTO>> listarPorNivel(
            @PathVariable String nivel,
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(aulaService.listarPorNivelYAnio(nivel.toUpperCase(), anio));
    }

    /**
     * GET /aulas/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<AulaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aulaService.buscarPorId(id));
    }

    /**
     * POST /aulas
     * Crea una nueva aula (Director/Admin).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<AulaResponseDTO> crear(@Valid @RequestBody AulaRequestDTO dto) {
        return ResponseEntity.ok(aulaService.crear(dto));
    }

    /**
     * DELETE /aulas/{id}
     * Elimina un aula sin alumnos activos ni cursos asignados (corrección de datos).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        aulaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
