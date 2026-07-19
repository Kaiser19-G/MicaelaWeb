package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.calidad.AulaValidacionDTO;
import com.colegio.bastidas.dto.calidad.CalidadResumenDTO;
import com.colegio.bastidas.service.CalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Validador de Pre-Actas (módulo de Calidad / SIAGIE).
 * Endpoints base: {@code /api/v1/calidad}
 */
@RestController
@RequestMapping("/calidad")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
public class CalidadController {

    private final CalidadService calidadService;

    /** GET /calidad/aulas?anio=2026 */
    @GetMapping("/aulas")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<List<AulaValidacionDTO>> listarValidacionAulas(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(calidadService.listarValidacionAulas(anio));
    }

    /** GET /calidad/resumen?anio=2026 */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<CalidadResumenDTO> obtenerResumen(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(calidadService.obtenerResumen(anio));
    }
}
