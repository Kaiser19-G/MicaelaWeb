package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.model.Asistencia.EstadoAsistencia;
import com.colegio.bastidas.service.AsistenciaService;
import com.colegio.bastidas.service.AsistenciaService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Control de Asistencia.
 *
 * <p>Endpoints base: {@code /api/v1/asistencias}
 * <p>CORS configurado para conectar con el cliente Angular en localhost:4200.
 */
@RestController
@RequestMapping("/asistencias")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    /**
     * POST /asistencias/registro
     * Registra la asistencia de un alumno individual.
     * Acceso: DOCENTE, DIRECTOR
     */
    @PostMapping("/registro")
    @PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<Asistencia> registrarAsistencia(
            @RequestParam Long alumnoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam EstadoAsistencia estado,
            @RequestParam Long docenteId) {

        log.info("POST /asistencias/registro - alumno={}, fecha={}", alumnoId, fecha);
        Asistencia asistencia = asistenciaService.registrarAsistencia(alumnoId, fecha, estado, docenteId);
        return ResponseEntity.ok(asistencia);
    }

    /**
     * POST /asistencias/lote
     * Registro masivo de asistencia para un aula completa (vista móvil).
     * Acceso: DOCENTE, DIRECTOR
     */
    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<Map<String, Object>> registrarLote(
            @RequestParam Long aulaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Long docenteId,
            @RequestBody @Valid List<RegistroAsistenciaDto> registros) {

        log.info("POST /asistencias/lote - aula={}, fecha={}, registros={}", aulaId, fecha, registros.size());
        // Nota: cada registro se guarda en su propia transacción REQUIRES_NEW
        // (ver AsistenciaRegistroHelper), por lo que las entidades devueltas
        // pueden traer asociaciones lazy ligadas a una sesión ya cerrada.
        // Se responde con un resumen en vez de las entidades para evitar
        // fallos de serialización (LazyInitializationException).
        List<Asistencia> resultado =
            asistenciaService.registrarAsistenciaLote(aulaId, fecha, registros, docenteId);
        return ResponseEntity.ok(Map.of(
            "registrados", resultado.size(),
            "total", registros.size()
        ));
    }

    /**
     * GET /asistencias/aula/{aulaId}?fecha=YYYY-MM-DD
     * Asistencias de un aula para una fecha específica (tiempo real).
     * Acceso: DOCENTE, DIRECTOR
     */
    @GetMapping("/aula/{aulaId}")
    @PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<List<Asistencia>> obtenerPorAulaYFecha(
            @PathVariable Long aulaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return ResponseEntity.ok(
            asistenciaService.obtenerAsistenciasAulaPorFecha(aulaId, fecha));
    }

    /**
     * GET /asistencias/alumno/{alumnoId}/resumen
     * Resumen de asistencia del alumno (para Portal del Alumno – Pestaña 2).
     * Acceso: ALUMNO (propio), DOCENTE, DIRECTOR
     */
    @GetMapping("/alumno/{alumnoId}/resumen")
    @PreAuthorize("hasAnyRole('ALUMNO', 'DOCENTE', 'DIRECTOR')")
    public ResponseEntity<ResumenAsistenciaDto> obtenerResumen(
            @PathVariable Long alumnoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        return ResponseEntity.ok(
            asistenciaService.obtenerResumenAlumno(alumnoId, inicio, fin));
    }

    /**
     * GET /asistencias/alumno/{alumnoId}/historial
     * Historial detallado de asistencia (Calendario de inasistencias del Portal Alumno).
     */
    @GetMapping("/alumno/{alumnoId}/historial")
    @PreAuthorize("hasAnyRole('ALUMNO', 'DOCENTE', 'DIRECTOR')")
    public ResponseEntity<List<Asistencia>> obtenerHistorial(
            @PathVariable Long alumnoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        // Delegado al repositorio vía servicio (se puede ampliar en el servicio)
        return ResponseEntity.ok(List.of()); // TODO: delegar a servicio
    }

    /**
     * POST /asistencias/offline/sync
     * Sincroniza registros capturados offline desde la app móvil del docente.
     * Acceso: DOCENTE
     */
    @PostMapping("/offline/sync")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Integer>> sincronizarOffline(
            @RequestBody List<Asistencia> registros) {

        log.info("POST /asistencias/offline/sync - registros={}", registros.size());
        int sincronizados = asistenciaService.sincronizarRegistrosOffline(registros);
        return ResponseEntity.ok(Map.of("sincronizados", sincronizados));
    }

    /**
     * GET /asistencias/alertas/faltas-excesivas
     * Alumnos con faltas excesivas (panel del director).
     * Acceso: DIRECTOR
     */
    @GetMapping("/alertas/faltas-excesivas")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<List<AlertaFaltaDto>> alertasFaltasExcesivas(
            @RequestParam Long aulaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "3") int minFaltas) {

        return ResponseEntity.ok(
            asistenciaService.detectarAlumnosConFaltasExcesivas(aulaId, inicio, fin, minFaltas));
    }

    /**
     * GET /asistencias/alumno/{alumnoId}/permiso-academia
     * Verifica el permiso de academia de un alumno.
     */
    @GetMapping("/alumno/{alumnoId}/permiso-academia")
    @PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<Map<String, Object>> verificarPermisoAcademia(
            @PathVariable Long alumnoId) {

        boolean tienePermiso = asistenciaService.verificarHorarioAcademia(alumnoId);
        String hora = asistenciaService.obtenerHoraPermisoAcademia(alumnoId);
        return ResponseEntity.ok(Map.of(
            "tienePermiso", tienePermiso,
            "horaEntrada", hora != null ? hora : "Sin permiso"
        ));
    }
}
