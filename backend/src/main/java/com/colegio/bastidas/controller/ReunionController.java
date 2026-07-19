package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.reunion.ReunionRequestDTO;
import com.colegio.bastidas.dto.reunion.ReunionResponseDTO;
import com.colegio.bastidas.service.ReunionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agenda de reuniones con apoderados (Director y Docente-Tutor de Secundaria).
 * Endpoints base: {@code /api/v1/reuniones}
 */
@RestController
@RequestMapping("/reuniones")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class ReunionController {

    private final ReunionService reunionService;

    /** POST /reuniones — reunión individual con el apoderado de un alumno. */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'DOCENTE')")
    public ResponseEntity<ReunionResponseDTO> crear(
            @RequestBody ReunionRequestDTO dto, Authentication authentication) {
        return ResponseEntity.ok(reunionService.crear(dto, authentication));
    }

    /** POST /reuniones/aula/{aulaId} — reunión general: una convocatoria por alumno del aula. */
    @PostMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'DOCENTE')")
    public ResponseEntity<List<ReunionResponseDTO>> crearParaAula(
            @PathVariable Long aulaId, @RequestBody ReunionRequestDTO dto, Authentication authentication) {
        return ResponseEntity.ok(reunionService.crearParaAula(aulaId, dto, authentication));
    }

    /** GET /reuniones/proximas — próximas reuniones agendadas (desde hoy). */
    @GetMapping("/proximas")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'DOCENTE')")
    public ResponseEntity<List<ReunionResponseDTO>> listarProximas() {
        return ResponseEntity.ok(reunionService.listarProximas());
    }

    /** GET /reuniones/aula/{aulaId} — historial de reuniones de un aula. */
    @GetMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'DOCENTE')")
    public ResponseEntity<List<ReunionResponseDTO>> listarPorAula(@PathVariable Long aulaId) {
        return ResponseEntity.ok(reunionService.listarPorAula(aulaId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
