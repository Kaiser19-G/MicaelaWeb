package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.alumno.AlumnoCreadoResponseDTO;
import com.colegio.bastidas.dto.alumno.AlumnoRequestDTO;
import com.colegio.bastidas.dto.alumno.AlumnoResponseDTO;
import com.colegio.bastidas.service.AlumnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.model.CursoAsignado;

/**
 * API REST de Alumnos.
 * Base: {@code /api/v1/alumnos}
 */
@RestController
@RequestMapping("/alumnos")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    /**
     * GET /alumnos?anio=2026
     * Lista todos los alumnos activos del año académico.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AlumnoResponseDTO>> listar(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(alumnoService.listarActivosPorAnio(anio));
    }

    /**
     * GET /alumnos/aula/{aulaId}
     * Lista los alumnos de un aula específica.
     * Endpoint principal del módulo de asistencia del docente.
     */
    @GetMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AlumnoResponseDTO>> listarPorAula(@PathVariable Long aulaId) {
        return ResponseEntity.ok(alumnoService.listarPorAula(aulaId));
    }

    /**
     * GET /alumnos/buscar?q=garcia
     * Búsqueda por nombre o DNI (case-insensitive).
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AlumnoResponseDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(alumnoService.buscar(q));
    }

    /**
     * GET /alumnos/{id}
     * Retorna el perfil completo de un alumno.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE','ALUMNO')")
    public ResponseEntity<AlumnoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.buscarPorId(id));
    }

    /**
     * GET /alumnos/dni/{dni}
     */
    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','ALUMNO')")
    public ResponseEntity<AlumnoResponseDTO> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(alumnoService.buscarPorDni(dni));
    }

    /**
     * GET /alumnos/academia?anio=2026
     * Lista los alumnos con permiso de academia (para el módulo de asistencia).
     */
    @GetMapping("/academia")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','DOCENTE')")
    public ResponseEntity<List<AlumnoResponseDTO>> listarConPermiso(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(alumnoService.listarConPermisoAcademia(anio));
    }


    /**
     * POST /alumnos
     * Registra un nuevo alumno (Matrículas).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<AlumnoCreadoResponseDTO> crear(@Valid @RequestBody AlumnoRequestDTO dto) {
        AlumnoCreadoResponseDTO creado = alumnoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * PUT /alumnos/{id}
     * Actualiza datos de un alumno.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<AlumnoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AlumnoRequestDTO dto) {
        return ResponseEntity.ok(alumnoService.actualizar(id, dto));
    }

    /**
     * PATCH /alumnos/{id}/estado?estado=RETIRADO
     * Da de baja (o reactiva) a un alumno cambiando su estado de matrícula.
     * Baja lógica: el alumno deja de aparecer en listados activos, pero su
     * historial (notas, asistencias, expediente) se conserva.
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<AlumnoResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(alumnoService.actualizarEstado(id, estado));
    }

    /**
     * GET /alumnos/{id}/cursos
     * Obtiene los cursos asignados al aula del alumno.
     */
    @GetMapping("/{id}/cursos")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN','ALUMNO')")
    public ResponseEntity<List<CursoAsignado>> obtenerCursosDelAlumno(@PathVariable Long id) {
        AlumnoResponseDTO alumno = alumnoService.buscarPorId(id);
        if (alumno.getAulaId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(cursoAsignadoRepository.findByAulaId(alumno.getAulaId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
}
