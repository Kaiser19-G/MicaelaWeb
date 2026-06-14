package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.aula.AulaResponseDTO;
import com.colegio.bastidas.service.AulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
