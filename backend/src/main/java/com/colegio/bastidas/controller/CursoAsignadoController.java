package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.curso.CursoAsignadoRequestDTO;
import com.colegio.bastidas.dto.curso.HorarioRequestDTO;
import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.service.CursoAsignadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST para la asignación de cursos (Docente + Aula + Área Curricular).
 * Base: {@code /api/v1/cursos-asignados}
 */
@RestController
@RequestMapping("/cursos-asignados")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class CursoAsignadoController {

    private final CursoAsignadoService cursoAsignadoService;

    /**
     * POST /cursos-asignados
     * Asigna un docente a un aula/área/año (Director/Admin).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<CursoAsignado> crear(@Valid @RequestBody CursoAsignadoRequestDTO dto) {
        CursoAsignado creado = cursoAsignadoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * DELETE /cursos-asignados/{id}
     * Elimina una asignación (Director/Admin).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoAsignadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /cursos-asignados/aula/{aulaId}
     * Lista las asignaciones (y su horario, si ya fue definido) de una aula — usado por el
     * dashboard de aula para mostrar el horario de clases.
     */
    @GetMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<CursoAsignado>> listarPorAula(@PathVariable Long aulaId) {
        return ResponseEntity.ok(cursoAsignadoService.listarPorAula(aulaId));
    }

    /**
     * PATCH /cursos-asignados/{id}/horario
     * Fija o actualiza el horario (día + hora) de una asignación ya creada (Director/Admin).
     */
    @PatchMapping("/{id}/horario")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<CursoAsignado> actualizarHorario(
            @PathVariable Long id, @Valid @RequestBody HorarioRequestDTO dto) {
        return ResponseEntity.ok(cursoAsignadoService.actualizarHorario(id, dto));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
